package com.mute.shutter.camera

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mute.shutter.MuteApplication
import com.mute.shutter.R
import com.mute.shutter.adb.AdbResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CameraMuteService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("카메라 감시 중")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val app = application as MuteApplication
        app.preferences.watcherRunning = true
        startWatching(app)
        return START_STICKY
    }

    override fun onDestroy() {
        watchJob?.cancel()
        scope.cancel()
        (application as? MuteApplication)?.preferences?.watcherRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startWatching(app: MuteApplication) {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch {
            val usageDetector = CameraForegroundDetector(this@CameraMuteService)
            val controller = CameraMuteController(app.adb)
            var cameraActive = false

            while (isActive) {
                val connected = app.adb.testConnection() is AdbResult.Success
                if (!connected) {
                    updateNotification("ADB 연결 대기 중 — 무선 디버깅 확인")
                    delay(POLL_MS)
                    continue
                }

                // ADB dumpsys 우선, 실패 시 사용 통계
                val inCamera = ForegroundAppReader.isCameraForeground(app.adb)
                    ?: (usageDetector.getForegroundCameraPackage() != null)

                when {
                    inCamera -> {
                        val result = controller.muteForCamera()
                        cameraActive = true
                        val text = when (result) {
                            is AdbResult.Success -> "카메라 무음 적용 중"
                            is AdbResult.Failure -> "무음 시도 중 (${controller.lastError ?: "명령 실패"})"
                        }
                        updateNotification(text)
                        delay(CAMERA_POLL_MS)
                    }
                    !inCamera && cameraActive -> {
                        controller.restoreAfterCamera()
                        cameraActive = false
                        updateNotification("카메라 감시 중")
                        delay(POLL_MS)
                    }
                    else -> delay(POLL_MS)
                }
            }
        }
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "카메라 무음", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "camera_mute"
        private const val NOTIFICATION_ID = 1
        private const val POLL_MS = 800L
        private const val CAMERA_POLL_MS = 250L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CameraMuteService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CameraMuteService::class.java))
        }
    }
}
