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
                when (val read = read()) {
                    is AdbResult.Success -> {
                        if (isMutedValue(read.value)) {
                            preferences.lastMuteValue = ShutterConstants.MUTED_VALUE
                            AdbResult.Success(read.value)
                        } else {
                            preferences.lastMuteValue = ShutterConstants.MUTED_VALUE
                            AdbResult.Success(ShutterConstants.MUTED_VALUE)
                        }
                    }
                    is AdbResult.Failure -> {
                        preferences.lastMuteValue = ShutterConstants.MUTED_VALUE
                        AdbResult.Success(ShutterConstants.MUTED_VALUE)
                    }
                }
            }
            is AdbResult.Failure -> result
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

    companion object {
        fun isMutedValue(raw: String): Boolean = raw.trim() == ShutterConstants.MUTED_VALUE
    }
}
