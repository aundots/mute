package com.mute.shutter

object ShutterConstants {
    const val SETTINGS_KEY = "csc_pref_camera_forced_shuttersound_key"
    const val MUTED_VALUE = "0"
    const val DEFAULT_VALUE = "1"
    const val LOCALHOST = "127.0.0.1"

    const val STREAM_SYSTEM = 1
    const val STREAM_RING = 2
    const val STREAM_MUSIC = 3
    const val STREAM_NOTIFICATION = 5
    const val STREAM_SYSTEM_ENFORCED = 7

    val CAMERA_PACKAGES = setOf(
        "com.sec.android.app.camera",
        "com.samsung.android.camera",
        "com.samsung.android.app.camera",
        "com.android.camera2",
        "com.android.camera",
    )

    fun isCameraPackage(packageName: String): Boolean {
        if (CAMERA_PACKAGES.contains(packageName)) return true
        return packageName.contains("camera", ignoreCase = true) &&
            (packageName.startsWith("com.samsung") || packageName.startsWith("com.sec.android"))
    }
}
