package com.mute.shutter.adb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledAdbRunnerTest {
    @Test
    fun connectSuccess_rejectsConnectionRefused() {
        val output = "failed to connect to 127.0.0.1:46563: Connection refused"
        assertFalse(BundledAdbRunner.isConnectSuccess(output))
    }

    @Test
    fun connectSuccess_acceptsConnected() {
        assertTrue(BundledAdbRunner.isConnectSuccess("connected to 127.0.0.1:46563"))
    }

    @Test
    fun connectSuccess_acceptsAlreadyConnected() {
        assertTrue(BundledAdbRunner.isConnectSuccess("already connected to localhost:46563"))
    }

    @Test
    fun pairSuccess_detectsMessage() {
        assertTrue(BundledAdbRunner.isPairSuccess("Successfully paired to localhost:34747"))
    }
}
