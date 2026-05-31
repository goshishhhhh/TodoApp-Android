package com.example.todoapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.todoapp.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Периодическая синхронизация накопленных PENDING_UPLOAD / PENDING_DELETE записей.
 *
 * Запускается автоматически при наличии сети (NetworkType.CONNECTED).
 * Планируется один раз при старте приложения через schedulePeriodicSync().
 * ExistingPeriodicWorkPolicy.KEEP предотвращает дублирование задач.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "periodic_sync"

        /**
         * Планирует периодическую синхронизацию раз в 15 минут (минимум WorkManager).
         * Вызывается из TodoApplication или MainActivity один раз.
         */
        fun schedulePeriodicSync(context: Context) {
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval      = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        // Запускаем только при наличии сети
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                // Retry при сбое: экспоненциальная задержка, старт через 30 сек
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // KEEP: если задача уже запланирована — не пересоздаём
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "Periodic sync scheduled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync of pending tasks")
        return try {
            repository.syncPendingTasks()
            Log.d(TAG, "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }
}
