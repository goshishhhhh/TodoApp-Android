package com.example.todoapp.di

import com.example.todoapp.worker.AlarmManagerScheduler
import com.example.todoapp.worker.NotificationScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModule {
    /**
     * Биндим интерфейс NotificationScheduler на конкретную реализацию AlarmManagerScheduler.
     * ViewModel и BootReceiver зависят от интерфейса — легко заменять реализацию в тестах.
     */
    @Binds
    @Singleton
    abstract fun bindNotificationScheduler(
        impl: AlarmManagerScheduler
    ): NotificationScheduler
}
