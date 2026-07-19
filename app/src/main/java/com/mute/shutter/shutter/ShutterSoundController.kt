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
        val commands = listOf(
            "settings put system ${ShutterConstants.SETTINGS_KEY} ${ShutterConstants.MUTED_VALUE}",
            "settings put global ${ShutterConstants.SETTINGS_KEY} ${ShutterConstants.MUTED_VALUE}",
            "settings put global csc_pref_camera_forced_shuttersound_key ${ShutterConstants.MUTED_VALUE}",
        )

        var lastFailure: AdbResult.Failure? = null
        for (command in commands) {
            val result = adb.shell(command)
            if (result is AdbResult.Failure) {
                lastFailure = result
            }
        }

        return when (val read = read()) {
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
