package com.mute.shutter.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.mute.shutter.debug.DebugLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.NetworkInterface

/** Uses Android's system resolver, independently of the bundled adb mDNS backend. */
class AndroidAdbDiscovery(context: Context) {
    private val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    @Suppress("DEPRECATION")
    suspend fun discover(): BundledAdbRunner.MdnsEndpoint? = withContext(Dispatchers.Main) {
        val services = Channel<NsdServiceInfo>(Channel.UNLIMITED)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) { DebugLogger.logInfo("NSD", "검색 시작") }
            override fun onDiscoveryStopped(type: String) = Unit
            override fun onServiceLost(service: NsdServiceInfo) = Unit
            override fun onServiceFound(service: NsdServiceInfo) {
                services.trySend(service)
            }
            override fun onStartDiscoveryFailed(type: String, code: Int) {
                DebugLogger.logInfo("NSD", "검색 시작 실패: $code")
                services.close()
            }
            override fun onStopDiscoveryFailed(type: String, code: Int) {
                DebugLogger.logInfo("NSD", "검색 종료 실패: $code")
            }
        }
        try {
            manager.discoverServices("_adb-tls-connect._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
            withTimeoutOrNull(8000) {
                // Resolve serially: older Android versions reject simultaneous resolutions.
                for (service in services) {
                    val resolved = CompletableDeferred<NsdServiceInfo?>()
                    val resolver = object : NsdManager.ResolveListener {
                        override fun onServiceResolved(info: NsdServiceInfo) { resolved.complete(info) }
                        override fun onResolveFailed(info: NsdServiceInfo, code: Int) {
                            DebugLogger.logInfo("NSD", "주소 확인 실패: $code")
                            resolved.complete(null)
                        }
                    }
                    try {
                        manager.resolveService(service, resolver)
                        val info = withTimeoutOrNull(2000) { resolved.await() } ?: continue
                        val ip = info.host?.hostAddress ?: continue
                        val locals = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                            .flatMap { it.inetAddresses.toList() }.mapNotNull { it.hostAddress }.toSet()
                        if (isLocalEndpoint(ip, info.port, locals)) {
                            DebugLogger.logInfo("NSD", "현재 휴대폰 발견: $ip:${info.port}")
                            return@withTimeoutOrNull BundledAdbRunner.MdnsEndpoint(ip, info.port)
                        }
                    } finally {
                        if (!resolved.isCompleted && Build.VERSION.SDK_INT >= 34) {
                            runCatching { manager.stopServiceResolution(resolver) }
                        }
                        resolved.cancel()
                    }
                }
                null
            }.also { if (it == null) DebugLogger.logInfo("NSD", "현재 휴대폰 연결 포트 미발견") }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            DebugLogger.logError("NSD 검색 오류", e)
            null
        } finally {
            runCatching { manager.stopServiceDiscovery(listener) }
            services.close()
        }
    }

    companion object {
        fun isLocalEndpoint(ip: String, port: Int, locals: Set<String>): Boolean =
            ip in locals && port in 1..65535
    }
}
