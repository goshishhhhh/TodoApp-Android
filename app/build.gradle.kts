plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Compose Compiler как отдельный плагин (Kotlin 2.x+)
    id("org.jetbrains.kotlin.plugin.compose")
    // KSP для генерации кода Room и Hilt
    id("com.google.devtools.ksp")
    // Hilt DI
    id("com.google.dagger.hilt.android")
    // Firebase google-services.json обработчик
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.todoapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.todoapp"
        minSdk = 26          // Android 8.0+ (поддержка JobScheduler/AlarmManager)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room схема экспортируется для миграций
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Включаем десугаринг для java.time на API < 26 (у нас minSdk=26, но оставляем для совместимости)
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Версии зависимостей вынесены в переменные для удобства обновления
// ──────────────────────────────────────────────────────────────
val roomVersion            = "2.6.1"
val hiltVersion            = "2.52"
val hiltAndroidxVersion    = "1.2.0"
val firebaseBomVersion     = "33.5.1"
val workManagerVersion     = "2.9.1"
val navigationVersion      = "2.8.4"
val coroutinesVersion      = "1.9.0"
val lifecycleVersion       = "2.8.7"
val composeBomVersion      = "2024.12.01"

dependencies {

    // ── Core AndroidX ──────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Десугаринг java.time
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // ── Jetpack Compose BOM ────────────────────────────────────
    // BOM фиксирует совместимые версии всех Compose-артефактов
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Расширенный набор иконок (нужны для задач: флаг важности, дедлайн и т.д.)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // ── Navigation ─────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:$navigationVersion")

    // ── ViewModel + Lifecycle ──────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")

    // ── Room (локальная база данных / кэш) ─────────────────────
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")           // Flow + корутины
    ksp("androidx.room:room-compiler:$roomVersion")                 // Генерация кода

    // ── Hilt (Dependency Injection) ────────────────────────────
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    // Hilt-интеграция с Navigation Compose (hiltViewModel())
    implementation("androidx.hilt:hilt-navigation-compose:$hiltAndroidxVersion")
    // Hilt-интеграция с WorkManager
    implementation("androidx.hilt:hilt-work:$hiltAndroidxVersion")
    ksp("androidx.hilt:hilt-compiler:$hiltAndroidxVersion")

    // ── Firebase BOM ───────────────────────────────────────────
    // BOM гарантирует совместимость версий всех Firebase SDK
    implementation(platform("com.google.firebase:firebase-bom:$firebaseBomVersion"))
    implementation("com.google.firebase:firebase-firestore-ktx")    // Firestore
    implementation("com.google.firebase:firebase-auth-ktx")         // Auth (анонимный)

    // ── WorkManager ────────────────────────────────────────────
    implementation("androidx.work:work-runtime-ktx:$workManagerVersion")

    // ── Coroutines ─────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    // Адаптеры для Firebase Tasks → корутины (await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion")

    // ── Сплэш-экран (опционально, красиво) ─────────────────────
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ── Тесты ─────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
