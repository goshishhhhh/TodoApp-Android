package com.example.todoapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.todoapp.worker.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Главный класс Application.
 *
 * @HiltAndroidApp — точка входа для Hilt DI. Генерирует компонент Hilt
 * и инициализирует граф зависимостей при старте приложения.
 *
 * Реализует Configuration.Provider для ручной инициализации WorkManager
 * с HiltWorkerFactory — это позволяет инжектить зависимости прямо в Worker-классы.
 */
@HiltAndroidApp
class TodoApplication : Application(), Configuration.Provider {

    // WorkerFactory от Hilt — инжектируется автоматически
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Планируем фоновую синхронизацию с Firestore (раз в 15 мин при наличии сети)
        SyncWorker.schedulePeriodicSync(this)
    }

    // WorkManager конфигурация с Hilt-фабрикой
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /**
     * Создаём канал уведомлений для Android 8.0+ (API 26+).
     * Без канала уведомления на новых версиях Android не отображаются.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
