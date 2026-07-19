package com.mute.shutter.camera

object AudioShellCommands {
    fun getRingerMode(): String = "cmd audio get-ringer-mode"

    fun setRingerModeSilent(): String = "cmd audio set-ringer-mode silent"

    fun setRingerModeNormal(): String = "cmd audio set-ringer-mode normal"

    fun setRingerModeVibrate(): String = "cmd audio set-ringer-mode vibrate"

    /** 첫 빌드에서 S26 등에 동작했던 방식 */
    fun getStreamVolumeLegacy(stream: Int): String = "cmd audio get-stream-volume $stream"

    fun setStreamVolumeLegacy(stream: Int, level: Int): String =
        "cmd audio set-stream-volume $stream $level 0"

    fun getStreamVolume(stream: Int): String =
        "cmd media_session volume --stream $stream --get"

    fun setStreamVolume(stream: Int, level: Int): String =
        "cmd media_session volume --stream $stream --set $level"

    fun setRingerModeSilentForce(): String = "cmd audio set-ringer-mode silent force"

    fun setStreamVolumeMute(stream: Int): String =
        "cmd audio set-stream-volume $stream 0 0 force"

    fun parseVolumeOutput(output: String): Int? {
        val trimmed = output.trim()
        trimmed.toIntOrNull()?.let { return it }
        return trimmed.lineSequence()
            .flatMap { line -> Regex("""\d+""").findAll(line).map { it.value.toInt() } }
            .firstOrNull()
    }

    fun parseRingerMode(output: String): Int? {
        val trimmed = output.trim().lowercase()
        return when {
            "silent" in trimmed -> 0
            "vibrate" in trimmed -> 1
            "normal" in trimmed -> 2
            else -> trimmed.toIntOrNull()
        }
    }

    fun getSystemSettings(): String = "settings get system csc_pref_camera_forced_shuttersound_key"

    fun getGlobalSettings(): String = "settings get global csc_pref_camera_forced_shuttersound_key"

    fun getSecureSettings(): String = "settings get secure sound_effects_enabled"

    fun disableCameraAudioFocus(): String = "cmd audio get-ringer-mode"

    fun muteAllAudio(): String = "am broadcast -a android.media.VOLUME_CHANGED_ACTION --ei android.media.EXTRA_VOLUME_STREAM_TYPE 1 --ei android.media.EXTRA_VOLUME_STREAM_VALUE 0"
}
