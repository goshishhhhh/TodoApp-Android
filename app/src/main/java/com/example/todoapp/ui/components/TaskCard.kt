package com.example.todoapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.model.Task
import com.example.todoapp.ui.theme.CompletedColor
import com.example.todoapp.ui.theme.ImportantColor
import com.example.todoapp.ui.theme.OverdueColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Карточка задачи для списка.
 * Отображает: заголовок, описание (2 строки), дедлайн, бейдж важности, прогресс подзадач.
 * Просроченные задачи выделяются оранжевой рамкой.
 */
@Composable
fun TaskCard(
    task: Task,
    onTaskClick: () -> Unit,
    onCompleteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isOverdue = task.deadlineMillis != null &&
            task.deadlineMillis < now &&
            !task.isCompleted

    val borderColor by animateColorAsState(
        targetValue = when {
            isOverdue       -> OverdueColor
            task.isImportant -> ImportantColor.copy(alpha = 0.5f)
            else            -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        },
        label = "card_border"
    )

    OutlinedCard(
        onClick  = onTaskClick,
        modifier = modifier.fillMaxWidth(),
        border   = BorderStroke(
            width = if (isOverdue || task.isImportant) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ── Чекбокс-иконка ──────────────────────────────
            IconButton(
                onClick  = onCompleteToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted)
                        Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Отметить выполненной",
                    tint = if (task.isCompleted)
                        CompletedColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // ── Содержимое карточки ──────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                // Заголовок
                Text(
                    text  = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    ),
                    color    = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Описание
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = task.description,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Нижняя строка: дедлайн + бейдж важности + прогресс подзадач
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Дедлайн
                    if (task.deadlineMillis != null) {
                        DeadlineChip(
                            deadlineMillis = task.deadlineMillis,
                            isOverdue      = isOverdue
                        )
                    }

                    // Важность
                    if (task.isImportant) {
                        ImportantBadge()
                    }

                    // Прогресс подзадач
                    if (task.subtasks.isNotEmpty()) {
                        val completed = task.subtasks.count { it.isCompleted }
                        Text(
                            text  = "$completed/${task.subtasks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineChip(deadlineMillis: Long, isOverdue: Boolean) {
    val dateStr = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
        .format(Date(deadlineMillis))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.AccessTime,
            contentDescription = null,
            modifier           = Modifier.size(12.dp),
            tint               = if (isOverdue) OverdueColor
                                 else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text  = dateStr,
            style = MaterialTheme.typography.labelSmall,
            color = if (isOverdue) OverdueColor
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
