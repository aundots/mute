package com.mute.shutter.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.mute.shutter.MuteApplication

/**
 * 앱을 포그라운드로 가져올 때 표시하는 앱 오픈 광고.
 *
 * 이 앱은 "한 번 설정하면 자동" 유틸리티라 세션이 적고, 유일하게 반복되는 세션은
 * 재부팅 후 재연결하러 앱을 여는 흐름이다. 그 순간을 광고로 포착한다.
 *
 * 정책/사용성 가드:
 * - 최초 페어링 완료 전(온보딩 중)에는 절대 표시하지 않는다.
 * - 콜드 스타트 첫 진입에는 광고가 아직 로드돼 있지 않아 자연히 표시되지 않는다(다음 진입부터).
 * - 전면 광고 등 다른 전체화면 광고와 연달아 뜨지 않도록 [AdFrequencyGate]를 공유한다.
 * - 로드 실패/미준비면 조용히 통과해 앱 흐름을 막지 않는다.
 */
class AppOpenAdManager(
    private val application: Application,
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime = 0L
    private var currentActivity: Activity? = null

    private val isPaired: Boolean
        get() = (application as? MuteApplication)?.preferences?.isPaired == true

    fun register() {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    // ProcessLifecycleOwner: 앱이 포그라운드로 올라올 때
    override fun onStart(owner: LifecycleOwner) {
        showAdIfAvailable()
    }

    private fun loadAd() {
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true
        val context = currentActivity ?: application
        AppOpenAd.load(
            context,
            AdIds.appOpen(context),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                }
            },
        )
    }

    /** 앱 오픈 광고는 로드 후 4시간이 지나면 만료된다 */
    private fun isAdAvailable(): Boolean =
        appOpenAd != null && System.currentTimeMillis() - loadTime < 4 * 60 * 60 * 1000L

    private fun showAdIfAvailable() {
        if (isShowingAd) return

        // 온보딩(최초 페어링 전)에는 표시하지 않고, 다음을 위해 미리 로드만 한다.
        if (!isPaired) {
            loadAd()
            return
        }
        if (!AdFrequencyGate.canShowFullScreen()) return

        val activity = currentActivity
        val ad = appOpenAd
        if (activity == null || ad == null || !isAdAvailable()) {
            loadAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdFrequencyGate.markShown()
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }
        }
        isShowingAd = true
        ad.show(activity)
    }

    // ActivityLifecycleCallbacks: 현재 액티비티 추적 (광고 표시 중에는 갱신하지 않음)
    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isShowingAd) currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
