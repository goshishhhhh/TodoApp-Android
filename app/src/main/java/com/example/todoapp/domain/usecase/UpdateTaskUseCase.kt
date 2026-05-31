package com.example.todoapp.domain.usecase

import com.example.todoapp.data.model.Subtask
import com.example.todoapp.data.model.Task
import com.example.todoapp.data.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task): Result<Unit> = runCatching {
        require(task.title.isNotBlank()) { "Заголовок задачи не может быть пустым" }
        repository.updateTask(task)
    }
}

class UpdateSubtaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(subtask: Subtask): Result<Unit> = runCatching {
        repository.updateSubtask(subtask)
    }
}
