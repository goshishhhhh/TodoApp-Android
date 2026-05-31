package com.example.todoapp.domain.usecase

import com.example.todoapp.data.model.Task
import com.example.todoapp.data.repository.TaskRepository
import javax.inject.Inject

class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    /**
     * Добавляет задачу и возвращает её с заполненным id.
     * Result-обёртка позволяет ViewModel обработать ошибку без крашей.
     */
    suspend operator fun invoke(task: Task): Result<Task> = runCatching {
        require(task.title.isNotBlank()) { "Заголовок задачи не может быть пустым" }
        repository.addTask(task)
    }
}
