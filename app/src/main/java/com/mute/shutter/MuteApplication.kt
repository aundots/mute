package com.mute.shutter

import android.app.Application
import com.mute.shutter.adb.AdbSessionManager
import com.mute.shutter.ads.AdInitializer
import com.mute.shutter.data.SessionPreferences
import com.mute.shutter.shutter.ShutterSoundController

class MuteApplication : Application() {
    lateinit var preferences: SessionPreferences
        private set

    lateinit var adb: AdbSessionManager
        private set

    lateinit var shutter: ShutterSoundController
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.HAS_ADS) {
            AdInitializer.init(this)
        }
        preferences = SessionPreferences(this)
        adb = AdbSessionManager(this, preferences)
        shutter = ShutterSoundController(adb, preferences)
    }
}
