package com.mute.shutter.camera

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mute.shutter.MuteApplication
import com.mute.shutter.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 카메라 열림/닫힘 이벤트에만 반응(폴링 없음)해 배터리 사용을 최소화한다.
 * ADB는 카메라가 켜질 때만 실행된다.
 */
class CameraMuteService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val muteLock = Mutex()

    /** 카메라 앱이 연 카메라 ID들. 비면 촬영 종료로 간주해 볼륨 복구 */
    private val activeCameraIds = mutableSetOf<String>()

    private var cameraManager: CameraManager? = null
    private var callbackThread: HandlerThread? = null
    private var controller: CameraMuteController? = null

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            val wasEmpty = synchronized(activeCameraIds) {
                val empty = activeCameraIds.isEmpty()
                activeCameraIds.add(cameraId)
                empty
            }
            if (wasEmpty) muteNow()
        }

        override fun onCameraAvailable(cameraId: String) {
            val nowEmpty = synchronized(activeCameraIds) {
                activeCameraIds.remove(cameraId)
                activeCameraIds.isEmpty()
            }
            if (nowEmpty) restoreNow()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        controller = CameraMuteController((application as MuteApplication).adb)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            buildSilentNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )

        (application as MuteApplication).preferences.watcherRunning = true
        registerCameraCallback()
        return START_STICKY
    }

    override fun onDestroy() {
        cameraManager?.unregisterAvailabilityCallback(availabilityCallback)
        callbackThread?.quitSafely()
        callbackThread = null
        scope.cancel()
        (application as? MuteApplication)?.preferences?.watcherRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerCameraCallback() {
        if (callbackThread != null) return
        val thread = HandlerThread("camera-availability").also { it.start() }
        callbackThread = thread
        val manager = getSystemService(CameraManager::class.java) ?: return
        cameraManager = manager
        manager.registerAvailabilityCallback(availabilityCallback, Handler(thread.looper))
    }

    private fun muteNow() {
        val app = application as MuteApplication
        val ctrl = controller ?: return
        scope.launch {
            muteLock.withLock {
                app.adb.testConnection()
                val isCamera = ForegroundAppReader.isCameraForeground(app.adb)
                    ?: CameraForegroundDetector(this@CameraMuteService).getForegroundCameraPackage() != null
                if (!isCamera) {
                    synchronized(activeCameraIds) { activeCameraIds.clear() }
                    return@withLock
                }
                ctrl.muteForCamera()
            }
        }
    }

    private fun restoreNow() {
        val ctrl = controller ?: return
        scope.launch {
            muteLock.withLock {
                ctrl.restoreAfterCamera()
            }
        }
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL_ID, "카메라 무음", NotificationManager.IMPORTANCE_MIN)
        channel.setShowBadge(false)
        channel.enableLights(false)
        channel.enableVibration(false)
        channel.setSound(null, null)
        nm.createNotificationChannel(channel)
    }

    private fun buildSilentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "camera_mute_v3"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CameraMuteService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CameraMuteService::class.java))
        }
    }
}
