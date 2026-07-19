package com.mute.shutter.shutter

import com.mute.shutter.ShutterConstants
import com.mute.shutter.adb.AdbResult
import com.mute.shutter.adb.AdbSessionManager
import com.mute.shutter.data.SessionPreferences
import com.mute.shutter.debug.DebugLogger

class ShutterSoundController(
    private val adb: AdbSessionManager,
    private val preferences: SessionPreferences,
) {
    suspend fun mute(): AdbResult<String> {
        val commands = listOf(
            "settings put system ${ShutterConstants.SETTINGS_KEY} ${ShutterConstants.MUTED_VALUE}",
            "settings put global ${ShutterConstants.SETTINGS_KEY} ${ShutterConstants.MUTED_VALUE}",
            "settings put global csc_pref_camera_forced_shuttersound_key ${ShutterConstants.MUTED_VALUE}",
            "settings put global camera_sound 0",
            "settings put global camera_shutter_sound 0",
            "settings put secure sound_effects_enabled 0",
            "settings put global com.samsung.android.app.camera_shuttersound 0",
            "settings put global com.sec.android.app.camera_shuttersound 0",
        )

        DebugLogger.log("▶ 셔터음 설정 명령 실행 시작 (${commands.size}개)")
        var anySuccess = false
        for (command in commands) {
            when (val result = adb.shell(command)) {
                is AdbResult.Success -> {
                    anySuccess = true
                    DebugLogger.logSuccess("$command → OK")
                }
                is AdbResult.Failure -> DebugLogger.logError("$command → 실패: ${result.message} ${result.detail}")
            }
        }

        logSettings()

        // settings get으로 실제 저장된 값이 0인지 검증 — 이게 진짜 성공 판정
        return when (val read = read()) {
            is AdbResult.Success -> {
                if (isMutedValue(read.value)) {
                    preferences.lastMuteValue = ShutterConstants.MUTED_VALUE
                    DebugLogger.logSuccess("셔터음 설정 확정 (값=0)")
                    AdbResult.Success(read.value)
                } else {
                    DebugLogger.logError("설정이 적용되지 않음 (읽은 값='${read.value}') — 기기 연결 확인 필요")
                    AdbResult.Failure("설정 미적용", read.value)
                }
            }
            is AdbResult.Failure -> {
                DebugLogger.logError("설정 확인 실패: ${read.message} — ADB 연결 안 됨")
                AdbResult.Failure("기기 연결 안 됨", read.detail.ifBlank { read.message })
            }
        }
    }

    suspend fun read(): AdbResult<String> {
        return when (val result = adb.shell("settings get system ${ShutterConstants.SETTINGS_KEY}")) {
            is AdbResult.Success -> {
                val value = result.value.trim()
                if (isMutedValue(value)) {
                    preferences.lastMuteValue = ShutterConstants.MUTED_VALUE
                }
                AdbResult.Success(value)
            }
            is AdbResult.Failure -> result
        }
    }

    private suspend fun logSettings() {
        DebugLogger.log("▶ 설정 적용 결과 검증 (settings get)")
        listOf(
            "settings get system ${ShutterConstants.SETTINGS_KEY}",
            "settings get global ${ShutterConstants.SETTINGS_KEY}",
            "settings get global camera_sound",
            "settings get global camera_shutter_sound",
        ).forEach { cmd ->
            when (val result = adb.shell(cmd)) {
                is AdbResult.Success -> DebugLogger.logInfo("확인", "$cmd = ${result.value.trim()}")
                is AdbResult.Failure -> DebugLogger.logError("$cmd 읽기 실패: ${result.message}")
            }
        }
    }

    companion object {
        fun isMutedValue(raw: String): Boolean = raw.trim() == ShutterConstants.MUTED_VALUE
    }
}
