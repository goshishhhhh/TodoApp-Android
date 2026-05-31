package com.example.todoapp.data.remote

import com.example.todoapp.data.model.Subtask
import com.example.todoapp.data.model.SyncStatus
import com.example.todoapp.data.model.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис для работы с Firebase Firestore.
 *
 * Структура коллекций:
 *   users/{userId}/tasks/{taskId}            — задачи
 *   users/{userId}/tasks/{taskId}/subtasks/{subtaskId} — подзадачи (subcollection)
 *
 * Использование userId позволяет поддержать мультипользовательский режим
 * и безопасность через Firestore Rules.
 */
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_TASKS = "tasks"
        private const val COLLECTION_SUBTASKS = "subtasks"

        // Анонимный userId для случая без авторизации
        private const val ANONYMOUS_USER_ID = "anonymous"
    }

    // Текущий пользователь (можно заменить на Firebase Auth UID)
    private val userId: String
        get() = com.google.firebase.auth.FirebaseAuth.getInstance()
            .currentUser?.uid ?: ANONYMOUS_USER_ID

    private fun tasksCollection() = firestore
        .collection(COLLECTION_USERS)
        .document(userId)
        .collection(COLLECTION_TASKS)

    private fun subtasksCollection(taskId: String) = tasksCollection()
        .document(taskId)
        .collection(COLLECTION_SUBTASKS)

    // ── Real-time наблюдение за всеми задачами ─────────────────

    /**
     * callbackFlow оборачивает Firestore Listener в холодный Flow.
     * awaitClose гарантирует отписку от Listener при отмене корутины.
     */
    fun observeTasks(): Flow<List<Task>> = callbackFlow {
        val listener = tasksCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toTask()
                } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    // ── Одиночные операции CRUD ────────────────────────────────

    suspend fun saveTask(task: Task) {
        tasksCollection()
            .document(task.id)
            .set(task.toFirestoreMap(), SetOptions.merge())
            .await()

        // Сохраняем подзадачи
        task.subtasks.forEach { subtask ->
            saveSubtask(task.id, subtask)
        }
    }

    suspend fun deleteTask(taskId: String) {
        // Сначала удаляем все подзадачи (Firestore не каскадирует удаления)
        val subtaskDocs = subtasksCollection(taskId).get().await()
        subtaskDocs.documents.forEach { it.reference.delete().await() }

        // Затем удаляем саму задачу
        tasksCollection().document(taskId).delete().await()
    }

    suspend fun saveSubtask(taskId: String, subtask: Subtask) {
        subtasksCollection(taskId)
            .document(subtask.id)
            .set(subtask.toFirestoreMap(), SetOptions.merge())
            .await()
    }

    suspend fun deleteSubtask(taskId: String, subtaskId: String) {
        subtasksCollection(taskId).document(subtaskId).delete().await()
    }

    /** Загрузка всех задач (одиночный запрос, не real-time). */
    suspend fun fetchAllTasks(): List<Task> {
        val taskDocs = tasksCollection().get().await()
        return taskDocs.documents.mapNotNull { doc ->
            val task = doc.toTask() ?: return@mapNotNull null
            val subtaskDocs = subtasksCollection(task.id).get().await()
            val subtasks = subtaskDocs.documents.mapNotNull { it.toSubtask(task.id) }
            task.copy(subtasks = subtasks)
        }
    }

    // ── Маппинг Task ↔ Firestore Map ──────────────────────────

    private fun Task.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "isCompleted" to isCompleted,
        "isImportant" to isImportant,
        "deadlineMillis" to deadlineMillis,
        "createdAtMillis" to createdAtMillis
        // syncStatus не сохраняем в Firestore — это локальный статус
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toTask(): Task? {
        return try {
            Task(
                id = getString("id") ?: id,
                title = getString("title") ?: "",
                description = getString("description") ?: "",
                isCompleted = getBoolean("isCompleted") ?: false,
                isImportant = getBoolean("isImportant") ?: false,
                deadlineMillis = getLong("deadlineMillis"),
                createdAtMillis = getLong("createdAtMillis") ?: System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun Subtask.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "taskId" to taskId,
        "title" to title,
        "isCompleted" to isCompleted
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toSubtask(taskId: String): Subtask? {
        return try {
            Subtask(
                id = getString("id") ?: id,
                taskId = taskId,
                title = getString("title") ?: "",
                isCompleted = getBoolean("isCompleted") ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
}
