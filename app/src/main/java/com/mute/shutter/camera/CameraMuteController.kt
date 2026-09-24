package com.mute.shutter.camera

import com.mute.shutter.ShutterConstants
import com.mute.shutter.adb.AdbResult
import com.mute.shutter.adb.AdbSessionManager
import com.mute.shutter.debug.DebugLogger

/**
 * ADB로 셔터음 설정 키 + 셔터 관련 스트림(SYSTEM/SYSTEM_ENFORCED)만 무음 처리.
 *
 * 벨소리 모드와 알림/벨/미디어 스트림은 절대 건드리지 않는다:
 * - S26U(Android 16)에서 `cmd audio get-ringer-mode`가 빈 값을 줘 원래 모드를 잃고
 *   NORMAL로 밀어버리는 사고가 났었다(사용자 무음/진동 해제).
 * - 진동·무음 모드에서 알림/벨 음량을 읽으면 0이라 복구가 원래 음량을 0으로 덮어쓴다.
 * 셔터음은 설정 키가 실제로 잠재운다(S26U에서 스트림7=15인데도 무음 확인).
 */
class CameraMuteController(private val adb: AdbSessionManager) {
    private var savedSoundEffects: Int? = null
    private val savedVolumes = mutableMapOf<Int, Int>()
    private var muted = false
    var lastError: String? = null
        private set

    suspend fun muteForCamera(): AdbResult<Unit> {
        if (savedSoundEffects == null) {
            when (val fx = adb.shell(AudioShellCommands.getSecureSettings())) {
                is AdbResult.Success -> savedSoundEffects = fx.value.trim().toIntOrNull()
                is AdbResult.Failure -> Unit
            }
        }

        for (stream in muteStreams) {
            if (!savedVolumes.containsKey(stream)) {
                readStreamVolume(stream)?.let { savedVolumes[stream] = it }
            }
        }

        var anyFailure: String? = null
        for (command in muteCommands()) {
            when (val result = adb.shell(command)) {
                is AdbResult.Failure -> {
                    anyFailure = result.message
                    DebugLogger.logError("$command → ${result.message}")
                }
                is AdbResult.Success -> DebugLogger.logSuccess("$command → OK")
            }
        }

        logSettings()
        lastError = anyFailure
        muted = true
        return if (anyFailure == null) AdbResult.Success(Unit) else AdbResult.Failure("일부 무음 명령 실패", anyFailure)
    }

    suspend fun restoreAfterCamera(): AdbResult<Unit> {
        if (!muted) return AdbResult.Success(Unit)

        for ((stream, level) in savedVolumes) {
            adb.shell(AudioShellCommands.setStreamVolumeLegacy(stream, level))
            adb.shell(AudioShellCommands.setStreamVolume(stream, level))
        }
        savedVolumes.clear()

        // 터치음은 원래 값으로만 되돌린다(무조건 1로 켜면 꺼둔 사용자에게 소리가 생김).
        savedSoundEffects?.let { adb.shell("settings put secure sound_effects_enabled $it") }
        savedSoundEffects = null
        muted = false
        return AdbResult.Success(Unit)
    }

    fun isMuted(): Boolean = muted

    private suspend fun readStreamVolume(stream: Int): Int? {
        when (val media = adb.shell(AudioShellCommands.getStreamVolume(stream))) {
            is AdbResult.Success -> AudioShellCommands.parseVolumeOutput(media.value)?.let { return it }
            is AdbResult.Failure -> Unit
        }
        return when (val legacy = adb.shell(AudioShellCommands.getStreamVolumeLegacy(stream))) {
            is AdbResult.Success -> AudioShellCommands.parseVolumeOutput(legacy.value)
            is AdbResult.Failure -> null
        }
    }

    private suspend fun logSettings() {
        when (val sys = adb.shell(AudioShellCommands.getSystemSettings())) {
            is AdbResult.Success -> DebugLogger.logInfo("셔터키(system)", sys.value.trim())
            is AdbResult.Failure -> DebugLogger.logError("셔터키(system) 읽기 실패")
        }
        when (val glb = adb.shell(AudioShellCommands.getGlobalSettings())) {
            is AdbResult.Success -> DebugLogger.logInfo("셔터키(global)", glb.value.trim())
            is AdbResult.Failure -> DebugLogger.logError("셔터키(global) 읽기 실패")
        }
    }

    private fun muteCommands(): List<String> = buildList {
        add("settings put system ${ShutterConstants.SETTINGS_KEY} 0")
        add("settings put global ${ShutterConstants.SETTINGS_KEY} 0")
        add("settings put global csc_pref_camera_forced_shuttersound_key 0")
        add("settings put secure sound_effects_enabled 0")
        add("settings put global camera_sound 0")
        add("settings put global camera_shutter_sound 0")
        for (stream in muteStreams) {
            add(AudioShellCommands.setStreamVolumeLegacy(stream, 0))
            add(AudioShellCommands.setStreamVolume(stream, 0))
        }
    }

    companion object {
        /** 셔터음 관련 스트림만. 알림/벨/미디어는 절대 포함하지 말 것(클래스 주석 참고) */
        private val muteStreams = listOf(
            ShutterConstants.STREAM_SYSTEM,
            ShutterConstants.STREAM_SYSTEM_ENFORCED,
        )
    }
}
