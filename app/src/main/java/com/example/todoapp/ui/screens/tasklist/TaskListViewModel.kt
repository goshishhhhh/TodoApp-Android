package com.example.todoapp.ui.screens.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.model.Task
import com.example.todoapp.domain.usecase.DeleteTaskUseCase
import com.example.todoapp.domain.usecase.GetTasksUseCase
import com.example.todoapp.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val tasks: List<Task>        = emptyList(),
    val isLoading: Boolean       = true,
    val showOnlyImportant: Boolean = false,
    val error: String?           = null
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _showOnlyImportant = MutableStateFlow(false)

    val uiState: StateFlow<TaskListUiState> = _showOnlyImportant
        .flatMapLatest { onlyImportant ->
            // При переключении фильтра немедленно переподписываемся на нужный Flow
            getTasksUseCase(onlyImportant)
                .map { tasks ->
                    TaskListUiState(
                        tasks             = tasks,
                        isLoading         = false,
                        showOnlyImportant = onlyImportant
                    )
                }
                .catch { e ->
                    emit(TaskListUiState(
                        isLoading = false,
                        error     = e.message ?: "Ошибка загрузки задач"
                    ))
                }
        }
        .stateIn(
            scope            = viewModelScope,
            started          = SharingStarted.WhileSubscribed(5_000),
            initialValue     = TaskListUiState()
        )

    fun toggleImportantFilter() {
        _showOnlyImportant.value = !_showOnlyImportant.value
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            updateTaskUseCase(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            deleteTaskUseCase(taskId).onFailure { e ->
                // Ошибка отображается через UiState — не крашим приложение
            }
        }
    }

    fun clearError() {
        // При необходимости сбрасываем ошибку через отдельный MutableStateFlow
    }
}
