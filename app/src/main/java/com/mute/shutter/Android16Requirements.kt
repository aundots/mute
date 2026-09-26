package com.mute.shutter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

object Android16Requirements {
    const val MIN_SDK = 36

    /** Android 16 무선 디버깅 메뉴 경로 */
    const val WIRELESS_DEBUGGING_PATH =
        "설정 → 시스템 → 개발자 옵션 → 무선 디버깅"

    fun needsNearbyWifiPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasNearbyWifiPermission(activity: ComponentActivity): Boolean {
        if (!needsNearbyWifiPermission()) return true
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.NEARBY_WIFI_DEVICES,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
