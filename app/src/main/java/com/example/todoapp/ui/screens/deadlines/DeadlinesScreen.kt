package com.example.todoapp.ui.screens.deadlines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoapp.data.model.Task
import com.example.todoapp.ui.components.TaskCard
import com.example.todoapp.ui.theme.OverdueColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinesScreen(
    onTaskClick: (String) -> Unit,
    viewModel: DeadlinesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дедлайны") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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

                state.overdueTasks.isEmpty() && state.upcomingTasks.isEmpty() -> {
                    Text(
                        text      = "Нет задач с дедлайном",
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                    )
                }

                else -> {
                    DeadlinesList(
                        overdueTasks     = state.overdueTasks,
                        upcomingTasks    = state.upcomingTasks,
                        onTaskClick      = onTaskClick,
                        onCompleteToggle = viewModel::toggleTaskComplete
                    )
                }
            }
        }
    }
}

@Composable
private fun DeadlinesList(
    overdueTasks: List<Task>,
    upcomingTasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onCompleteToggle: (Task) -> Unit
) {
    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        // ── Секция «Просрочено» ──────────────────────────────
        if (overdueTasks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Просрочено (${overdueTasks.size})",
                    color = OverdueColor
                )
            }
            items(overdueTasks, key = { it.id }) { task ->
                TaskCard(
                    task             = task,
                    onTaskClick      = { onTaskClick(task.id) },
                    onCompleteToggle = { onCompleteToggle(task) }
                )
            }
        }

        // ── Секция «Предстоящие» ─────────────────────────────
        if (upcomingTasks.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Предстоящие",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(upcomingTasks, key = { it.id }) { task ->
                TaskCard(
                    task             = task,
                    onTaskClick      = { onTaskClick(task.id) },
                    onCompleteToggle = { onCompleteToggle(task) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge,
        color    = color,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
