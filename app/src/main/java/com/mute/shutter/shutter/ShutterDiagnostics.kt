package com.mute.shutter.shutter

import com.mute.shutter.adb.AdbResult
import com.mute.shutter.adb.AdbSessionManager
import com.mute.shutter.debug.DebugLogger

/**
 * 셔터음이 계속 나는 원인을 한 번의 로그로 판별하기 위한 진단 덤프.
 *
 * 원인은 크게 둘인데 대응이 완전히 다르다:
 *  A) ADB 연결 실패 → 설정 자체가 안 써짐
 *  B) 설정은 0으로 써졌는데도 소리가 남 → 삼성이 펌웨어에서 적용 키/방식을 바꾼 것
 *
 * B라면 "어떤 키로 바뀌었는지"를 알아야 고칠 수 있으므로, 우리가 아는 키만 보지 않고
 * 기기의 셔터/카메라/사운드 관련 설정을 전부 훑어서 남긴다.
 *
 * 기기 쪽 grep/toybox 동작에 의존하지 않도록 필터링은 코틀린에서 한다.
 */
class ShutterDiagnostics(private val adb: AdbSessionManager) {

    suspend fun dump() {
        DebugLogger.log("========== 진단 덤프 시작 ==========")
        dumpDeviceInfo()
        for (namespace in listOf("system", "global", "secure")) {
            dumpSettings(namespace)
        }
        DebugLogger.log("========== 진단 덤프 끝 ==========")
    }

    private suspend fun dumpDeviceInfo() {
        DebugLogger.log("--- 기기 정보 ---")
        PROPS.forEach { prop ->
            val value = shellOrNull("getprop $prop")?.trim().orEmpty()
            if (value.isNotBlank()) {
                DebugLogger.logInfo(prop, value)
            }
        }
    }

    private suspend fun dumpSettings(namespace: String) {
        val output = shellOrNull("settings list $namespace")
        if (output == null) {
            DebugLogger.logError("settings list $namespace 읽기 실패")
            return
        }

        val matches = output.lineSequence()
            .map { it.trim() }
            .filter { line -> line.isNotBlank() && KEYWORDS.any { line.contains(it, ignoreCase = true) } }
            .take(MAX_LINES_PER_NAMESPACE)
            .toList()

        DebugLogger.log("--- $namespace (관련 키 ${matches.size}개) ---")
        matches.forEach { DebugLogger.log("    $it") }
        if (matches.isEmpty()) {
            DebugLogger.log("    (관련 키 없음)")
        }
    }

    private suspend fun shellOrNull(command: String): String? =
        when (val result = adb.shell(command)) {
            is AdbResult.Success -> result.value
            is AdbResult.Failure -> null
        }

    private companion object {
        val PROPS = listOf(
            "ro.product.model",
            "ro.build.version.release",
            "ro.build.version.sdk",
            "ro.build.version.oneui",
            "ro.build.display.id",
            "ro.csc.sales_code",
            "ro.boot.sales_code",
            "ro.csc.country_code",
        )

        /** 셔터음 적용 키가 이 중 하나에 걸리도록 넓게 잡는다 */
        val KEYWORDS = listOf("shutter", "camera", "sound")

        const val MAX_LINES_PER_NAMESPACE = 60
    }
}
