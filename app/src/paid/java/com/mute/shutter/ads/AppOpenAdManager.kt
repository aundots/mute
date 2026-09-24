package com.mute.shutter.ads

import android.app.Application

/** 유료 빌드에는 광고가 없으므로 no-op 스텁 (main 코드가 두 플레이버에서 모두 컴파일되도록) */
class AppOpenAdManager(application: Application) {
    fun register() = Unit
}
