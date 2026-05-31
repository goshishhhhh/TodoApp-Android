package com.example.todoapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.todoapp.data.local.dao.TaskDao
import com.example.todoapp.data.local.entity.SubtaskEntity
import com.example.todoapp.data.local.entity.TaskEntity

/**
 * Room Database — единая точка доступа к локальному SQLite.
 *
 * exportSchema = true → схема сохраняется в app/schemas/ для контроля миграций.
 * При изменении version обязательно добавляй Migration объект.
 */
@Database(
    entities = [TaskEntity::class, SubtaskEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "todo_database"
    }
}
