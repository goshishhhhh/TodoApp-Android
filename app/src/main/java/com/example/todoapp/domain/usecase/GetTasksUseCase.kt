package com.example.todoapp.domain.usecase

import com.example.todoapp.data.model.Task
import com.example.todoapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Получение всех задач с возможностью фильтрации по важности. */
class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    /** @param onlyImportant — если true, возвращает только важные задачи */
    operator fun invoke(onlyImportant: Boolean = false): Flow<List<Task>> {
        return if (onlyImportant) {
            repository.observeImportantTasks()
        } else {
            repository.observeAllTasks()
        }
    }
}

/** Получение задач с дедлайнами (для экрана «Дедлайны»). */
class GetDeadlineTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> = repository.observeTasksWithDeadline()
}
