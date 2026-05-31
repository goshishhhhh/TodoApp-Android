package com.example.todoapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Light palette ────────────────────────────────────────────
val Primary       = Color(0xFF4F6BF0)   // синий акцент
val OnPrimary     = Color(0xFFFFFFFF)
val PrimaryContainer   = Color(0xFFDDE3FF)
val OnPrimaryContainer = Color(0xFF001258)

val Secondary     = Color(0xFF5C5F72)
val SecondaryContainer   = Color(0xFFE1E2F5)
val OnSecondaryContainer = Color(0xFF191B2C)

val Background    = Color(0xFFFAF9FF)
val Surface       = Color(0xFFFAF9FF)
val SurfaceVariant = Color(0xFFE3E1EC)
val OnSurfaceVariant = Color(0xFF46464F)

val Error         = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)

// ── Dark palette ─────────────────────────────────────────────
val PrimaryDark   = Color(0xFFB7C4FF)
val OnPrimaryDark = Color(0xFF0D2578)
val PrimaryContainerDark   = Color(0xFF2E47D7)
val OnPrimaryContainerDark = Color(0xFFDDE3FF)

val BackgroundDark = Color(0xFF12131C)
val SurfaceDark    = Color(0xFF12131C)
val SurfaceVariantDark = Color(0xFF46464F)
val OnSurfaceVariantDark = Color(0xFFC7C5D0)

val ErrorDark     = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)

// ── Семантические цвета (используются в компонентах) ─────────
val ImportantColor  = Color(0xFFE53935)   // красный для важных задач
val OverdueColor    = Color(0xFFFF6D00)   // оранжевый для просроченных
val CompletedColor  = Color(0xFF43A047)   // зелёный для выполненных
