package com.example.todoapp.domain.usecase

import com.example.todoapp.data.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> = runCatching {
        repository.deleteTask(taskId)
    }
}

class DeleteSubtaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, subtaskId: String): Result<Unit> = runCatching {
        repository.deleteSubtask(taskId, subtaskId)
    }
}
