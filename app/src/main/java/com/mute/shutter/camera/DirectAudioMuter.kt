package com.mute.shutter.camera

import android.content.Context
import android.media.AudioManager
import com.mute.shutter.ShutterConstants
import com.mute.shutter.debug.DebugLogger

/**
 * 카메라 사용 중 셔터음 관련 스트림만 API로 직접 음소거한다.
 *
 * 절대 건드리면 안 되는 것들(과거 버그의 원인):
 * - 벨소리 모드: SILENT로 바꿨다 복구가 실패하면 사용자의 무음/진동 설정이 풀린다.
 * - RING/NOTIFICATION 스트림: 진동·무음 모드에서는 읽으면 0이 나오므로
 *   "저장→복구"가 사용자의 원래 음량을 0으로 덮어써 알림이 영구 무음이 된다.
 * - MUSIC/ALARM 스트림: 셔터음과 무관하고, 배경 음악을 끊거나 알람을 놓치게 한다.
 *
 * 셔터음은 설정 키(csc_pref_...)가 실제로 잠재우며(S26U에서 확인),
 * 여기서는 SYSTEM/SYSTEM_ENFORCED 스트림만 보조로 0으로 내린다.
 */
class DirectAudioMuter(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val savedVolumes = mutableMapOf<Int, Int>()

    fun muteNow() {
        DebugLogger.log("▶ DirectAudioMuter.muteNow() 호출")

        if (audioManager == null) {
            DebugLogger.logError("AudioManager를 얻을 수 없음")
            return
        }

        for (stream in MUTE_STREAMS) {
            try {
                val currentVolume = audioManager.getStreamVolume(stream)
                val streamName = streamToString(stream)
                savedVolumes[stream] = currentVolume
                DebugLogger.logInfo("스트림 $streamName", "현재음량=$currentVolume")

                audioManager.setStreamVolume(stream, 0, NO_UI_NO_SOUND)

                val afterVolume = audioManager.getStreamVolume(stream)
                if (afterVolume == 0) {
                    DebugLogger.logSuccess("스트림 $streamName 음소거 됨 (0)")
                } else {
                    DebugLogger.logError("스트림 $streamName 음소거 실패 (현재: $afterVolume)")
                }
            } catch (e: Exception) {
                // SYSTEM_ENFORCED(7)는 기기에 따라 조작이 거부될 수 있다 — 무시하고 계속.
                savedVolumes.remove(stream)
                DebugLogger.logError("스트림 $stream 음소거 실패", e)
            }
        }

        DebugLogger.logSuccess("DirectAudioMuter 음소거 완료")
    }

    fun restoreNow() {
        DebugLogger.log("▶ DirectAudioMuter.restoreNow() 호출")

        if (audioManager == null) {
            DebugLogger.logError("AudioManager를 얻을 수 없음")
            return
        }

        // 음소거한 적이 없으면 아무것도 건드리지 않는다.
        // (영상통화·QR 스캔 등으로 muteNow()를 건너뛴 경우에도 카메라가 닫히면 여기로 온다)
        if (savedVolumes.isEmpty()) {
            DebugLogger.log("복구할 상태 없음 — 건너뜀")
            return
        }

        for ((stream, level) in savedVolumes) {
            try {
                val streamName = streamToString(stream)
                audioManager.setStreamVolume(stream, level, NO_UI_NO_SOUND)

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

        DebugLogger.logSuccess("DirectAudioMuter 복구 완료")
    }

    private fun streamToString(stream: Int): String = when (stream) {
        AudioManager.STREAM_SYSTEM -> "SYSTEM"
        ShutterConstants.STREAM_SYSTEM_ENFORCED -> "SYSTEM_ENFORCED"
        else -> "UNKNOWN($stream)"
    }

    companion object {
        /**
         * FLAG_SHOW_UI를 주면 시스템 볼륨 패널이 뜨고, 스트림에 따라 새 음량을
         * 들려주는 미리듣기 소리까지 재생한다. 백그라운드 음소거에는 둘 다 방해만 되므로
         * 플래그 없이 조용히 바꾼다.
         */
        private const val NO_UI_NO_SOUND = 0

        /** 셔터음 관련 스트림만. 알림/벨/미디어/알람은 절대 포함하지 말 것(클래스 주석 참고) */
        private val MUTE_STREAMS = listOf(
            AudioManager.STREAM_SYSTEM,
            ShutterConstants.STREAM_SYSTEM_ENFORCED,
        )
    }
}
