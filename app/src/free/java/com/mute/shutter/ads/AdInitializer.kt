package com.mute.shutter.ads

import android.app.Application
import com.google.android.gms.ads.MobileAds

object AdInitializer {
    fun init(application: Application) {
        MobileAds.initialize(application)
    }
}
