package com.mute.shutter.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(private val activity: Activity) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun preload() {
        if (isLoading || interstitialAd != null) return
        isLoading = true
        InterstitialAd.load(
            activity,
            AdIds.interstitial(activity),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            AdFrequencyGate.markShown()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            preload()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                            preload()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                }
            },
        )
    }

    fun showIfReady() {
        if (activity.isFinishing || activity.isDestroyed) return
        // 앱 오픈 광고 등 다른 전체화면 광고와 연달아 뜨지 않도록 최소 간격 확인
        if (!AdFrequencyGate.canShowFullScreen()) return
        val ad = interstitialAd
        if (ad != null) {
            ad.show(activity)
        } else {
            preload()
        }
    }

    fun destroy() {
        interstitialAd = null
        isLoading = false
    }
}
