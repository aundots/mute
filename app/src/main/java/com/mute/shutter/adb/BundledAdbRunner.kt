package com.mute.shutter.adb

import android.content.Context
import android.net.wifi.WifiManager
import java.net.NetworkInterface
import kotlinx.coroutines.delay
import com.mute.shutter.debug.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * PC adb 명령을 그대로 실행 (Android 16 무선 디버깅 self-connect).
 */
class BundledAdbRunner(context: Context) {
    data class ProcessResult(
        val exitCode: Int,
        val output: String,
    ) {
        val success: Boolean get() = exitCode == 0
    }

    private val appContext = context.applicationContext
    private val install by lazy { AdbBinaryLocator.resolve(appContext) }
    private val homeDir: File by lazy { appContext.filesDir }

    suspend fun startServer(): ProcessResult = withContext(Dispatchers.IO) {
        runAdb(listOf("start-server"), timeoutSeconds = 15)
    }

    suspend fun killServer(): ProcessResult = withContext(Dispatchers.IO) {
        runAdb(listOf("kill-server"), timeoutSeconds = 10)
    }

    suspend fun pair(host: String, port: Int, pin: String): ProcessResult = withContext(Dispatchers.IO) {
        runAdb(listOf("pair", "$host:$port", pin), timeoutSeconds = 30)
    }

    suspend fun connect(host: String, port: Int): ProcessResult = withContext(Dispatchers.IO) {
        runAdb(listOf("connect", if (':' in host) "[$host]:$port" else "$host:$port"), timeoutSeconds = 5)
    }

    suspend fun shell(command: String, serial: String): ProcessResult = withContext(Dispatchers.IO) {
        runAdb(shellArguments(serial, command), timeoutSeconds = 20)
    }

    suspend fun discoverConnectPortViaMdns(): Int? =
        discoverMdnsEndpoint().port

    suspend fun discoverMdnsEndpoint(): MdnsEndpoint = withContext(Dispatchers.IO) {
        withMulticastLock {
            startServer()
            val locals = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .flatMap { it.inetAddresses.toList() }.mapNotNull { it.hostAddress }.toSet()
            var endpoint = MdnsEndpoint(null, null)
            for (attempt in 0..5) {
                val result = runAdb(listOf("mdns", "services"), timeoutSeconds = 3)
                endpoint = parseMdnsEndpoints(result.output).firstOrNull { it.ip in locals }
                    ?: MdnsEndpoint(null, null)
                if (endpoint.port != null) break
                if (attempt < 5) delay(600)
            }
            DebugLogger.logInfo("mDNS", "ip=${endpoint.ip} port=${endpoint.port}")
            endpoint
        }
    }

    /**
     * CHANGE_WIFI_MULTICAST_STATE 권한만으로는 부족하고 실제 락을 잡아야
     * 일부 기기/드라이버가 mDNS(멀티캐스트 UDP) 패킷을 필터링하지 않는다.
     */
    private suspend fun <T> withMulticastLock(block: suspend () -> T): T {
        val wifi = appContext.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = wifi?.createMulticastLock("mute_mdns_lock")
        try {
            lock?.setReferenceCounted(true)
            lock?.acquire()
            return block()
        } finally {
            if (lock?.isHeld == true) lock.release()
        }
    }

    data class MdnsEndpoint(val ip: String?, val port: Int?)

    private fun runAdb(
        args: List<String>,
        timeoutSeconds: Long = 30,
    ): ProcessResult {
        val adb = install.binary
        val workDir = install.workDir
        val commands = listOf(
            listOf(adb.absolutePath) + args,
            listOf("/system/bin/linker64", adb.absolutePath) + args,
        )
        var last = ProcessResult(-1, "")
        for (command in commands) {
            last = runAdbCommand(command, workDir, timeoutSeconds)
            val denied = last.output.contains("Permission denied", ignoreCase = true) ||
                last.output.contains("error=13", ignoreCase = true)
            if (!denied) return last
        }
        return last
    }

    private fun runAdbCommand(
        command: List<String>,
        workDir: File,
        timeoutSeconds: Long,
    ): ProcessResult {
        return try {
            val processBuilder = ProcessBuilder(command)
                .directory(workDir)
                .redirectErrorStream(true)

            val env = processBuilder.environment()
            env["HOME"] = homeDir.absolutePath
            env["ADB_VENDOR_KEYS"] = homeDir.absolutePath
            env["TMPDIR"] = homeDir.absolutePath
            env["LD_LIBRARY_PATH"] = workDir.absolutePath
            env["ADB_MDNS"] = "1"
            env["ADB_MDNS_OPENSCREEN"] = "1"

            val process = processBuilder.start()
            val output = java.util.concurrent.atomic.AtomicReference("")
            val reader = Thread {
                try { process.inputStream.bufferedReader().use { output.set(it.readText()) } }
                catch (_: java.io.IOException) { /* Closed when the command times out. */ }
            }.apply { isDaemon = true; start() }
            try {
                val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
                if (!finished) process.destroyForcibly()
                reader.join(1000)
                ProcessResult(if (finished) process.exitValue() else -1,
                    output.get().trim() + if (finished) "" else "\n(timeout)")
            } finally {
                if (process.isAlive) process.destroyForcibly()
                process.inputStream.close()
            }

        } catch (e: Exception) {
            ProcessResult(-1, e.message ?: e.javaClass.simpleName)
        }
    }

    companion object {
        fun shellArguments(serial: String, command: String): List<String> =
            listOf("-s", serial, "shell", command)

        fun endpointSerial(host: String, port: Int): String =
            if (':' in host) "[$host]:$port" else "$host:$port"

        fun parseMdnsEndpoints(output: String): List<MdnsEndpoint> =
            Regex("""_adb-tls-connect\._tcp\.?\s+(\d+\.\d+\.\d+\.\d+):(\d+)(?=\s|$)""")
                .findAll(output).mapNotNull { match ->
                    val ip = match.groupValues[1]
                    val port = match.groupValues[2].toIntOrNull()
                    if (port == null || port !in 1..65535 ||
                        ip.split('.').any { (it.toIntOrNull() ?: -1) !in 0..255 }) null
                    else MdnsEndpoint(ip, port)
                }.toList()

        fun isConnectSuccess(output: String): Boolean {
            val lower = output.lowercase()
            return (lower.contains("connected") || lower.contains("already connected")) &&
                !lower.contains("failed to connect") &&
                !lower.contains("connection refused") &&
                !lower.contains("unable to connect")
        }

        fun isPairSuccess(output: String): Boolean =
            output.contains("Successfully paired", ignoreCase = true)
    }
}
