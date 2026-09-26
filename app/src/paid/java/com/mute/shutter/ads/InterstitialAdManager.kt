package com.mute.shutter.ads

import android.app.Activity

class InterstitialAdManager(private val activity: Activity) {
    fun preload() = Unit
    fun showIfReady() = Unit
    fun destroy() = Unit
}
