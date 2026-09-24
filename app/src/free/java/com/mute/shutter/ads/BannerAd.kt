package com.mute.shutter.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adViewRef = remember { mutableStateOf<AdView?>(null) }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdIds.banner(context)
                loadAd(AdRequest.Builder().build())
                adViewRef.value = this
            }
        },
        // adUnitId는 factory에서 한 번만 설정한다.
        // AdView는 재설정 시 IllegalStateException을 던지므로 update에서 건드리면 안 된다.
        onRelease = { adView ->
            adView.destroy()
            if (adViewRef.value === adView) {
                adViewRef.value = null
            }
        },
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val adView = adViewRef.value ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_DESTROY -> {
                    adView.destroy()
                    adViewRef.value = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adViewRef.value?.destroy()
            adViewRef.value = null
        }
    }
}
