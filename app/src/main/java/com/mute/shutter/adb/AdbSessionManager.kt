package com.mute.shutter.adb

import android.content.Context
import com.mute.shutter.ShutterConstants
import com.mute.shutter.data.SessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class AdbResult<out T> {
    data class Success<T>(val value: T) : AdbResult<T>()
    data class Failure(val message: String, val detail: String = "") : AdbResult<Nothing>()
}

class AdbSessionManager(
    context: Context,
    private val preferences: SessionPreferences,
) {
    private val appContext = context.applicationContext
    private val adb = BundledAdbRunner(appContext)
    private val endpointReader = WirelessEndpointReader(appContext)
    private val mutex = Mutex()

    suspend fun pair(host: String, pairPort: Int, pin: String): AdbResult<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                adb.startServer()
                val result = adb.pair(host, pairPort, pin)
                if (BundledAdbRunner.isPairSuccess(result.output)) {
                    preferences.isPaired = true
                    preferences.lastHost = host
                    AdbResult.Success(Unit)
                } else {
                    AdbResult.Failure(result.output.ifBlank { "exit ${result.exitCode}" })
                }
            } catch (e: Exception) {
                AdbResult.Failure(e.message ?: "페어링 오류")
            }
        }
    }

    suspend fun connect(host: String, connectPort: Int): AdbResult<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                adb.startServer()
                val result = adb.connect(host, connectPort)
                if (BundledAdbRunner.isConnectSuccess(result.output)) {
                    preferences.isPaired = true
                    preferences.lastConnectPort = connectPort
                    preferences.lastHost = host
                    AdbResult.Success(Unit)
                } else {
                    AdbResult.Failure(result.output.ifBlank { "exit ${result.exitCode}" })
                }
            } catch (e: Exception) {
                AdbResult.Failure(e.message ?: "연결 오류")
            }
        }
    }

    /** 저장/감지 IP·포트로 여러 host에 connect 시도. 이미 adb 연결돼 있으면 포트 없이 통과 */
    suspend fun connectAuto(hintPort: Int? = null): AdbResult<String> = withContext(Dispatchers.IO) {
        probeExistingConnection()?.let { return@withContext AdbResult.Success(it) }

        val d = discoverAll()
        val port = hintPort?.takeIf { it in 1..65535 }
            ?: d.connectPort
            ?: preferences.lastConnectPort.takeIf { it in 1..65535 }
            ?: endpointReader.readConnectPortFromDumpsys()
            ?: return@withContext AdbResult.Failure("연결 포트 없음")

        if (port != preferences.lastConnectPort) {
            preferences.lastConnectPort = port
        }

        val hosts = linkedSetOf<String>()
        hosts.add(ShutterConstants.LOCALHOST)
        hosts.add("localhost")
        preferences.lastHost?.let { hosts.add(it) }
        d.ip?.let { hosts.add(it) }
        NetworkAddressHelper.getLocalIpv4(appContext)?.first?.let { hosts.add(it) }

        var lastError = "연결 실패"
        for (host in hosts) {
            when (val result = connect(host, port)) {
                is AdbResult.Success -> return@withContext AdbResult.Success(host)
                is AdbResult.Failure -> lastError = result.message
            }
        }

        probeExistingConnection()?.let { return@withContext AdbResult.Success(it) }
        AdbResult.Failure(lastError)
    }

    suspend fun testConnection(): AdbResult<Unit> = withContext(Dispatchers.IO) {
        probeExistingConnection()?.let { return@withContext AdbResult.Success(Unit) }
        when (val result = connectAuto()) {
            is AdbResult.Success -> AdbResult.Success(Unit)
            is AdbResult.Failure -> result
        }
    }

    /** adb connect 없이 shell이 되는지 (이전 세션 유지) */
    private suspend fun probeExistingConnection(): String? {
        return when (val result = shell("echo mute_ok")) {
            is AdbResult.Success -> {
                if (result.value.contains("mute_ok")) {
                    preferences.isPaired = true
                    preferences.lastHost ?: ShutterConstants.LOCALHOST
                } else {
                    null
                }
            }
            is AdbResult.Failure -> null
        }
    }

    suspend fun shell(command: String): AdbResult<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val result = adb.shell(command)
                if (result.success || result.output.isNotBlank()) {
                    AdbResult.Success(result.output.trim())
                } else {
                    AdbResult.Failure("shell 실패", result.output)
                }
            } catch (e: Exception) {
                AdbResult.Failure(e.message ?: "shell 오류")
            }
        }
    }

    /** IP와 포트를 각각 감지 — 하나만 찾아도 UI에 반영 */
    suspend fun discoverAll(): DiscoveredEndpoints = withContext(Dispatchers.IO) {
        var ip: String? = null
        var ipSource: String? = null
        endpointReader.readWlanIp()?.let {
            ip = it.first
            ipSource = it.second
        }
        if (ip == null) {
            preferences.lastHost?.let {
                ip = it
                ipSource = "saved"
            }
        }

        var port = endpointReader.readConnectPortFromGetprop()
        var portSource: String? = if (port != null) "getprop" else null

        if (ip == null || port == null) {
            val mdns = adb.discoverMdnsEndpoint()
            if (ip == null && mdns.ip != null) {
                ip = mdns.ip
                ipSource = "mdns"
            }
            if (port == null && mdns.port != null) {
                port = mdns.port
                portSource = "mdns"
            }
        }

        if (port == null) {
            port = preferences.lastConnectPort.takeIf { it in 1..65535 }
            portSource = if (port != null) "saved" else null
        }
        if (port == null) {
            port = endpointReader.readConnectPortFromDumpsys()
            portSource = if (port != null) "dumpsys" else null
        }

        if (port != null && port != preferences.lastConnectPort) {
            preferences.lastConnectPort = port
        }

        DiscoveredEndpoints(ip, ipSource, port, portSource)
    }

    suspend fun discoverEndpoints(): WirelessEndpoint? {
        val d = discoverAll()
        val ip = d.ip ?: return null
        val port = d.connectPort ?: return null
        return WirelessEndpoint(ip, port, d.ipSource ?: "?", d.portSource ?: "?")
    }
}
