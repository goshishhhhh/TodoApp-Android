// Project-level build.gradle.kts
// Объявляем плагины, но НЕ применяем их здесь (apply false).
// Каждый модуль подключит нужный плагин самостоятельно.
plugins {
    id("com.android.application")         version "8.5.2" apply false
    id("com.android.library")             version "8.5.2" apply false
    id("org.jetbrains.kotlin.android")    version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // KSP — процессор аннотаций (нужен для Room + Hilt)
    id("com.google.devtools.ksp")         version "2.0.21-1.0.28" apply false
    // Hilt — Dependency Injection от Google
    id("com.google.dagger.hilt.android")  version "2.52" apply false
    // Google Services — подключает google-services.json для Firebase
    id("com.google.gms.google-services")  version "4.4.2" apply false
}
