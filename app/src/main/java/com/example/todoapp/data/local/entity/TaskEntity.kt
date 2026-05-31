package com.example.todoapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todoapp.data.model.SyncStatus
import com.example.todoapp.data.model.Task

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val isImportant: Boolean,
    val deadlineMillis: Long?,          // nullable — дедлайн не обязателен
    val createdAtMillis: Long,
    val syncStatus: String              // хранится как String (Room не знает о enum)
) {
    /** Конвертация Entity → Domain модель (без subtasks — они загружаются отдельно). */
    fun toDomain(subtasks: List<com.example.todoapp.data.model.Subtask> = emptyList()) = Task(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        isImportant = isImportant,
        deadlineMillis = deadlineMillis,
        createdAtMillis = createdAtMillis,
        subtasks = subtasks,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )

    companion object {
        /** Конвертация Domain модели → Entity. */
        fun fromDomain(task: Task) = TaskEntity(
            id = task.id,
            title = task.title,
            description = task.description,
            isCompleted = task.isCompleted,
            isImportant = task.isImportant,
            deadlineMillis = task.deadlineMillis,
            createdAtMillis = task.createdAtMillis,
            syncStatus = task.syncStatus.name
        )
    }
}
