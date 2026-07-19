package com.mute.shutter.debug

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private const val TAG = "MuteDebug"
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        logFile = File(context.filesDir, "mute_debug.log")
        if (logFile?.length() ?: 0 > 1_000_000) {
            logFile?.delete()
        }
        log("=== 디버그 로그 시작 ===")
    }

    fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] $message"

        Log.d(TAG, message)

        try {
            logFile?.appendText("$logMessage\n")
        } catch (e: Exception) {
            Log.e(TAG, "로그 파일 쓰기 실패", e)
        }
    }

    fun logError(message: String, exception: Exception? = null) {
        val fullMessage = if (exception != null) {
            "$message\n${exception.stackTraceToString()}"
        } else {
            message
        }
        log("❌ $fullMessage")
    }

    fun logSuccess(message: String) {
        log("✅ $message")
    }

    fun logInfo(title: String, content: String) {
        log("ℹ️ [$title] $content")
    }

    fun getLogs(): String {
        return try {
            logFile?.readText() ?: "로그 파일이 없습니다"
        } catch (e: Exception) {
            "로그 읽기 실패: ${e.message}"
        }
    }

    fun clearLogs() {
        try {
            logFile?.delete()
            log("로그 초기화됨")
        } catch (e: Exception) {
            Log.e(TAG, "로그 삭제 실패", e)
        }
    }

    fun getLogFile(): File? = logFile
}
