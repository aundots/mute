package com.mute.shutter.adb

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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
        runAdb(listOf("connect", "$host:$port"), timeoutSeconds = 20)
    }

    suspend fun shell(command: String): ProcessResult = withContext(Dispatchers.IO) {
        runAdb(listOf("shell", command), timeoutSeconds = 20)
    }

    suspend fun discoverConnectPortViaMdns(): Int? =
        discoverMdnsEndpoint().port

    suspend fun discoverMdnsEndpoint(): MdnsEndpoint = withContext(Dispatchers.IO) {
        startServer()
        val result = runAdb(listOf("mdns", "services"), timeoutSeconds = 12)
        parseMdnsEndpoint(result.output)
    }

    private fun parseConnectPort(output: String): Int? = parseMdnsEndpoint(output).port

    private fun parseMdnsEndpoint(output: String): MdnsEndpoint {
        val withIp = Regex("""_adb-tls-connect\._tcp\.\s+\S+\s+(\d+\.\d+\.\d+\.\d+):(\d+)""")
            .find(output)
        if (withIp != null) {
            return MdnsEndpoint(
                ip = withIp.groupValues[1],
                port = withIp.groupValues[2].toIntOrNull(),
            )
        }
        val portOnly = Regex("""_adb-tls-connect\._tcp\.\s+\S+\s+(\d+)""")
            .find(output)
        return MdnsEndpoint(
            ip = null,
            port = portOnly?.groupValues?.getOrNull(1)?.toIntOrNull(),
        )
    }

    data class MdnsEndpoint(val ip: String?, val port: Int?)

    private fun runAdb(
        args: List<String>,
        timeoutSeconds: Long = 30,
    ): ProcessResult {
        return try {
            val adb = install.binary
            val workDir = install.workDir
            val command = listOf(adb.absolutePath) + args

            val processBuilder = ProcessBuilder(command)
                .directory(workDir)
                .redirectErrorStream(true)

            val env = processBuilder.environment()
            env["HOME"] = homeDir.absolutePath
            env["ADB_VENDOR_KEYS"] = homeDir.absolutePath
            env["TMPDIR"] = homeDir.absolutePath
            env["LD_LIBRARY_PATH"] = workDir.absolutePath
            // Android 16+: mDNS 비활성 기기에서도 adb 자체 connect는 동작
            env["ADB_MDNS"] = "1"
            if (Build.VERSION.SDK_INT >= 36) {
                env["ADB_MDNS_OPENSCREEN"] = "0"
            }

            val process = processBuilder.start()
            val output = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.appendLine(line)
                }
            }

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                ProcessResult(-1, output.toString().trim() + "\n(timeout)")
            } else {
                ProcessResult(process.exitValue(), output.toString().trim())
            }
        } catch (e: Exception) {
            ProcessResult(-1, e.message ?: e.javaClass.simpleName)
        }
    }

    companion object {
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
