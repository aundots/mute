package com.mute.shutter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mute.shutter.adb.AdbResult
import com.mute.shutter.adb.DiscoveredEndpoints
import com.mute.shutter.camera.CameraMuteService
import com.mute.shutter.camera.UsageAccessHelper
import com.mute.shutter.shutter.ShutterSoundController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConnectionStatus {
    NotPaired,
    PairedNotConnected,
    Connected,
    Muted,
}

data class MainUiState(
    val pairPort: String = "",
    val pin: String = "",
    val wlanIp: String = "",
    val connectPort: String = "",
    val status: ConnectionStatus = ConnectionStatus.NotPaired,
    val isLoading: Boolean = false,
    val isPaired: Boolean = false,
    val showAdvanced: Boolean = false,
    val needsUsageAccess: Boolean = false,
    val statusMessage: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val muteApp = application as MuteApplication
    private val preferences = muteApp.preferences
    private val adb = muteApp.adb
    private val shutter = muteApp.shutter
    private val app = application.applicationContext

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _interstitialEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val interstitialEvents: SharedFlow<Unit> = _interstitialEvents.asSharedFlow()

    init {
        restoreFromPreferences()
        viewModelScope.launch {
            syncMuteStatusIfConnected()
            if (preferences.isPaired) {
                startCameraWatcherIfNeeded()
            }
        }
    }

    fun updatePairPort(v: String) = _uiState.update { it.copy(pairPort = v.filter(Char::isDigit)) }
    fun updatePin(v: String) = _uiState.update { it.copy(pin = v.filter(Char::isDigit).take(6)) }
    fun updateWlanIp(v: String) = _uiState.update { it.copy(wlanIp = v.trim()) }
    fun updateConnectPort(v: String) = _uiState.update { it.copy(connectPort = v.filter(Char::isDigit)) }
    fun toggleAdvanced() = _uiState.update { it.copy(showAdvanced = !it.showAdvanced) }

    fun onResume() {
        viewModelScope.launch {
            if (!_uiState.value.isLoading) {
                restoreFromPreferences()
            }
            updateUsageAccessFlag()
            if (!preferences.isPaired && !_uiState.value.isLoading) {
                refreshEndpointsInternal()
                syncMuteStatusIfConnected()
                return@launch
            }
            if (preferences.isPaired) {
                startCameraWatcherIfNeeded()
                if (!_uiState.value.isLoading) {
                    applyMuteInternal(silent = true)
                }
            }
        }
    }

    fun refreshEndpoints() {
        viewModelScope.launch { refreshEndpointsInternal() }
    }

    fun applyMute() {
        viewModelScope.launch { applyMuteInternal(silent = false) }
    }

    fun resetPairing() {
        CameraMuteService.stop(app)
        preferences.clearPairing()
        _uiState.value = MainUiState()
        viewModelScope.launch { refreshEndpointsInternal() }
    }

    fun onUsageAccessGranted() {
        updateUsageAccessFlag()
        startCameraWatcherIfNeeded()
    }

    private fun restoreFromPreferences() {
        _uiState.update {
            it.copy(
                isPaired = preferences.isPaired,
                wlanIp = preferences.lastHost.orEmpty(),
                connectPort = preferences.lastConnectPort.takeIf { p -> p > 0 }?.toString().orEmpty(),
                needsUsageAccess = preferences.isPaired && !UsageAccessHelper.hasUsageAccess(app),
                status = when {
                    preferences.lastMuteValue == ShutterConstants.MUTED_VALUE -> ConnectionStatus.Muted
                    preferences.isPaired -> ConnectionStatus.PairedNotConnected
                    else -> ConnectionStatus.NotPaired
                },
            )
        }
    }

    private fun updateUsageAccessFlag() {
        _uiState.update {
            it.copy(needsUsageAccess = preferences.isPaired && !UsageAccessHelper.hasUsageAccess(app))
        }
    }

    private fun startCameraWatcherIfNeeded() {
        if (!preferences.isPaired) return
        CameraMuteService.start(app)
    }

    private suspend fun applyMuteInternal(silent: Boolean) {
        if (!preferences.isPaired) {
            if (!silent) runFirstTimeSetup()
            return
        }
        runConnectAndMute(silent = silent)
    }

    private suspend fun runFirstTimeSetup() {
        val pairPort = _uiState.value.pairPort.toIntOrNull()
        val pin = _uiState.value.pin
        if (pairPort == null || pairPort !in 1..65535 || pin.length != 6) {
            _uiState.update {
                it.copy(statusMessage = app.getString(R.string.error_invalid_pairing_input))
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                statusMessage = app.getString(R.string.status_applying),
                status = ConnectionStatus.Connected,
                wlanIp = ShutterConstants.LOCALHOST,
            )
        }

        var pairError: String? = null
        when (val pair = adb.pair(ShutterConstants.LOCALHOST, pairPort, pin)) {
            is AdbResult.Failure -> pairError = pair.message
            is AdbResult.Success -> {
                preferences.lastHost = ShutterConstants.LOCALHOST
                _uiState.update {
                    it.copy(isPaired = true, status = ConnectionStatus.PairedNotConnected)
                }
            }
        }

        delay(800)
        runConnectAndMute(silent = false, priorError = pairError)
    }

    private suspend fun runConnectAndMute(silent: Boolean, priorError: String? = null) {
        if (!silent) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    status = ConnectionStatus.Connected,
                    statusMessage = app.getString(R.string.status_applying),
                )
            }
        }
        refreshEndpointsInternal()

        val hintPort = _uiState.value.connectPort.toIntOrNull()
            ?: preferences.lastConnectPort.takeIf { it in 1..65535 }

        when (val connect = adb.connectAuto(hintPort)) {
            is AdbResult.Failure -> {
                if (handleAlreadyMuted()) {
                    finishMuted(silent)
                    return
                }
                if (preferences.isPaired) {
                    when (shutter.mute()) {
                        is AdbResult.Success -> finishMuted(silent)
                        is AdbResult.Failure -> finishMuted(silent)
                    }
                    return
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        status = ConnectionStatus.NotPaired,
                        statusMessage = priorError ?: app.getString(R.string.error_connect_failed),
                    )
                }
                return
            }
            is AdbResult.Success -> Unit
        }

        when (val mute = shutter.mute()) {
            is AdbResult.Success -> finishMuted(silent)
            is AdbResult.Failure -> {
                if (handleAlreadyMuted()) {
                    finishMuted(silent)
                } else if (preferences.isPaired) {
                    finishMuted(silent)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = ConnectionStatus.NotPaired,
                            statusMessage = mute.message.ifBlank { app.getString(R.string.error_connect_failed) },
                        )
                    }
                }
            }
        }
    }

    private suspend fun syncMuteStatusIfConnected() {
        if (preferences.lastMuteValue == ShutterConstants.MUTED_VALUE) {
            _uiState.update {
                it.copy(
                    status = ConnectionStatus.Muted,
                    isPaired = true,
                    statusMessage = null,
                )
            }
            return
        }
        when (adb.testConnection()) {
            is AdbResult.Success -> {
                if (handleAlreadyMuted()) {
                    finishMuted(silent = true)
                }
            }
            is AdbResult.Failure -> Unit
        }
    }

    private suspend fun handleAlreadyMuted(): Boolean {
        return when (val current = shutter.read()) {
            is AdbResult.Success -> ShutterSoundController.isMutedValue(current.value)
            is AdbResult.Failure -> false
        }
    }

    private fun finishMuted(silent: Boolean) {
        preferences.isPaired = true
        preferences.lastMuteValue = ShutterConstants.MUTED_VALUE
        startCameraWatcherIfNeeded()
        updateUsageAccessFlag()
        _uiState.update {
            it.copy(
                isLoading = false,
                status = ConnectionStatus.Muted,
                isPaired = true,
                statusMessage = null,
            )
        }
        if (!silent && BuildConfig.HAS_ADS) {
            _interstitialEvents.tryEmit(Unit)
        }
    }

    private suspend fun refreshEndpointsInternal() {
        applyDiscoveredToUi(adb.discoverAll())
    }

    private fun applyDiscoveredToUi(discovered: DiscoveredEndpoints) {
        _uiState.update { state ->
            state.copy(
                wlanIp = discovered.ip ?: state.wlanIp,
                connectPort = discovered.connectPort?.toString() ?: state.connectPort,
            )
        }
    }
}
