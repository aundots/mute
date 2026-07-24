package com.mute.shutter.ads

import android.content.Context
import com.mute.shutter.BuildConfig
import com.mute.shutter.R

/**
 * 광고 단위 ID를 한 곳에서 관리한다.
 *
 * 디버그 빌드에서는 반드시 구글 공식 **테스트 ID**를 사용한다.
 * 개발/테스트 중 발생한 노출·클릭이 실제 AdMob 계정에 부정 트래픽으로 쌓이면
 * 계정이 정지될 수 있고, 그러면 수익 채널 자체가 사라진다.
 * 실제 ID는 릴리스 빌드에서만 리소스에서 읽어 쓴다.
 */
object AdIds {
    // 구글 공식 테스트 광고 단위 (https://developers.google.com/admob/android/test-ads)
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"

    fun banner(context: Context): String =
        if (BuildConfig.DEBUG) TEST_BANNER
        else context.getString(R.string.admob_banner_unit_id)

    fun interstitial(context: Context): String =
        if (BuildConfig.DEBUG) TEST_INTERSTITIAL
        else context.getString(R.string.admob_interstitial_unit_id)

    fun appOpen(context: Context): String =
        if (BuildConfig.DEBUG) TEST_APP_OPEN
        else context.getString(R.string.admob_app_open_unit_id)
}
