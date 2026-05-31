package com.example.todoapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary             = Primary,
    onPrimary           = OnPrimary,
    primaryContainer    = PrimaryContainer,
    onPrimaryContainer  = OnPrimaryContainer,
    secondary           = Secondary,
    secondaryContainer  = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background          = Background,
    surface             = Surface,
    surfaceVariant      = SurfaceVariant,
    onSurfaceVariant    = OnSurfaceVariant,
    error               = Error,
    errorContainer      = ErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary             = PrimaryDark,
    onPrimary           = OnPrimaryDark,
    primaryContainer    = PrimaryContainerDark,
    onPrimaryContainer  = OnPrimaryContainerDark,
    background          = BackgroundDark,
    surface             = SurfaceDark,
    surfaceVariant      = SurfaceVariantDark,
    onSurfaceVariant    = OnSurfaceVariantDark,
    error               = ErrorDark,
    errorContainer      = ErrorContainerDark
)

@Composable
fun TodoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic Color доступен на Android 12+ (Material You)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
