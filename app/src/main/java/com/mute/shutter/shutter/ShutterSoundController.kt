package com.mute.shutter.shutter

import com.mute.shutter.ShutterConstants
import com.mute.shutter.adb.AdbResult
import com.mute.shutter.adb.AdbSessionManager
import com.mute.shutter.data.SessionPreferences

class ShutterSoundController(
    private val adb: AdbSessionManager,
    private val preferences: SessionPreferences,
) {
    suspend fun mute(): AdbResult<String> {
        val result = adb.shell("settings put system ${ShutterConstants.SETTINGS_KEY} ${ShutterConstants.MUTED_VALUE}")
        return when (result) {
            is AdbResult.Success -> {
                val read = read()
                if (read is AdbResult.Success && read.value.trim() == ShutterConstants.MUTED_VALUE) {
                    preferences.lastMuteValue = ShutterConstants.MUTED_VALUE
                    AdbResult.Success(read.value)
                } else {
                    AdbResult.Failure("설정 적용 후 확인 실패", (read as? AdbResult.Failure)?.detail.orEmpty())
                }
            }
            is AdbResult.Failure -> result
        }
    }

    suspend fun read(): AdbResult<String> {
        return when (val result = adb.shell("settings get system ${ShutterConstants.SETTINGS_KEY}")) {
            is AdbResult.Success -> {
                preferences.lastMuteValue = result.value.trim()
                result
            }
            is AdbResult.Failure -> result
        }
    }
}
