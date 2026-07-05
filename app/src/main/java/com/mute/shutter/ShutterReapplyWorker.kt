package com.mute.shutter

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mute.shutter.adb.AdbResult
import com.mute.shutter.camera.CameraMuteService
import java.util.concurrent.TimeUnit

class ShutterReapplyWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MuteApplication ?: return Result.failure()
        if (!app.preferences.isPaired) return Result.success()

        when (val connection = app.adb.testConnection()) {
            is AdbResult.Failure -> {
                return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
            is AdbResult.Success -> Unit
        }

        return when (app.shutter.mute()) {
            is AdbResult.Failure -> {
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
            is AdbResult.Success -> {
                CameraMuteService.start(applicationContext)
                Result.success()
            }
        }
    }

    companion object {
        private const val WORK_NAME = "shutter_reapply"
        private const val MAX_RETRIES = 5

        fun enqueue(context: Context, immediate: Boolean = false) {
            val builder = OneTimeWorkRequestBuilder<ShutterReapplyWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            if (!immediate) {
                builder.setInitialDelay(10, TimeUnit.SECONDS)
            }
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                builder.build(),
            )
        }
    }
}
