package com.example.todoapp.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.todoapp.data.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Планировщик уведомлений на основе AlarmManager.
 *
 * Схема работы:
 *   1. scheduleDeadlineNotification() → AlarmManager.setExactAndAllowWhileIdle()
 *   2. По срабатыванию → AlarmReceiver.onReceive()
 *   3. AlarmReceiver → WorkManager.enqueue(DeadlineNotificationWorker)
 *   4. Worker проверяет актуальность задачи и показывает уведомление
 *
 * Почему не только WorkManager?
 *   WorkManager не гарантирует точность — он выполняет задачи с возможной задержкой
 *   из-за батарейной оптимизации. AlarmManager с setExactAndAllowWhileIdle()
 *   срабатывает даже в Doze-режиме (Android 6+).
 *
 * Android 12+ (API 31+):
 *   Требует разрешение SCHEDULE_EXACT_ALARM, которое пользователь должен выдать
 *   вручную через Settings → Apps → Special app access → Alarms & reminders.
 *   Проверяем canScheduleExactAlarms() и предлагаем fallback.
 */
@Singleton
class AlarmManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) : NotificationScheduler {

    companion object {
        private const val TAG = "AlarmScheduler"
        // За сколько миллисекунд до дедлайна показывать уведомление (2 часа)
        private const val NOTIFY_BEFORE_MS = 2 * 60 * 60 * 1000L
    }

    override fun scheduleDeadlineNotification(task: Task) {
        val deadline = task.deadlineMillis ?: return
        val notifyAt = deadline - NOTIFY_BEFORE_MS

        // Не планируем уведомление в прошлом
        if (notifyAt <= System.currentTimeMillis()) {
            Log.d(TAG, "Skipping notification for '${task.title}' — time already passed")
            return
        }

        val pendingIntent = buildPendingIntent(task) ?: return

        scheduleExactAlarm(notifyAt, pendingIntent, task.title)
        Log.d(TAG, "Scheduled notification for '${task.title}' at $notifyAt")
    }

    override fun cancelNotification(taskId: String) {
        // Создаём идентичный Intent для поиска и отмены существующего PendingIntent
        val intent = AlarmReceiver.createIntent(context, taskId, "")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            // FLAG_NO_CREATE — возвращает null если PendingIntent не существует
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "Cancelled notification for taskId=$taskId")
        }
    }

    private fun buildPendingIntent(task: Task): PendingIntent? {
        val intent = AlarmReceiver.createIntent(context, task.id, task.title)
        return PendingIntent.getBroadcast(
            context,
            // requestCode уникален для каждой задачи через hashCode
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleExactAlarm(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        taskTitle: String
    ) {
        when {
            // Android 12+ (API 31+): проверяем разрешение на точные будильники
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    // Точный будильник, работает в Doze-режиме
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback: неточный, но всё равно сработает
                    // Для получения точного разрешения нужно направить пользователя в Settings:
                    // Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    Log.w(TAG, "Exact alarm permission not granted for '$taskTitle'. Using inexact alarm.")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
            // Android 6–11: setExactAndAllowWhileIdle() доступен без доп. разрешений
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            // Android < 6: обычный точный будильник
            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }
}
