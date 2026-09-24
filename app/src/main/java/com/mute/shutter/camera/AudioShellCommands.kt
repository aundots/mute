package com.mute.shutter.camera

/**
 * 셔터음 무음에 필요한 셸 명령만 남겼다.
 * 벨소리 모드(set-ringer-mode 계열)는 사용자 무음/진동 설정을 망가뜨렸던 전력이 있어
 * 다시 추가하지 말 것 (CameraMuteController 주석 참고).
 */
object AudioShellCommands {
    /** 첫 빌드에서 S26 등에 동작했던 방식 */
    fun getStreamVolumeLegacy(stream: Int): String = "cmd audio get-stream-volume $stream"

    fun setStreamVolumeLegacy(stream: Int, level: Int): String =
        "cmd audio set-stream-volume $stream $level 0"

    fun getStreamVolume(stream: Int): String =
        "cmd media_session volume --stream $stream --get"

    fun setStreamVolume(stream: Int, level: Int): String =
        "cmd media_session volume --stream $stream --set $level"

    /**
     * 볼륨 읽기 출력 파싱.
     * `cmd audio get-stream-volume 1`은 "AudioManager.getStreamVolume(1) -> 6" 형태를 주는데,
     * 첫 숫자를 집으면 괄호 안 스트림 번호(1)를 음량으로 오인한다(실제로 겪은 버그).
     * "->"가 있으면 그 뒤의 숫자를, 없으면 첫 숫자를 취한다.
     */
    fun parseVolumeOutput(output: String): Int? {
        val trimmed = output.trim()
        trimmed.toIntOrNull()?.let { return it }

        val afterArrow = trimmed.substringAfter("->", missingDelimiterValue = "")
        val target = if (afterArrow.isNotBlank()) afterArrow else trimmed
        return Regex("""\d+""").find(target)?.value?.toIntOrNull()
    }

    fun getSystemSettings(): String = "settings get system csc_pref_camera_forced_shuttersound_key"

    fun getGlobalSettings(): String = "settings get global csc_pref_camera_forced_shuttersound_key"

    fun getSecureSettings(): String = "settings get secure sound_effects_enabled"
}
