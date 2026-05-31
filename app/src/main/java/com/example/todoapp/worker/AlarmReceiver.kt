package com.example.todoapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * BroadcastReceiver, срабатывающий при наступлении запланированного будильника.
 *
 * Намеренно НЕ показывает уведомление напрямую — вместо этого он ставит в очередь
 * DeadlineNotificationWorker через WorkManager. Это даёт несколько преимуществ:
 *  - Worker получает Hilt-зависимости (Repository) и может проверить актуальность задачи
 *  - WorkManager гарантирует выполнение даже если Receiver завершился быстро
 *  - Лёгкость тестирования Worker-а в изоляции
 *
 * onReceive() выполняется на главном потоке, поэтому только ставим задачу в очередь.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val EXTRA_TASK_ID    = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"

        /** Фабричный метод для создания Intent с нужными extras. */
        fun createIntent(
            context: Context,
            taskId: String,
            taskTitle: String
        ): Intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId    = intent.getStringExtra(EXTRA_TASK_ID)    ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: ""

        Log.d(TAG, "Alarm fired for taskId=$taskId, title='$taskTitle'")

        // Передаём данные в Worker через Data
        val inputData = workDataOf(
            DeadlineNotificationWorker.KEY_TASK_ID    to taskId,
            DeadlineNotificationWorker.KEY_TASK_TITLE to taskTitle
        )

        // EXPEDITED — запускается с максимальным приоритетом, не откладывается батарейной оптимизацией
        val workRequest = OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // работает без сети
                    .build()
            )
            // Уникальное имя предотвращает дублирование уведомления
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "notification_$taskId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }
}
