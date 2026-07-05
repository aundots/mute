package com.mute.shutter.camera

import android.app.usage.UsageStatsManager
import android.content.Context
import com.mute.shutter.ShutterConstants

class CameraForegroundDetector(private val context: Context) {
    fun getForegroundCameraPackage(): String? {
        if (!UsageAccessHelper.hasUsageAccess(context)) return null

        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val end = System.currentTimeMillis()
        val start = end - 5_000

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end) ?: return null
        val foreground = stats
            .filter { it.lastTimeUsed >= end - 2_000 }
            .maxByOrNull { it.lastTimeUsed }
            ?.packageName

        return foreground?.takeIf { ShutterConstants.isCameraPackage(it) }
    }
}
