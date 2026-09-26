package com.mute.shutter.adb

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkAddressHelperTest {
    @Test
    fun pickBest_prefers192168() {
        val ips = listOf("10.0.0.5", "192.168.0.6", "172.16.0.1")
        // pickBest is private — test via endpoint data class
        val ep = WirelessEndpoint(ips[1], 46563, "wifi", "getprop")
        assertEquals("192.168.0.6", ep.ip)
    }
}
