package com.mute.shutter.adb

import android.content.Context
import com.mute.shutter.ShutterConstants
import com.mute.shutter.data.SessionPreferences
import com.mute.shutter.debug.DebugLogger
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
    private var selectedSerial: String? = preferences.lastHost?.let { host ->
        preferences.lastConnectPort.takeIf { it in 1..65535 }
            ?.let { BundledAdbRunner.endpointSerial(host, it) }
    }
    private val mutex = Mutex()
    private val reconnectMutex = Mutex()
    private val discoveryMutex = Mutex()
    private var discoveryFinishedAt = Long.MIN_VALUE
    private var discoveryResult = DiscoveredEndpoints(null, null, null, null)
    private val androidDiscovery = AndroidAdbDiscovery(appContext)

    suspend fun pair(host: String, pairPort: Int, pin: String): AdbResult<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                adb.startServer()
                val result = adb.pair(host, pairPort, pin)
                DebugLogger.log("adb pair 원본 출력: ${result.output.take(200)}")
                if (BundledAdbRunner.isPairSuccess(result.output)) {
                    preferences.isPaired = true
                    preferences.lastHost = host
                    AdbResult.Success(Unit)
                } else {
                    AdbResult.Failure(result.output.ifBlank { "exit ${result.exitCode}" })
                }
            } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
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
                    selectedSerial = BundledAdbRunner.endpointSerial(host, connectPort)
                    preferences.lastConnectPort = connectPort
                    preferences.lastHost = host
                    AdbResult.Success(Unit)
                } else {
                    AdbResult.Failure(result.output.ifBlank { "exit ${result.exitCode}" })
                }
            } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
                AdbResult.Failure(e.message ?: "연결 오류")
            }
        }
    }

    /** 저장/감지 IP·포트로 여러 host에 connect 시도. 이미 adb 연결돼 있으면 포트 없이 통과 */
    suspend fun connectAuto(hintPort: Int? = null): AdbResult<String> = reconnectMutex.withLock {
        connectAutoInternal(hintPort)
    }

    private suspend fun connectAutoInternal(hintPort: Int?): AdbResult<String> = withContext(Dispatchers.IO) {
        probeExistingConnection()?.let {
            DebugLogger.logSuccess("기존 ADB 세션 재사용 (host=$it)")
            return@withContext AdbResult.Success(it)
        }

        var lastError = "현재 연결 포트를 찾지 못했습니다. 무선 디버깅을 확인해주세요."
        val attempted = mutableSetOf<Pair<String, Int>>()
        repeat(2) {
            val d = discoverAll()
            val ports = listOfNotNull(d.connectPort, hintPort, preferences.lastConnectPort)
                .filter { it in 1..65535 }.distinct()
            val locals = java.net.NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .flatMap { it.inetAddresses.toList() }.mapNotNull { it.hostAddress }.toSet()
            val hosts = (listOfNotNull(ShutterConstants.LOCALHOST, d.ip, preferences.lastHost) +
                locals.filter { it.startsWith("192.168.") || it.startsWith("10.") })
                .filter { it == ShutterConstants.LOCALHOST || it in locals }.distinct()
            for (port in ports) for (host in hosts) {
                if (!attempted.add(host to port)) continue
                DebugLogger.logInfo("자동 재연결", "$host:$port (${d.portSource})")
                when (val result = connect(host, port)) {
                    is AdbResult.Success -> return@withContext AdbResult.Success(host)
                    is AdbResult.Failure -> lastError = result.message
                }
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
                val serial = selectedSerial
                    ?: return@withContext AdbResult.Failure("선택된 ADB 연결 없음")
                val result = adb.shell(command, serial)
                val output = result.output.trim()
                when {
                    isAdbError(output) -> AdbResult.Failure("기기 연결 안 됨", output)
                    result.success -> AdbResult.Success(output)
                    else -> AdbResult.Failure("shell 실패", output)
                }
            } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
                AdbResult.Failure(e.message ?: "shell 오류")
            }
        }
    }

    /** adb 데몬이 명령 미실행 시 stdout으로 뱉는 오류. 이걸 성공으로 착각하면 안 됨 */
    private fun isAdbError(output: String): Boolean {
        val lower = output.lowercase()
        return "no devices/emulators found" in lower ||
            "device offline" in lower ||
            "device unauthorized" in lower ||
            "failed to connect" in lower ||
            "cannot connect" in lower ||
            "no such device" in lower ||
            lower.startsWith("error:") ||
            lower.startsWith("adb:")
    }

    /** IP와 포트를 각각 감지 — 하나만 찾아도 UI에 반영 */
    suspend fun discoverAll(): DiscoveredEndpoints {
        val requestedAt = android.os.SystemClock.elapsedRealtime()
        return discoveryMutex.withLock {
            // Share only a discovery that finished after this request began.
            if (discoveryFinishedAt >= requestedAt) return@withLock discoveryResult
            discoverSafely().also {
                discoveryResult = it
                discoveryFinishedAt = android.os.SystemClock.elapsedRealtime()
            }
        }
    }

    private suspend fun discoverSafely(): DiscoveredEndpoints = withContext(Dispatchers.IO) {
        try {
            discoverAllInternal()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // adb 바이너리 실행 실패(AdbBinaryLocator.resolve() 등) 같은 예외가 여기까지
            // 안 잡히면 viewModelScope까지 전파돼 앱이 죽는다. 탐지 실패는 빈 결과로 처리.
            DebugLogger.logError("discoverAll 실패", e)
            DiscoveredEndpoints(null, null, null, null)
        }
    }

    private suspend fun discoverAllInternal(): DiscoveredEndpoints {
        androidDiscovery.discover()?.let {
            return DiscoveredEndpoints(it.ip, "NSD", it.port, "NSD")
        }
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

        run {
            val mdns = adb.discoverMdnsEndpoint()
            if (mdns.ip != null) {
                ip = mdns.ip
                ipSource = "mdns"
            }
            if (mdns.port != null) {
                port = mdns.port
                portSource = "mdns"
            }
        }

        if (port == null) {
            port = endpointReader.readConnectPortFromDumpsys()
            portSource = if (port != null) "dumpsys" else null
        }
        if (port == null) {
            port = preferences.lastConnectPort.takeIf { it in 1..65535 }
            portSource = if (port != null) "saved" else null
        }

        return DiscoveredEndpoints(ip, ipSource, port, portSource)
    }

    suspend fun discoverEndpoints(): WirelessEndpoint? {
        val d = discoverAll()
        val ip = d.ip ?: return null
        val port = d.connectPort ?: return null
        return WirelessEndpoint(ip, port, d.ipSource ?: "?", d.portSource ?: "?")
    }
}
