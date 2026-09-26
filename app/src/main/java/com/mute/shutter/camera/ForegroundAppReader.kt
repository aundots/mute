package com.mute.shutter.camera

import com.mute.shutter.ShutterConstants
import com.mute.shutter.adb.AdbResult
import com.mute.shutter.adb.AdbSessionManager

/** ADB dumpsys로 실제 포그라운드 앱을 확인 (사용 통계보다 정확/즉각) */
object ForegroundAppReader {
    private val commands = listOf(
        "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'",
        "dumpsys activity activities",
        "dumpsys window",
    )

    suspend fun isCameraForeground(adb: AdbSessionManager): Boolean? {
        for (command in commands) {
            when (val result = adb.shell(command)) {
                is AdbResult.Success -> {
                    val pkg = parsePackage(result.value) ?: continue
                    return ShutterConstants.isCameraPackage(pkg)
                }
                is AdbResult.Failure -> Unit
            }
        }
        return null
    }

    fun parsePackage(output: String): String? {
        val patterns = listOf(
            Regex("""mResumedActivity[^\n]*?\s([\w.]+)/"""),
            Regex("""topResumedActivity[^\n]*?\s([\w.]+)/"""),
            Regex("""mCurrentFocus=[^\n]*?\s([\w.]+)/"""),
            Regex("""mFocusedApp=[^\n]*?\s([\w.]+)/"""),
        )
        for (line in output.lineSequence()) {
            for (pattern in patterns) {
                pattern.find(line)?.groupValues?.getOrNull(1)?.let { return it }
            }
        }
        return null
    }
}
