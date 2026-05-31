package com.example.todoapp.worker

import com.example.todoapp.data.model.Task
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фасад для управления уведомлениями о дедлайнах.
 * Полная реализация — в AlarmManagerScheduler (Шаг 4).
 * Этот интерфейс позволяет ViewModel не зависеть от деталей реализации.
 */
interface NotificationScheduler {
    fun scheduleDeadlineNotification(task: Task)
    fun cancelNotification(taskId: String)
}

/**
 * Stub-реализация для компиляции на Шаге 3.
 * Заменяется на AlarmManagerScheduler при регистрации в Hilt (Шаг 4).
 */
@Singleton
class StubNotificationScheduler @Inject constructor() : NotificationScheduler {
    override fun scheduleDeadlineNotification(task: Task) { /* реализация в Шаге 4 */ }
    override fun cancelNotification(taskId: String) { /* реализация в Шаге 4 */ }
}
