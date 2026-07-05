package com.mute.shutter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mute.shutter.camera.CameraMuteService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? MuteApplication ?: return
        if (!app.preferences.isPaired) return
        ShutterReapplyWorker.enqueue(context, immediate = true)
        CameraMuteService.start(context)
    }
}
