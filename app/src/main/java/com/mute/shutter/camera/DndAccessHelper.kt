package com.mute.shutter.camera

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Android 6+에서 AudioManager.ringerMode를 SILENT/VIBRATE로 바꾸려면
 * "방해 금지 모드 접근 권한"(Notification Policy Access)이 필요하다.
 * 없으면 setRingerMode 호출 시 SecurityException이 발생한다.
 */
object DndAccessHelper {
    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return nm.isNotificationPolicyAccessGranted
    }

    fun openDndAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
