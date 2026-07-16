package com.mute.shutter

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.mute.shutter.ads.BannerAd
import com.mute.shutter.ads.InterstitialAdManager
import com.mute.shutter.camera.UsageAccessHelper
import com.mute.shutter.ui.PairingGuideScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingAction: (() -> Unit)? = null
    private var interstitialAdManager: InterstitialAdManager? = null

    private val nearbyWifiPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshEndpoints()
        if (granted) {
            pendingAction?.invoke()
        }
        pendingAction = null
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.HAS_ADS) {
            interstitialAdManager = InterstitialAdManager(this).also { it.preload() }
            lifecycleScope.launch {
                viewModel.interstitialEvents.collectLatest {
                    interstitialAdManager?.showIfReady()
                }
            }
        }

        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val state by viewModel.uiState.collectAsState()
                        Box(modifier = Modifier.weight(1f)) {
                            PairingGuideScreen(
                                modifier = Modifier.fillMaxSize(),
                                state = state,
                                onPairPortChange = viewModel::updatePairPort,
                                onPinChange = viewModel::updatePin,
                                onConnectPortChange = viewModel::updateConnectPort,
                                onWlanIpChange = viewModel::updateWlanIp,
                                onApplyMute = { runWithNetworkPermission(viewModel::applyMute) },
                                onToggleAdvanced = viewModel::toggleAdvanced,
                                onResetPairing = viewModel::resetPairing,
                                onOpenUsageAccess = {
                                    UsageAccessHelper.openUsageAccessSettings(this@MainActivity)
                                },
                            )
                        }
                        if (BuildConfig.HAS_ADS) {
                            BannerAd()
                        }
                    }
                }
            }
        }
        ensureNetworkPermissionThenRefresh()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onUsageAccessGranted()
        ensureNetworkPermissionThenRefresh()
        if (Android16Requirements.hasNearbyWifiPermission(this)) {
            viewModel.onResume()
        }
    }

    override fun onDestroy() {
        if (BuildConfig.HAS_ADS) {
            interstitialAdManager?.destroy()
            interstitialAdManager = null
        }
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun ensureNetworkPermissionThenRefresh() {
        if (Android16Requirements.hasNearbyWifiPermission(this)) {
            viewModel.refreshEndpoints()
            return
        }
        if (Android16Requirements.needsNearbyWifiPermission()) {
            nearbyWifiPermission.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            viewModel.refreshEndpoints()
        }
    }

    private fun runWithNetworkPermission(action: () -> Unit) {
        if (!Android16Requirements.needsNearbyWifiPermission() ||
            Android16Requirements.hasNearbyWifiPermission(this)
        ) {
            action()
            return
        }
        pendingAction = action
        nearbyWifiPermission.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
}
