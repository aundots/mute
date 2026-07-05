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
}
