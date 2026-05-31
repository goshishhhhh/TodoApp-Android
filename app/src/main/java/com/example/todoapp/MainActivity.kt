package com.example.todoapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.todoapp.ui.navigation.AppNavigation
import com.example.todoapp.ui.theme.TodoAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Единственная Activity приложения.
 * @AndroidEntryPoint — позволяет Hilt инжектировать зависимости в Activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Запрос разрешения на отправку уведомлений (Android 13+ / API 33+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* разрешение получено или отклонено — не блокируем работу приложения */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen должен быть установлен ДО super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Запрашиваем разрешение на уведомления на Android 13+
        requestNotificationPermission()

        setContent {
            TodoAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
