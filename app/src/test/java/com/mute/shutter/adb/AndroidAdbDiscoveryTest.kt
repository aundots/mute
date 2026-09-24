package com.mute.shutter.adb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAdbDiscoveryTest {
    @Test fun acceptsNewPortOnThisPhoneOnly() {
        val locals = setOf("192.168.86.30", "127.0.0.1")
        assertTrue(AndroidAdbDiscovery.isLocalEndpoint("192.168.86.30", 45001, locals))
        assertFalse(AndroidAdbDiscovery.isLocalEndpoint("192.168.86.31", 45001, locals))
        assertFalse(AndroidAdbDiscovery.isLocalEndpoint("192.168.86.30", 0, locals))
        assertFalse(AndroidAdbDiscovery.isLocalEndpoint("192.168.86.30", 65536, locals))
    }
}
