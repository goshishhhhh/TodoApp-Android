package com.example.todoapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.ui.theme.ImportantColor

/** Иконка-бейдж «Важная задача». Используется в карточке и на экране деталей. */
@Composable
fun ImportantBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector        = Icons.Default.Star,
            contentDescription = "Важная задача",
            tint               = ImportantColor,
            modifier           = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text  = "Важная",
            style = MaterialTheme.typography.labelSmall,
            color = ImportantColor
        )
    }
}
