package com.example.todoapp.ui.screens.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.model.Subtask
import com.example.todoapp.data.model.Task
import com.example.todoapp.data.repository.TaskRepository
import com.example.todoapp.domain.usecase.AddTaskUseCase
import com.example.todoapp.domain.usecase.DeleteSubtaskUseCase
import com.example.todoapp.domain.usecase.UpdateSubtaskUseCase
import com.example.todoapp.domain.usecase.UpdateTaskUseCase
import com.example.todoapp.ui.navigation.Screen
import com.example.todoapp.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TaskDetailUiState(
    val taskId: String          = "",
    val title: String           = "",
    val description: String     = "",
    val isCompleted: Boolean    = false,
    val isImportant: Boolean    = false,
    val deadlineMillis: Long?   = null,
    val subtasks: List<Subtask> = emptyList(),
    val isNewTask: Boolean      = true,
    val isLoading: Boolean      = false,
    val isSaved: Boolean        = false,    // сигнал для навигации назад
    val error: String?          = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
    private val addTaskUseCase: AddTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val updateSubtaskUseCase: UpdateSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])
    private val isNew: Boolean = taskId == Screen.TaskDetail.NEW_TASK_ID

    private val _uiState = MutableStateFlow(
        TaskDetailUiState(
            taskId    = if (isNew) UUID.randomUUID().toString() else taskId,
            isNewTask = isNew
        )
    )
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        if (!isNew) loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val task = repository.getTaskById(taskId)
            if (task != null) {
                _uiState.update { state ->
                    state.copy(
                        taskId       = task.id,
                        title        = task.title,
                        description  = task.description,
                        isCompleted  = task.isCompleted,
                        isImportant  = task.isImportant,
                        deadlineMillis = task.deadlineMillis,
                        subtasks     = task.subtasks,
                        isLoading    = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Задача не найдена") }
            }
        }
    }

    // ── Обновление полей формы ─────────────────────────────────

    fun onTitleChange(title: String)             = _uiState.update { it.copy(title = title) }
    fun onDescriptionChange(desc: String)        = _uiState.update { it.copy(description = desc) }
    fun onImportantToggle()                      = _uiState.update { it.copy(isImportant = !it.isImportant) }
    fun onDeadlineChange(millis: Long?)          = _uiState.update { it.copy(deadlineMillis = millis) }

    fun addSubtask(title: String) {
        if (title.isBlank()) return
        val newSubtask = Subtask(
            id     = UUID.randomUUID().toString(),
            taskId = _uiState.value.taskId,
            title  = title.trim()
        )
        _uiState.update { it.copy(subtasks = it.subtasks + newSubtask) }
    }

    fun toggleSubtask(subtask: Subtask) {
        val updated = subtask.copy(isCompleted = !subtask.isCompleted)
        _uiState.update { state ->
            state.copy(subtasks = state.subtasks.map {
                if (it.id == subtask.id) updated else it
            })
        }
        // Если задача уже существует — синхронизируем подзадачу немедленно
        if (!isNew) {
            viewModelScope.launch { updateSubtaskUseCase(updated) }
        }
    }

    fun removeSubtask(subtask: Subtask) {
        _uiState.update { state ->
            state.copy(subtasks = state.subtasks.filter { it.id != subtask.id })
        }
        if (!isNew) {
            viewModelScope.launch {
                deleteSubtaskUseCase(_uiState.value.taskId, subtask.id)
            }
        }
    }

    // ── Сохранение ────────────────────────────────────────────

    fun saveTask() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Введите заголовок задачи") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val task = Task(
                id             = state.taskId,
                title          = state.title.trim(),
                description    = state.description.trim(),
                isCompleted    = state.isCompleted,
                isImportant    = state.isImportant,
                deadlineMillis = state.deadlineMillis,
                subtasks       = state.subtasks
            )

            val result = if (isNew) addTaskUseCase(task) else updateTaskUseCase(task)

            result
                .onSuccess { savedTask ->
                    // Планируем уведомление за 2 часа до дедлайна
                    val actualTask = if (isNew) savedTask as Task else task
                    actualTask.deadlineMillis?.let { deadline ->
                        notificationScheduler.scheduleDeadlineNotification(actualTask)
                    } ?: run {
                        // Если дедлайн убран — отменяем ранее запланированное уведомление
                        notificationScheduler.cancelNotification(task.id)
                    }
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
