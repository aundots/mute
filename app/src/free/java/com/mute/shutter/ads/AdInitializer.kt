package com.mute.shutter.ads

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

object AdInitializer {
    /**
     * 디버그 빌드에서 실제 광고 노출/클릭이 계정에 부정 트래픽으로 잡히는 것을 막는 1차 방어는
     * "디버그일 때 구글 테스트 광고 단위 ID 사용"이며 이는 [AdIds]에서 처리한다.
     *
     * 추가로, 실제 광고 단위로 테스트해야 할 일이 생기면 아래 목록에 logcat에 찍히는
     * 이 기기의 테스트 기기 해시("Use ... setTestDeviceIds(Arrays.asList("XXXX"))")를 넣으면 된다.
     * 에뮬레이터는 자동으로 테스트 기기로 처리된다.
     */
    private val TEST_DEVICE_IDS = emptyList<String>()

    fun init(application: Application) {
        if (TEST_DEVICE_IDS.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(TEST_DEVICE_IDS)
                    .build(),
            )
        }
        MobileAds.initialize(application)
    }
}
