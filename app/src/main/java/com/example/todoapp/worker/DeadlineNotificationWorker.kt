package com.example.todoapp.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.todoapp.MainActivity
import com.example.todoapp.R
import com.example.todoapp.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * HiltWorker для отображения уведомления о приближающемся дедлайне.
 *
 * @HiltWorker + @AssistedInject — стандартная связка для инжекции в WorkManager.
 * HiltWorkerFactory (зарегистрированная в TodoApplication) создаёт этот Worker
 * с правильными зависимостями.
 *
 * Перед показом уведомления Worker:
 *  1. Загружает задачу из Room (актуальные данные)
 *  2. Проверяет, что задача не завершена
 *  3. Проверяет, что дедлайн ещё не наступил (допуск: ±10 минут)
 * Это предотвращает «фантомные» уведомления для уже выполненных задач.
 */
@HiltWorker
class DeadlineNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TaskRepository,
    private val notificationManager: NotificationManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
        const val KEY_TASK_ID    = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        private const val CHANNEL_ID = "deadline_notifications"
        // Допуск: если уведомление задержалось до 10 минут — всё равно показываем
        private const val DEADLINE_TOLERANCE_MS = 10 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        val taskId    = inputData.getString(KEY_TASK_ID)    ?: return Result.failure()
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: ""

        Log.d(TAG, "Processing notification for taskId=$taskId")

        return try {
            val task = repository.getTaskById(taskId)

            when {
                task == null -> {
                    // Задача удалена — уведомление не нужно
                    Log.d(TAG, "Task $taskId not found, skipping notification")
                    Result.success()
                }
                task.isCompleted -> {
                    // Задача выполнена — уведомление не актуально
                    Log.d(TAG, "Task '${task.title}' already completed, skipping")
                    Result.success()
                }
                task.deadlineMillis != null &&
                        task.deadlineMillis < System.currentTimeMillis() - DEADLINE_TOLERANCE_MS -> {
                    // Дедлайн уже давно прошёл (Worker сильно задержался) — пропускаем
                    Log.d(TAG, "Task '${task.title}' deadline too far in the past, skipping")
                    Result.success()
                }
                else -> {
                    showNotification(taskId, task.title)
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in notification worker", e)
            // Retry — WorkManager повторит попытку
            Result.retry()
        }
    }

    private fun showNotification(taskId: String, title: String) {
        // PendingIntent открывает MainActivity при нажатии на уведомление
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", taskId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Дедлайн через 2 часа")
            .setContentText(title)
            // BigTextStyle показывает полный заголовок при развороте уведомления
            .setStyle(NotificationCompat.BigTextStyle().bigText("Задача «$title» должна быть выполнена через 2 часа"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        // notificationId уникален для каждой задачи — обновляем, а не дублируем
        notificationManager.notify(taskId.hashCode(), notification)
        Log.d(TAG, "Notification shown for '$title'")
    }

    /**
     * Expedited Worker обязан реализовывать getForegroundInfo().
     * Используется на Android < 12, где expedited работает через foreground service.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Проверка дедлайнов...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return ForegroundInfo(0, notification)
    }
}
