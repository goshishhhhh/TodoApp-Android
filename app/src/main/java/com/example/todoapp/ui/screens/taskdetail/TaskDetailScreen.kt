package com.example.todoapp.ui.screens.taskdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todoapp.data.model.Subtask
import com.example.todoapp.ui.components.ImportantBadge
import com.example.todoapp.ui.components.SubtaskItem
import com.example.todoapp.ui.theme.ImportantColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Навигация назад при успешном сохранении
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    // Показываем ошибку через Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isNewTask) "Новая задача" else "Редактировать")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    // Кнопка «Важная задача»
                    IconButton(onClick = viewModel::onImportantToggle) {
                        Icon(
                            imageVector = if (state.isImportant) Icons.Default.Star
                                          else Icons.Default.StarBorder,
                            contentDescription = "Важная задача",
                            tint = if (state.isImportant) ImportantColor
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Кнопка сохранения
                    IconButton(
                        onClick  = viewModel::saveTask,
                        enabled  = !state.isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Заголовок ──────────────────────────────────────
            OutlinedTextField(
                value         = state.title,
                onValueChange = viewModel::onTitleChange,
                label         = { Text("Заголовок *") },
                placeholder   = { Text("Название задачи") },
                singleLine    = true,
                isError       = state.error != null && state.title.isBlank(),
                modifier      = Modifier.fillMaxWidth(),
                trailingIcon  = if (state.isImportant) {
                    { ImportantBadge() }
                } else null
            )

            // ── Описание ───────────────────────────────────────
            OutlinedTextField(
                value         = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label         = { Text("Описание") },
                placeholder   = { Text("Дополнительные детали...") },
                minLines      = 3,
                maxLines      = 6,
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Дедлайн ───────────────────────────────────────
            DeadlinePicker(
                deadlineMillis = state.deadlineMillis,
                onDeadlineChange = viewModel::onDeadlineChange
            )

            HorizontalDivider()

            // ── Подзадачи ─────────────────────────────────────
            SubtasksSection(
                subtasks      = state.subtasks,
                onToggle      = viewModel::toggleSubtask,
                onDelete      = viewModel::removeSubtask,
                onAddSubtask  = viewModel::addSubtask
            )
        }
    }
}

// ── Компонент выбора дедлайна ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlinePicker(
    deadlineMillis: Long?,
    onDeadlineChange: (Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Буферизуем выбранную дату до подтверждения времени
    var selectedDateMillis by remember(deadlineMillis) {
        mutableStateOf(deadlineMillis ?: System.currentTimeMillis())
    }

    val dateStr = deadlineMillis?.let {
        SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date(it))
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(dateStr ?: "Установить дедлайн", maxLines = 1)
        }

        if (deadlineMillis != null) {
            IconButton(onClick = { onDeadlineChange(null) }) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Убрать дедлайн",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // ── DatePickerDialog ──────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Далее") }
            },
            dismissButton    = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── TimePickerDialog ──────────────────────────────────────
    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
        }
        val timePickerState = rememberTimePickerState(
            initialHour   = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour      = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title   = { Text("Выберите время") },
            text    = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalMillis = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    onDeadlineChange(finalMillis)
                    showTimePicker = false
                }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            }
        )
    }
}

// ── Секция подзадач ──────────────────────────────────────────────

@Composable
private fun SubtasksSection(
    subtasks: List<Subtask>,
    onToggle: (Subtask) -> Unit,
    onDelete: (Subtask) -> Unit,
    onAddSubtask: (String) -> Unit
) {
    var newSubtaskTitle by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Text(
        text  = "Подзадачи",
        style = MaterialTheme.typography.titleMedium
    )

    // Список существующих подзадач
    subtasks.forEach { subtask ->
        SubtaskItem(
            subtask         = subtask,
            onCheckedChange = { onToggle(subtask) },
            onDelete        = { onDelete(subtask) }
        )
    }

    // Поле ввода новой подзадачи
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value         = newSubtaskTitle,
            onValueChange = { newSubtaskTitle = it },
            placeholder   = { Text("Добавить подзадачу") },
            singleLine    = true,
            modifier      = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        IconButton(
            onClick  = {
                onAddSubtask(newSubtaskTitle)
                newSubtaskTitle = ""
            },
            enabled  = newSubtaskTitle.isNotBlank()
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Добавить подзадачу",
                tint = if (newSubtaskTitle.isNotBlank())
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}
