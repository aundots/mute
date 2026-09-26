package com.mute.shutter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mute.shutter.adb.AdbResult
import com.mute.shutter.adb.DiscoveredEndpoints
import com.mute.shutter.camera.CameraMuteService
import com.mute.shutter.camera.UsageAccessHelper
import com.mute.shutter.debug.DebugLogger
import com.mute.shutter.shutter.ShutterDiagnostics
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
    val connectPort: String = "",
    val status: ConnectionStatus = ConnectionStatus.NotPaired,
    val isLoading: Boolean = false,
    val isPaired: Boolean = false,
    val showAdvanced: Boolean = false,
    val needsUsageAccess: Boolean = false,
    val needsDndAccess: Boolean = false,
    val statusMessage: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val muteApp = application as MuteApplication
    private val preferences = muteApp.preferences
    private val adb = muteApp.adb
    private val shutter = muteApp.shutter
    private val diagnostics = ShutterDiagnostics(adb)
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
            it.copy(
                needsUsageAccess = preferences.isPaired && !UsageAccessHelper.hasUsageAccess(app),
            )
        }
    }

    private fun startCameraWatcherIfNeeded() {
        if (!preferences.isPaired) return
        CameraMuteService.start(app)
    }

    private suspend fun applyMuteInternal(silent: Boolean) {
        val pairPort = _uiState.value.pairPort.toIntOrNull()
        val pin = _uiState.value.pin
        val hasPairCreds = pairPort != null && pairPort in 1..65535 && pin.length == 6

        // 페어링 코드+PIN을 새로 입력했으면, 이미 페어링됨 상태여도 다시 페어링한다.
        // (소프트웨어 업데이트/재부팅으로 폰 쪽 페어링 신뢰가 풀리면 연결이 거부되기 때문)
        if (!silent && hasPairCreds) {
            DebugLogger.log("▶ 페어링 자격증명 입력됨 → 강제 재페어링 실행")
            runFirstTimeSetup()
            return
        }

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
            )
        }

        DebugLogger.log("▶ 페어링 시도 (127.0.0.1:$pairPort, PIN 6자리)")
        var pairError: String? = null
        when (val pair = adb.pair(ShutterConstants.LOCALHOST, pairPort, pin)) {
            is AdbResult.Failure -> {
                pairError = pair.message
                DebugLogger.logError("페어링 실패: ${pair.message} ${pair.detail}")
            }
            is AdbResult.Success -> {
                DebugLogger.logSuccess("페어링 성공")
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
        val hintPort = _uiState.value.connectPort.toIntOrNull()
            ?: preferences.lastConnectPort.takeIf { it in 1..65535 }

        DebugLogger.log("▶ ADB 연결 시도 (hintPort=$hintPort)")
        when (val connect = adb.connectAuto(hintPort)) {
            is AdbResult.Failure -> {
                DebugLogger.logError("ADB 연결 실패: ${connect.message} ${connect.detail}")
                if (handleAlreadyMuted()) {
                    finishMuted(silent)
                    return
                }
                failConnection(silent, priorError ?: app.getString(R.string.error_connect_failed))
                return
            }
            is AdbResult.Success -> {
                DebugLogger.logSuccess("ADB 연결 성공")
                _uiState.update { it.copy(connectPort = preferences.lastConnectPort.toString()) }
            }
        }

        when (val mute = shutter.mute()) {
            is AdbResult.Success -> finishMuted(silent)
            is AdbResult.Failure -> {
                if (handleAlreadyMuted()) {
                    finishMuted(silent)
                } else {
                    failConnection(
                        silent,
                        mute.message.ifBlank { app.getString(R.string.error_connect_failed) },
                    )
                }
            }
        }

        // 사용자가 직접 적용했을 때만 진단 덤프를 남긴다.
        // "설정은 0으로 써졌는데도 소리가 나는" 경우가 핵심이라 성공/실패 모두 남겨야 한다.
        // UI 상태를 먼저 갱신한 뒤 실행해 체감 지연을 줄인다.
        if (!silent) {
            diagnostics.dump()
        }
    }

    /** ADB 연결·설정 적용이 실제로 실패했을 때. isPaired여도 무음 적용됐다고 거짓 표시하지 않는다 */
    private fun failConnection(silent: Boolean, message: String) {
        DebugLogger.logError("무음 적용 실패 (silent=$silent): $message")
        _uiState.update {
            it.copy(
                isLoading = false,
                status = if (preferences.isPaired) ConnectionStatus.PairedNotConnected else ConnectionStatus.NotPaired,
                statusMessage = message,
            )
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
                connectPort = discovered.connectPort?.toString() ?: state.connectPort,
            )
        }
    }
}
