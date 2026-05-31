package com.example.todoapp.ui.screens.deadlines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.model.Task
import com.example.todoapp.domain.usecase.GetDeadlineTasksUseCase
import com.example.todoapp.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeadlinesUiState(
    val overdueTasks: List<Task>   = emptyList(),
    val upcomingTasks: List<Task>  = emptyList(),
    val isLoading: Boolean         = true
)

@HiltViewModel
class DeadlinesViewModel @Inject constructor(
    getDeadlineTasksUseCase: GetDeadlineTasksUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
) : ViewModel() {

    val uiState: StateFlow<DeadlinesUiState> = getDeadlineTasksUseCase()
        .map { tasks ->
            val now = System.currentTimeMillis()
            DeadlinesUiState(
                overdueTasks  = tasks.filter {
                    it.deadlineMillis != null && it.deadlineMillis < now && !it.isCompleted
                },
                upcomingTasks = tasks.filter {
                    it.deadlineMillis != null && (it.deadlineMillis >= now || it.isCompleted)
                },
                isLoading = false
            )
        }
        .catch { emit(DeadlinesUiState(isLoading = false)) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeadlinesUiState()
        )

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            updateTaskUseCase(task.copy(isCompleted = !task.isCompleted))
        }
    }
}
