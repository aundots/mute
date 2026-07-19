package com.mute.shutter.camera

import android.content.Context
import android.media.AudioManager
import com.mute.shutter.debug.DebugLogger

class DirectAudioMuter(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val savedVolumes = mutableMapOf<Int, Int>()
    private var savedRingerMode: Int? = null

    fun muteNow() {
        DebugLogger.log("▶ DirectAudioMuter.muteNow() 호출")

        if (audioManager == null) {
            DebugLogger.logError("AudioManager를 얻을 수 없음")
            return
        }

        try {
            savedRingerMode = audioManager.ringerMode
            DebugLogger.logInfo("현재 벨소리 모드", ringerModeToString(savedRingerMode!!))

            for (stream in MUTE_STREAMS) {
                try {
                    val currentVolume = audioManager.getStreamVolume(stream)
                    val streamName = streamToString(stream)
                    savedVolumes[stream] = currentVolume
                    DebugLogger.logInfo("스트림 $streamName", "현재음량=$currentVolume")

                    audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)

                    val afterVolume = audioManager.getStreamVolume(stream)
                    if (afterVolume == 0) {
                        DebugLogger.logSuccess("스트림 $streamName 음소거 됨 (0)")
                    } else {
                        DebugLogger.logError("스트림 $streamName 음소거 실패 (현재: $afterVolume)")
                    }
                } catch (e: Exception) {
                    DebugLogger.logError("스트림 $stream 음소거 실패", e)
                }
            }

            if (!DndAccessHelper.hasDndAccess(context)) {
                DebugLogger.logError("방해 금지 모드 접근 권한 없음 — 벨소리 모드 변경 건너뜀 (설정에서 권한 허용 필요)")
            } else {
                try {
                    val oldMode = audioManager.ringerMode
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    val newMode = audioManager.ringerMode
                    if (newMode == AudioManager.RINGER_MODE_SILENT) {
                        DebugLogger.logSuccess("벨소리 모드 → SILENT 설정됨")
                    } else {
                        DebugLogger.logError("벨소리 모드 설정 실패 (이전: ${ringerModeToString(oldMode)}, 현재: ${ringerModeToString(newMode)})")
                    }
                } catch (e: Exception) {
                    DebugLogger.logError("벨소리 모드 설정 실패", e)
                }
            }

            DebugLogger.logSuccess("DirectAudioMuter 음소거 완료")
        } catch (e: Exception) {
            DebugLogger.logError("DirectAudioMuter 음소거 중 예외 발생", e)
        }
    }

    fun restoreNow() {
        DebugLogger.log("▶ DirectAudioMuter.restoreNow() 호출")

        if (audioManager == null) {
            DebugLogger.logError("AudioManager를 얻을 수 없음")
            return
        }

        try {
            for ((stream, level) in savedVolumes) {
                try {
                    val streamName = streamToString(stream)
                    audioManager.setStreamVolume(stream, level, AudioManager.FLAG_SHOW_UI)

                    val afterVolume = audioManager.getStreamVolume(stream)
                    if (afterVolume == level) {
                        DebugLogger.logSuccess("스트림 $streamName 복구 ($level)")
                    } else {
                        DebugLogger.logError("스트림 $streamName 복구 실패 (예상: $level, 실제: $afterVolume)")
                    }
                } catch (e: Exception) {
                    DebugLogger.logError("스트림 $stream 복구 실패", e)
                }
            }
            savedVolumes.clear()

            if (!DndAccessHelper.hasDndAccess(context)) {
                DebugLogger.logError("방해 금지 모드 접근 권한 없음 — 벨소리 모드 복구 건너뜀")
            } else {
                try {
                    when (savedRingerMode) {
                        AudioManager.RINGER_MODE_SILENT -> {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                            DebugLogger.logSuccess("벨소리 모드 → SILENT 복구")
                        }
                        AudioManager.RINGER_MODE_VIBRATE -> {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                            DebugLogger.logSuccess("벨소리 모드 → VIBRATE 복구")
                        }
                        else -> {
                            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                            DebugLogger.logSuccess("벨소리 모드 → NORMAL 복구")
                        }
                    }
                } catch (e: Exception) {
                    DebugLogger.logError("벨소리 모드 복구 실패", e)
                }
            }
            savedRingerMode = null

            DebugLogger.logSuccess("DirectAudioMuter 복구 완료")
        } catch (e: Exception) {
            DebugLogger.logError("DirectAudioMuter 복구 중 예외 발생", e)
        }
    }

    private fun streamToString(stream: Int): String = when (stream) {
        AudioManager.STREAM_SYSTEM -> "SYSTEM"
        AudioManager.STREAM_NOTIFICATION -> "NOTIFICATION"
        AudioManager.STREAM_RING -> "RING"
        AudioManager.STREAM_MUSIC -> "MUSIC"
        AudioManager.STREAM_ALARM -> "ALARM"
        else -> "UNKNOWN($stream)"
    }

    private fun ringerModeToString(mode: Int): String = when (mode) {
        AudioManager.RINGER_MODE_SILENT -> "SILENT"
        AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
        AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
        else -> "UNKNOWN($mode)"
    }

    companion object {
        private val MUTE_STREAMS = listOf(
            AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_ALARM,
        )
    }
}
