package com.example.todoapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.todoapp.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver для восстановления будильников после перезагрузки.
 *
 * Проблема: AlarmManager теряет все запланированные будильники при выключении устройства.
 * Решение: слушаем ACTION_BOOT_COMPLETED и ACTION_MY_PACKAGE_REPLACED,
 * получаем все задачи с будущими дедлайнами из Room и перепланируем их.
 *
 * @AndroidEntryPoint — позволяет Hilt инжектировать зависимости в BroadcastReceiver.
 *
 * ВАЖНО: goAsync() / CoroutineScope обязателен, так как Room-запрос асинхронный,
 * а onReceive() имеет жёсткий timeout (~10 секунд).
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var scheduler: NotificationScheduler

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.d(TAG, "Device rebooted or app updated — rescheduling alarms")

        // goAsync() продлевает жизнь BroadcastReceiver-а для асинхронной работы.
        // SupervisorJob гарантирует, что одна упавшая задача не отменяет остальные.
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val tasks = repository.getTasksWithFutureDeadlines()
                Log.d(TAG, "Rescheduling ${tasks.size} notifications")

                tasks.forEach { task ->
                    scheduler.scheduleDeadlineNotification(task)
                }

                Log.d(TAG, "All alarms rescheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms", e)
            } finally {
                // Обязательно вызываем finish() чтобы освободить ресурсы
                pendingResult.finish()
            }
        }
    }
}
