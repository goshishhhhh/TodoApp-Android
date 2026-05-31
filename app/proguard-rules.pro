# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room — сохраняем data-классы сущностей
-keep class com.example.todoapp.data.local.entity.** { *; }

# Firebase Firestore — data-классы должны иметь пустой конструктор
-keep class com.example.todoapp.data.model.** { *; }
-keepclassmembers class com.example.todoapp.data.model.** {
    <init>();
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class com.example.todoapp.worker.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
