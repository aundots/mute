package com.mute.shutter.adb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledAdbRunnerTest {
    @Test fun shellAlwaysSelectsExactTransport() {
        org.junit.Assert.assertEquals(listOf("-s", "127.0.0.1:35709", "shell", "echo mute_ok"),
            BundledAdbRunner.shellArguments(BundledAdbRunner.endpointSerial("127.0.0.1", 35709), "echo mute_ok"))
        org.junit.Assert.assertEquals("[::1]:35709", BundledAdbRunner.endpointSerial("::1", 35709))
    }

    @Test
    fun mdns_parsesActualAdbOutputAndIgnoresPairingPort() {
        val entries = BundledAdbRunner.parseMdnsEndpoints(
            "List of discovered mdns services\n" +
            "adb-phone _adb-tls-pairing._tcp 192.168.86.54:44999\n" +
            "adb-phone _adb-tls-connect._tcp 192.168.86.54:37821\n")
        org.junit.Assert.assertEquals(listOf(BundledAdbRunner.MdnsEndpoint("192.168.86.54", 37821)), entries)
    }

    @Test
    fun mdns_acceptsTrailingDotAndRejectsInvalidEndpoints() {
        val entries = BundledAdbRunner.parseMdnsEndpoints(
            "a _adb-tls-connect._tcp. 192.168.0.2:12345\n" +
            "b _adb-tls-connect._tcp 999.1.1.1:12345\n" +
            "c _adb-tls-connect._tcp 192.168.0.3:0\n" +
            "d _adb-tls-connect._tcp 192.168.0.3:65536\n")
        org.junit.Assert.assertEquals(listOf(BundledAdbRunner.MdnsEndpoint("192.168.0.2", 12345)), entries)
    }

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
