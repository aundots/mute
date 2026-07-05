package com.mute.shutter.adb

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

/**
 * Android 16 / Samsung Wi‑Fi IPv4 자동 감지.
 */
object NetworkAddressHelper {

    fun getLocalIpv4(context: Context): Pair<String, String>? {
        val app = context.applicationContext
        val steps = mutableListOf<String>()

        val attempts: List<() -> Pair<String, String>?> = listOf(
            { fromConnectivity(app).also { if (it == null) steps += "connectivity" } },
            { fromIpRoute().also { if (it == null) steps += "ip-route" } },
            { fromIpGlobal().also { if (it == null) steps += "ip-global" } },
            { fromWifiManager(app).also { if (it == null) steps += "wifi" } },
            { fromGetpropDhcp().also { if (it == null) steps += "getprop" } },
            { fromIpCommand().also { if (it == null) steps += "ip-iface" } },
            { fromNetworkInterfaces().also { if (it == null) steps += "netif" } },
        )

        for (attempt in attempts) {
            attempt()?.let { return it }
        }
        lastFailureHint = steps.joinToString(", ")
        return null
    }

    var lastFailureHint: String? = null
        private set

    private fun fromConnectivity(context: Context): Pair<String, String>? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null

        cm.activeNetwork?.let { network ->
            val caps = cm.getNetworkCapabilities(network)
            val link = cm.getLinkProperties(network)
            if (caps != null && link != null) {
                val source = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "connectivity-wifi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "connectivity-eth"
                    else -> "connectivity-active"
                }
                pickFromLinkAddresses(link.linkAddresses.map { it.address }, source)?.let { return it }
            }
        }

        @Suppress("DEPRECATION")
        val networks = cm.allNetworks
        for (network in networks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
            val link = cm.getLinkProperties(network) ?: continue
            pickFromLinkAddresses(link.linkAddresses.map { it.address }, "connectivity-wifi")?.let { return it }
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun fromWifiManager(context: Context): Pair<String, String>? {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null

        wifi.dhcpInfo?.ipAddress?.takeIf { it != 0 }?.let { ipInt ->
            intToIpv4(ipInt)?.let { ip -> if (isUsableIpv4(ip)) return ip to "wifi-dhcp" }
        }

        wifi.connectionInfo?.ipAddress?.takeIf { it != 0 }?.let { ipInt ->
            intToIpv4(ipInt)?.let { ip -> if (isUsableIpv4(ip)) return ip to "wifi-info" }
        }
        return null
    }

    /** 가장 신뢰도 높음: 기본 라우트의 src IP */
    private fun fromIpRoute(): Pair<String, String>? {
        val output = runCommand(arrayOf("ip", "route", "get", "1.1.1.1")) ?: return null
        Regex("""\bsrc\s+(\d+\.\d+\.\d+\.\d+)""").find(output)?.groupValues?.getOrNull(1)?.let { ip ->
            if (isUsableIpv4(ip)) return ip to "ip-route"
        }
        return null
    }

    private fun fromIpGlobal(): Pair<String, String>? {
        val output = runCommand(arrayOf("ip", "-4", "-o", "addr", "show", "scope", "global")) ?: return null
        val ips = Regex("""inet\s+(\d+\.\d+\.\d+\.\d+)""")
            .findAll(output)
            .mapNotNull { it.groupValues.getOrNull(1) }
            .toList()
        return pickBest(ips)?.let { it to "ip-global" }
    }

    private fun fromGetpropDhcp(): Pair<String, String>? {
        val props = listOf(
            "dhcp.wlan0.ipaddress",
            "dhcp.wlan1.ipaddress",
            "dhcp.swlan0.ipaddress",
            "dhcp.wlan2.ipaddress",
        )
        for (prop in props) {
            readGetprop(prop)?.let { ip -> return ip to "getprop" }
        }
        val allProps = runCommand(arrayOf("getprop")) ?: return null
        Regex("""\[dhcp\.(\w+)\.ipaddress\]:\s*\[(\d+\.\d+\.\d+\.\d+)\]""")
            .findAll(allProps)
            .mapNotNull { it.groupValues.getOrNull(2) }
            .forEach { ip ->
                if (isUsableIpv4(ip)) return ip to "getprop-scan"
            }
        return null
    }

    private fun fromIpCommand(): Pair<String, String>? {
        for (iface in listOf("wlan0", "wlan1", "swlan0", "eth0", "ap0")) {
            val output = runCommand(arrayOf("ip", "-4", "addr", "show", iface)) ?: continue
            Regex("""inet\s+(\d+\.\d+\.\d+\.\d+)""").find(output)?.groupValues?.getOrNull(1)?.let { ip ->
                if (isUsableIpv4(ip)) return ip to "ip-$iface"
            }
        }
        return null
    }

    private fun fromNetworkInterfaces(): Pair<String, String>? {
        val ips = try {
            NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .sortedByDescending { it.name.startsWith("wlan") || it.name.contains("wlan") }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .mapNotNull { addr ->
                    if (addr is Inet4Address && !addr.isLoopbackAddress) addr.hostAddress else null
                }
                .distinct()
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
        return pickBest(ips)?.let { it to "netif" }
    }

    private fun pickFromLinkAddresses(
        addresses: List<java.net.InetAddress>,
        source: String,
    ): Pair<String, String>? {
        val ips = addresses.mapNotNull { addr ->
            if (addr is Inet4Address && !addr.isLoopbackAddress) addr.hostAddress else null
        }
        return pickBest(ips)?.let { it to source }
    }

    private fun pickBest(ips: List<String>): String? {
        return ips.firstOrNull { it.startsWith("192.168.") }
            ?: ips.firstOrNull { it.startsWith("10.") }
            ?: ips.firstOrNull { it.startsWith("172.") && isPrivate172(it) }
            ?: ips.firstOrNull { isUsableIpv4(it) }
    }

    private fun intToIpv4(ip: Int): String? {
        if (ip == 0) return null
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff,
        )
    }

    private fun isPrivate172(ip: String): Boolean {
        val second = ip.split('.').getOrNull(1)?.toIntOrNull() ?: return false
        return second in 16..31
    }

    private fun isUsableIpv4(ip: String?): Boolean {
        if (ip.isNullOrBlank() || ip.contains(':')) return false
        if (ip == "0.0.0.0" || ip.startsWith("127.")) return false
        return true
    }

    private fun readGetprop(name: String): String? {
        val value = runCommand(arrayOf("getprop", name))?.trim()
        return value?.takeIf { isUsableIpv4(it) }
    }

    private fun runCommand(command: Array<String>): String? {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
