package com.example.todoapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoapp.data.model.Subtask

@Entity(
    tableName = "subtasks",
    // ForeignKey гарантирует каскадное удаление подзадач при удалении родительской задачи
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]         // индекс для быстрой выборки по taskId
)
data class SubtaskEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,                 // FK → tasks.id
    val title: String,
    val isCompleted: Boolean
) {
    fun toDomain() = Subtask(
        id = id,
        taskId = taskId,
        title = title,
        isCompleted = isCompleted
    )

    companion object {
        fun fromDomain(subtask: Subtask) = SubtaskEntity(
            id = subtask.id,
            taskId = subtask.taskId,
            title = subtask.title,
            isCompleted = subtask.isCompleted
        )
    }
}
