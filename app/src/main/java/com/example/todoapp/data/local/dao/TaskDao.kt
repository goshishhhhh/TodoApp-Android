package com.example.todoapp.data.local.dao

import androidx.room.*
import com.example.todoapp.data.local.entity.SubtaskEntity
import com.example.todoapp.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ── Задачи ─────────────────────────────────────────────────

    /**
     * Наблюдаемый список всех задач.
     * Flow автоматически эмитит новые значения при любом изменении в таблице.
     */
    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    /**
     * Задачи с дедлайном, отсортированные по возрастанию дедлайна.
     * NULL дедлайны исключаются — они не нужны на экране «Дедлайны».
     */
    @Query("""
        SELECT * FROM tasks
        WHERE deadlineMillis IS NOT NULL
        ORDER BY deadlineMillis ASC
    """)
    fun observeTasksWithDeadline(): Flow<List<TaskEntity>>

    /** Только важные задачи. */
    @Query("SELECT * FROM tasks WHERE isImportant = 1 ORDER BY createdAtMillis DESC")
    fun observeImportantTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    /** Задачи, ожидающие отправки в Firestore (offline-first). */
    @Query("SELECT * FROM tasks WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingTasks(): List<TaskEntity>

    /**
     * REPLACE: если запись с таким id уже есть — обновляет,
     * если нет — вставляет. Идеально для upsert-операций.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("UPDATE tasks SET syncStatus = :status WHERE id = :taskId")
    suspend fun updateSyncStatus(taskId: String, status: String)

    // ── Подзадачи ──────────────────────────────────────────────

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    fun observeSubtasks(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    suspend fun getSubtasksForTask(taskId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtask(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtasks(subtasks: List<SubtaskEntity>)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtaskById(subtaskId: String)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: String)

    /**
     * Задачи с будущим дедлайном (не завершённые).
     * Используется в BootReceiver для восстановления AlarmManager после перезагрузки.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE deadlineMillis IS NOT NULL
          AND deadlineMillis > :afterMillis
          AND isCompleted = 0
    """)
    suspend fun getTasksWithDeadlineAfter(afterMillis: Long): List<TaskEntity>
}
