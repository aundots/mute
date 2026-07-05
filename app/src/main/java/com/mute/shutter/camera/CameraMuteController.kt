package com.mute.shutter.camera

import com.mute.shutter.ShutterConstants
import com.mute.shutter.adb.AdbResult
import com.mute.shutter.adb.AdbSessionManager

/** 벨소리 무음 + 시스템/강제/알림/미디어 스트림 0. 읽기 실패해도 무음은 무조건 적용 */
class CameraMuteController(private val adb: AdbSessionManager) {
    private var savedRingerMode: Int? = null
    private val savedVolumes = mutableMapOf<Int, Int>()
    private var muted = false
    var lastError: String? = null
        private set

    suspend fun muteForCamera(): AdbResult<Unit> {
        if (savedRingerMode == null) {
            when (val ringer = adb.shell(AudioShellCommands.getRingerMode())) {
                is AdbResult.Success -> AudioShellCommands.parseRingerMode(ringer.value)?.let { savedRingerMode = it }
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
                is AdbResult.Failure -> anyFailure = result.message
                is AdbResult.Success -> Unit
            }
        }

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

        when (savedRingerMode) {
            0 -> adb.shell(AudioShellCommands.setRingerModeSilent())
            1 -> adb.shell(AudioShellCommands.setRingerModeVibrate())
            else -> adb.shell(AudioShellCommands.setRingerModeNormal())
        }
        savedRingerMode = null
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

    private fun muteCommands(): List<String> = buildList {
        add(AudioShellCommands.setRingerModeSilent())
        for (stream in muteStreams) {
            add(AudioShellCommands.setStreamVolumeLegacy(stream, 0))
            add(AudioShellCommands.setStreamVolume(stream, 0))
        }
    }

    companion object {
        private val muteStreams = listOf(
            ShutterConstants.STREAM_SYSTEM,
            ShutterConstants.STREAM_SYSTEM_ENFORCED,
            ShutterConstants.STREAM_NOTIFICATION,
            ShutterConstants.STREAM_RING,
            ShutterConstants.STREAM_MUSIC,
        )
    }
}
