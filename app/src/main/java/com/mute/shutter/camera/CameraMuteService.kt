package com.mute.shutter.camera

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mute.shutter.MuteApplication
import com.mute.shutter.R
import com.mute.shutter.debug.DebugLogger
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
    private var directMuter: DirectAudioMuter? = null

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            DebugLogger.log("📷 카메라 사용 중: $cameraId")
            val wasEmpty = synchronized(activeCameraIds) {
                val empty = activeCameraIds.isEmpty()
                activeCameraIds.add(cameraId)
                empty
            }
            if (wasEmpty) {
                DebugLogger.log("→ 첫 번째 카메라 감지, 음소거 시작")
                muteNow()
            } else {
                DebugLogger.log("→ 추가 카메라 감지, 이미 음소거됨")
            }
        }

        override fun onCameraAvailable(cameraId: String) {
            DebugLogger.log("📷 카메라 종료: $cameraId")
            val nowEmpty = synchronized(activeCameraIds) {
                activeCameraIds.remove(cameraId)
                activeCameraIds.isEmpty()
            }
            if (nowEmpty) {
                DebugLogger.log("→ 모든 카메라 종료, 음량 복구 시작")
                restoreNow()
            } else {
                DebugLogger.log("→ 다른 카메라 실행 중, 음소거 유지")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(this)
        DebugLogger.logSuccess("CameraMuteService 생성됨")
        createChannel()
        controller = CameraMuteController((application as MuteApplication).adb)
        directMuter = DirectAudioMuter(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // FOREGROUND_SERVICE_TYPE_SPECIAL_USE는 API 34에 도입됐다.
        // minSdk 30을 위해 API 34 미만에서는 타입 없는 startForeground를 쓴다(30~33에서 유효).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildSilentNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildSilentNotification())
        }

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
        val muter = directMuter ?: return
        scope.launch {
            muteLock.withLock {
                DebugLogger.log("🔒 뮤트락 획득, 카메라 앱 확인 중...")
                val isCamera = ForegroundAppReader.isCameraForeground(app.adb)
                    ?: CameraForegroundDetector(this@CameraMuteService).getForegroundCameraPackage() != null
                DebugLogger.logInfo("카메라 앱 확인", if (isCamera) "YES" else "NO")

                if (!isCamera) {
                    DebugLogger.log("⚠️ 카메라 앱이 포그라운드에 없음, 작업 취소")
                    synchronized(activeCameraIds) { activeCameraIds.clear() }
                    return@withLock
                }

                DebugLogger.log("▶ DirectAudioMuter 실행")
                muter.muteNow()

                DebugLogger.log("▶ ADB 음소거 명령어 실행")
                ctrl.muteForCamera()
                DebugLogger.logSuccess("음소거 작업 완료")
            }
        }
    }

    private fun restoreNow() {
        val ctrl = controller ?: return
        val muter = directMuter ?: return
        scope.launch {
            muteLock.withLock {
                DebugLogger.log("🔒 복구락 획득")
                DebugLogger.log("▶ ADB 복구 명령어 실행")
                ctrl.restoreAfterCamera()

                DebugLogger.log("▶ DirectAudioMuter 복구 실행")
                muter.restoreNow()
                DebugLogger.logSuccess("복구 작업 완료")
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
