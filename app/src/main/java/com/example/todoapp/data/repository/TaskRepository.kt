package com.example.todoapp.data.repository

import com.example.todoapp.data.local.dao.TaskDao
import com.example.todoapp.data.local.entity.SubtaskEntity
import com.example.todoapp.data.local.entity.TaskEntity
import com.example.todoapp.data.model.Subtask
import com.example.todoapp.data.model.SyncStatus
import com.example.todoapp.data.model.Task
import com.example.todoapp.data.remote.FirestoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TaskRepository — единственная точка доступа к данным для всего приложения.
 *
 * Offline-first стратегия:
 * 1. Все операции СНАЧАЛА пишут в Room (мгновенный отклик в UI).
 * 2. Затем асинхронно синхронизируют с Firestore.
 * 3. При отсутствии сети данные помечаются PENDING_UPLOAD / PENDING_DELETE.
 * 4. syncPendingTasks() вызывается при восстановлении сети.
 *
 * SupervisorJob гарантирует, что падение одной синхронизации
 * не отменяет другие операции в том же scope.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val firestoreService: FirestoreService
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Наблюдение (Flow) ──────────────────────────────────────

    /**
     * Основной поток задач — собирает задачи + подзадачи из Room.
     * combine позволяет объединить два Flow в один.
     */
    fun observeAllTasks(): Flow<List<Task>> {
        return taskDao.observeAllTasks().map { entities ->
            entities.map { entity ->
                val subtasks = taskDao.getSubtasksForTask(entity.id)
                    .map { it.toDomain() }
                entity.toDomain(subtasks)
            }
        }
    }

    fun observeTasksWithDeadline(): Flow<List<Task>> {
        return taskDao.observeTasksWithDeadline().map { entities ->
            entities.map { entity ->
                val subtasks = taskDao.getSubtasksForTask(entity.id)
                    .map { it.toDomain() }
                entity.toDomain(subtasks)
            }
        }
    }

    fun observeImportantTasks(): Flow<List<Task>> {
        return taskDao.observeImportantTasks().map { entities ->
            entities.map { entity ->
                val subtasks = taskDao.getSubtasksForTask(entity.id)
                    .map { it.toDomain() }
                entity.toDomain(subtasks)
            }
        }
    }

    // ── CRUD операции ──────────────────────────────────────────

    /**
     * Создание новой задачи.
     * UUID генерируется здесь, а не в DAO, чтобы id был известен сразу —
     * он нужен для планирования уведомления WorkManager.
     */
    suspend fun addTask(task: Task): Task {
        val newTask = task.copy(
            id = task.id.ifEmpty { UUID.randomUUID().toString() },
            syncStatus = SyncStatus.PENDING_UPLOAD
        )

        // 1. Сохраняем в Room немедленно
        taskDao.upsertTask(TaskEntity.fromDomain(newTask))
        newTask.subtasks.forEach { subtask ->
            val newSubtask = subtask.copy(
                id = subtask.id.ifEmpty { UUID.randomUUID().toString() },
                taskId = newTask.id
            )
            taskDao.upsertSubtask(SubtaskEntity.fromDomain(newSubtask))
        }

        // 2. Асинхронная синхронизация с Firestore
        repositoryScope.launch {
            syncTaskToFirestore(newTask)
        }

        return newTask
    }

    suspend fun updateTask(task: Task) {
        val updatedTask = task.copy(syncStatus = SyncStatus.PENDING_UPLOAD)

        // 1. Обновляем Room
        taskDao.upsertTask(TaskEntity.fromDomain(updatedTask))

        // Синхронизируем подзадачи: удаляем старые и вставляем новые
        taskDao.deleteSubtasksForTask(task.id)
        task.subtasks.forEach { subtask ->
            val withId = subtask.copy(
                id = subtask.id.ifEmpty { UUID.randomUUID().toString() },
                taskId = task.id
            )
            taskDao.upsertSubtask(SubtaskEntity.fromDomain(withId))
        }

        // 2. Асинхронная синхронизация
        repositoryScope.launch {
            syncTaskToFirestore(updatedTask)
        }
    }

    suspend fun deleteTask(taskId: String) {
        // 1. Помечаем к удалению в Room (каскадно удаляет подзадачи через ForeignKey)
        taskDao.updateSyncStatus(taskId, SyncStatus.PENDING_DELETE.name)

        // 2. Асинхронно удаляем из Firestore, затем из Room
        repositoryScope.launch {
            try {
                firestoreService.deleteTask(taskId)
                taskDao.deleteTaskById(taskId)
            } catch (e: Exception) {
                // При ошибке оставляем запись с PENDING_DELETE —
                // она будет удалена при следующей синхронизации
            }
        }
    }

    /** Обновление отдельной подзадачи (например, чекбокс). */
    suspend fun updateSubtask(subtask: Subtask) {
        taskDao.upsertSubtask(SubtaskEntity.fromDomain(subtask))

        // Помечаем родительскую задачу как требующую синхронизации
        taskDao.updateSyncStatus(subtask.taskId, SyncStatus.PENDING_UPLOAD.name)

        repositoryScope.launch {
            try {
                firestoreService.saveSubtask(subtask.taskId, subtask)
                // Проверяем, нет ли ещё pending-изменений в этой задаче
                val task = taskDao.getTaskById(subtask.taskId)
                if (task != null) {
                    taskDao.updateSyncStatus(subtask.taskId, SyncStatus.SYNCED.name)
                }
            } catch (e: Exception) {
                // Оставляем PENDING_UPLOAD для повторной попытки
            }
        }
    }

    suspend fun deleteSubtask(taskId: String, subtaskId: String) {
        taskDao.deleteSubtaskById(subtaskId)

        repositoryScope.launch {
            try {
                firestoreService.deleteSubtask(taskId, subtaskId)
            } catch (e: Exception) {
                // Подзадача уже удалена из Room — приоритет локального состояния
            }
        }
    }

    // ── Синхронизация ──────────────────────────────────────────

    /**
     * Отправляет задачу в Firestore и при успехе обновляет syncStatus.
     * Вызывается после каждого добавления/обновления.
     */
    private suspend fun syncTaskToFirestore(task: Task) {
        try {
            firestoreService.saveTask(task)
            taskDao.updateSyncStatus(task.id, SyncStatus.SYNCED.name)
        } catch (e: Exception) {
            // Оставляем PENDING_UPLOAD — retried в syncPendingTasks()
        }
    }

    /**
     * Повторная синхронизация всех накопленных изменений.
     * Вызывается при восстановлении интернет-соединения
     * или при запуске приложения.
     */
    suspend fun syncPendingTasks() {
        val pending = taskDao.getPendingTasks()
        pending.forEach { entity ->
            when (SyncStatus.valueOf(entity.syncStatus)) {
                SyncStatus.PENDING_UPLOAD -> {
                    val subtasks = taskDao.getSubtasksForTask(entity.id)
                        .map { it.toDomain() }
                    syncTaskToFirestore(entity.toDomain(subtasks))
                }
                SyncStatus.PENDING_DELETE -> {
                    try {
                        firestoreService.deleteTask(entity.id)
                        taskDao.deleteTaskById(entity.id)
                    } catch (e: Exception) { /* retry later */ }
                }
                SyncStatus.SYNCED -> { /* nothing to do */ }
            }
        }
    }

    /**
     * Первоначальная загрузка данных из Firestore в Room.
     * Вызывается при первом запуске или после logout/login.
     */
    suspend fun initialSyncFromFirestore() {
        try {
            val remoteTasks = firestoreService.fetchAllTasks()
            remoteTasks.forEach { task ->
                taskDao.upsertTask(TaskEntity.fromDomain(task.copy(syncStatus = SyncStatus.SYNCED)))
                task.subtasks.forEach { subtask ->
                    taskDao.upsertSubtask(SubtaskEntity.fromDomain(subtask))
                }
            }
        } catch (e: Exception) {
            // Продолжаем работу в offline-режиме — данные есть в Room
        }
    }

    suspend fun getTaskById(taskId: String): Task? {
        val entity = taskDao.getTaskById(taskId) ?: return null
        val subtasks = taskDao.getSubtasksForTask(taskId).map { it.toDomain() }
        return entity.toDomain(subtasks)
    }

    /**
     * Задачи с будущим дедлайном — используется в BootReceiver
     * для восстановления будильников после перезагрузки устройства.
     */
    suspend fun getTasksWithFutureDeadlines(): List<Task> {
        return taskDao.getTasksWithDeadlineAfter(System.currentTimeMillis())
            .map { entity ->
                val subtasks = taskDao.getSubtasksForTask(entity.id)
                    .map { it.toDomain() }
                entity.toDomain(subtasks)
            }
    }
}
