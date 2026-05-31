package com.example.todoapp.data.model

/**
 * Domain-модель задачи.
 * Это «чистый» объект — не зависит ни от Room, ни от Firestore.
 * ViewModel и UseCase работают только с этим классом.
 */
data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val isImportant: Boolean = false,           // флаг «важная задача»
    val deadlineMillis: Long? = null,           // дедлайн в миллисекундах UTC, null = не задан
    val createdAtMillis: Long = System.currentTimeMillis(),
    val subtasks: List<Subtask> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

/**
 * Domain-модель подзадачи.
 */
data class Subtask(
    val id: String = "",
    val taskId: String = "",
    val title: String = "",
    val isCompleted: Boolean = false
)

/**
 * Статус синхронизации с Firestore.
 * Используется для offline-first логики:
 * - PENDING_UPLOAD   → запись в Room, но ещё не отправлена в Firestore
 * - PENDING_DELETE   → помечена к удалению в Firestore
 * - SYNCED           → актуальные данные есть и в Room, и в Firestore
 */
enum class SyncStatus {
    PENDING_UPLOAD,
    PENDING_DELETE,
    SYNCED
}
