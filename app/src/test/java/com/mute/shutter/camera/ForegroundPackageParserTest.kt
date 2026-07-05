package com.mute.shutter.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraMuteLogicTest {
    @Test
    fun isCameraPackageMatchesSamsungPrefixes() {
        assertTrue(com.mute.shutter.ShutterConstants.isCameraPackage("com.samsung.android.app.camera"))
        assertTrue(com.mute.shutter.ShutterConstants.isCameraPackage("com.sec.android.app.camera"))
        assertFalse(com.mute.shutter.ShutterConstants.isCameraPackage("com.samsung.android.messaging"))
    }
}
