package com.mute.shutter.camera

import android.content.Context
import android.media.AudioManager
import android.util.Log

class DirectAudioMuter(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val savedVolumes = mutableMapOf<Int, Int>()
    private var savedRingerMode: Int? = null

    fun muteNow() {
        if (audioManager == null) return

        try {
            savedRingerMode = audioManager.ringerMode

            for (stream in MUTE_STREAMS) {
                try {
                    savedVolumes[stream] = audioManager.getStreamVolume(stream)
                    audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to mute stream $stream: ${e.message}")
                }
            }

            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set ringer mode: ${e.message}")
            }

            Log.d(TAG, "Audio muted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error muting audio: ${e.message}")
        }
    }

    fun restoreNow() {
        if (audioManager == null) return

        try {
            for ((stream, level) in savedVolumes) {
                try {
                    audioManager.setStreamVolume(stream, level, AudioManager.FLAG_SHOW_UI)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore stream $stream: ${e.message}")
                }
            }
            savedVolumes.clear()

            when (savedRingerMode) {
                AudioManager.RINGER_MODE_SILENT -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                AudioManager.RINGER_MODE_VIBRATE -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                else -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            savedRingerMode = null

            Log.d(TAG, "Audio restored successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring audio: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "DirectAudioMuter"
        private val MUTE_STREAMS = listOf(
            AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_ALARM,
        )
    }
}
