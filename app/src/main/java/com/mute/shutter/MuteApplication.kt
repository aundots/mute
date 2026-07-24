package com.mute.shutter

import android.app.Application
import com.mute.shutter.adb.AdbSessionManager
import com.mute.shutter.ads.AdInitializer
import com.mute.shutter.ads.AppOpenAdManager
import com.mute.shutter.data.SessionPreferences
import com.mute.shutter.debug.DebugLogger
import com.mute.shutter.shutter.ShutterSoundController

class MuteApplication : Application() {
    lateinit var preferences: SessionPreferences
        private set

    lateinit var adb: AdbSessionManager
        private set

    lateinit var shutter: ShutterSoundController
        private set

    private var appOpenAdManager: AppOpenAdManager? = null

    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(this)
        preferences = SessionPreferences(this)
        adb = AdbSessionManager(this, preferences)
        shutter = ShutterSoundController(adb, preferences)

        if (BuildConfig.HAS_ADS) {
            AdInitializer.init(this)
            appOpenAdManager = AppOpenAdManager(this).also { it.register() }
        }
    }
}
