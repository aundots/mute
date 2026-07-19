package com.mute.shutter.shutter

import android.util.Log
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
            "settings put global camera_sound 0",
            "settings put global camera_shutter_sound 0",
            "settings put secure sound_effects_enabled 0",
            "settings put global com.samsung.android.app.camera_shuttersound 0",
            "settings put global com.sec.android.app.camera_shuttersound 0",
        )

        for (command in commands) {
            adb.shell(command)
        }

        logSettings()
        return when (val read = read()) {
            is AdbResult.Success -> {
                preferences.lastMuteValue = ShutterConstants.MUTED_VALUE
                AdbResult.Success(read.value)
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

    private suspend fun logSettings() {
        listOf(
            "settings get system ${ShutterConstants.SETTINGS_KEY}",
            "settings get global ${ShutterConstants.SETTINGS_KEY}",
            "settings get global camera_sound",
            "settings get global camera_shutter_sound",
        ).forEach { cmd ->
            when (val result = adb.shell(cmd)) {
                is AdbResult.Success -> Log.d(TAG, "$cmd = ${result.value.trim()}")
                is AdbResult.Failure -> Log.d(TAG, "$cmd failed")
            }
        }
    }

    companion object {
        private const val TAG = "ShutterSound"
        fun isMutedValue(raw: String): Boolean = raw.trim() == ShutterConstants.MUTED_VALUE
    }
}
