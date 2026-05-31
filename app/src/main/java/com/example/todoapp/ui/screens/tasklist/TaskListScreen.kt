package com.example.todoapp.ui.screens.tasklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoapp.ui.components.TaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onTaskClick: (String) -> Unit,
    onAddTaskClick: () -> Unit,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Задачи") },
                actions = {
                    // Кнопка фильтра «Только важные»
                    IconButton(onClick = viewModel::toggleImportantFilter) {
                        Icon(
                            imageVector = if (state.showOnlyImportant)
                                Icons.Default.Star
                            else Icons.Outlined.StarBorder,
                            contentDescription = "Фильтр по важности",
                            tint = if (state.showOnlyImportant)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTaskClick) {
                Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    Text(
                        text      = state.error!!,
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                    )
                }

                state.tasks.isEmpty() -> {
                    EmptyTasksPlaceholder(
                        isFiltered = state.showOnlyImportant,
                        modifier   = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    TaskList(
                        tasks            = state.tasks,
                        onTaskClick      = onTaskClick,
                        onCompleteToggle = viewModel::toggleTaskComplete,
                        onDeleteTask     = viewModel::deleteTask
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskList(
    tasks: List<com.example.todoapp.data.model.Task>,
    onTaskClick: (String) -> Unit,
    onCompleteToggle: (com.example.todoapp.data.model.Task) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    // Сначала невыполненные, потом выполненные
    val sortedTasks = remember(tasks) {
        tasks.sortedWith(compareBy({ it.isCompleted }, { it.createdAtMillis }))
    }

    LazyColumn(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.fillMaxSize()
    ) {
        items(
            items = sortedTasks,
            key   = { it.id }   // стабильные ключи для анимации вставки/удаления
        ) { task ->
            // SwipeToDismiss для удаления свайпом
            SwipeToDismissTaskCard(
                task             = task,
                onTaskClick      = { onTaskClick(task.id) },
                onCompleteToggle = { onCompleteToggle(task) },
                onDelete         = { onDeleteTask(task.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissTaskCard(
    task: com.example.todoapp.data.model.Task,
    onTaskClick: () -> Unit,
    onCompleteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state           = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Красный фон при свайпе влево
            Box(
                modifier          = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment  = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint               = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        TaskCard(
            task             = task,
            onTaskClick      = onTaskClick,
            onCompleteToggle = onCompleteToggle
        )
    }
}

@Composable
private fun EmptyTasksPlaceholder(isFiltered: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier              = modifier.padding(32.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text  = if (isFiltered) "Нет важных задач" else "Нет задач",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text      = if (isFiltered)
                "Пометьте задачи звёздочкой, чтобы они появились здесь"
            else
                "Нажмите + чтобы добавить первую задачу",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
