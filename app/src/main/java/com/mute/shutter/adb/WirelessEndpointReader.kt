package com.mute.shutter.adb

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

data class WirelessEndpoint(
    val ip: String,
    val connectPort: Int,
    val ipSource: String,
    val portSource: String,
)

data class DiscoveredEndpoints(
    val ip: String?,
    val ipSource: String?,
    val connectPort: Int?,
    val portSource: String?,
)

class WirelessEndpointReader(private val context: Context) {

    fun readWlanIp(): Pair<String, String>? {
        return NetworkAddressHelper.getLocalIpv4(context.applicationContext)
    }

    fun readConnectPortFromGetprop(): Int? {
        for (prop in CONNECT_PORT_PROPS) {
            readGetprop(prop)?.toIntOrNull()?.takeIf { it in 1..65535 }?.let { return it }
        }
        return null
    }

    fun readConnectPortFromDumpsys(): Int? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("dumpsys", "adb"))
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor()
            parsePortFromText(output)
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePortFromText(text: String): Int? {
        val patterns = listOf(
            Regex("""(?:tls|connect|wireless).*?port[=:\s]+(\d{4,5})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d{4,5})"""),
            Regex("""port\s+(\d{4,5})""", RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            pattern.find(text)?.let { match ->
                val portStr = match.groupValues.lastOrNull { it.matches(Regex("""\d{4,5}""")) }
                portStr?.toIntOrNull()?.takeIf { it in 1024..65535 }?.let { return it }
            }
        }
        return null
    }

    private fun readGetprop(name: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", name))
            val value = BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() }
            process.waitFor()
            value?.trim()?.takeIf { it.isNotEmpty() && it != "0" }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val CONNECT_PORT_PROPS = listOf(
            "service.adb.tls.port",
            "persist.adb.tls_server.port",
        )
    }
}
