package com.example.todoapp.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().also { db ->
            /**
             * Настраиваем offline persistence для Firestore.
             * persistentCacheSettings — данные сохраняются на диске между сессиями.
             * Это дополняет наш Room-кэш: Firestore сам по себе кэширует
             * данные для real-time listener-ов, но Room даёт нам полный контроль.
             */
            db.firestoreSettings = firestoreSettings {
                setLocalCacheSettings(
                    persistentCacheSettings { setSizeBytes(50 * 1024 * 1024L) } // 50 MB
                )
            }
        }
    }
}
