# Todo App — Структура проекта

## Дерево директорий

```
TodoApp/
├── build.gradle.kts                   # Project-level: объявление плагинов
├── settings.gradle.kts                # Подключение модулей + репозитории
├── gradle.properties                  # Глобальные настройки Gradle
├── gradle/wrapper/
│   └── gradle-wrapper.properties      # Версия Gradle (8.9)
│
└── app/
    ├── build.gradle.kts               # App-level: зависимости, конфиг Android
    ├── proguard-rules.pro             # ProGuard правила для release-сборки
    │
    └── src/main/
        ├── AndroidManifest.xml        # Разрешения, компоненты Android
        ├── res/
        │   └── values/
        │       ├── strings.xml
        │       └── themes.xml
        │
        └── java/com/example/todoapp/
            │
            ├── TodoApplication.kt     # Application класс + Hilt + WorkManager + NotifChannel
            ├── MainActivity.kt        # Единственная Activity + Navigation host
            │
            ├── data/                  # Слой данных (ШАГ 2)
            │   ├── model/
            │   │   ├── Task.kt        # Domain модель задачи
            │   │   └── Subtask.kt     # Domain модель подзадачи
            │   ├── local/
            │   │   ├── entity/
            │   │   │   ├── TaskEntity.kt     # Room сущность
            │   │   │   └── SubtaskEntity.kt  # Room сущность
            │   │   ├── dao/
            │   │   │   └── TaskDao.kt        # Room DAO интерфейс
            │   │   └── database/
            │   │       └── AppDatabase.kt    # Room Database
            │   ├── remote/
            │   │   └── FirestoreService.kt   # Firestore CRUD
            │   └── repository/
            │       └── TaskRepository.kt     # Offline-first логика
            │
            ├── domain/                # Слой бизнес-логики (ШАГ 2)
            │   └── usecase/
            │       ├── GetTasksUseCase.kt
            │       ├── AddTaskUseCase.kt
            │       ├── UpdateTaskUseCase.kt
            │       └── DeleteTaskUseCase.kt
            │
            ├── ui/                    # Слой UI (ШАГ 3)
            │   ├── navigation/
            │   │   └── AppNavigation.kt      # NavHost + маршруты
            │   ├── theme/
            │   │   ├── Color.kt
            │   │   ├── Theme.kt
            │   │   └── Type.kt
            │   ├── components/
            │   │   ├── TaskCard.kt           # Карточка задачи
            │   │   ├── SubtaskItem.kt        # Элемент подзадачи
            │   │   └── PriorityBadge.kt      # Бейдж приоритета
            │   └── screens/
            │       ├── tasklist/
            │       │   ├── TaskListScreen.kt
            │       │   └── TaskListViewModel.kt
            │       ├── taskdetail/
            │       │   ├── TaskDetailScreen.kt
            │       │   └── TaskDetailViewModel.kt
            │       └── deadlines/
            │           ├── DeadlinesScreen.kt
            │           └── DeadlinesViewModel.kt
            │
            ├── worker/                # Уведомления (ШАГ 4)
            │   ├── DeadlineNotificationWorker.kt
            │   ├── AlarmReceiver.kt
            │   └── BootReceiver.kt
            │
            └── di/                    # Hilt модули (ШАГ 2)
                ├── AppModule.kt       # Firebase, общие зависимости
                └── DatabaseModule.kt  # Room, Repository
```

## Зависимости (ключевые)

| Библиотека | Версия | Назначение |
|---|---|---|
| Kotlin | 2.0.21 | Язык |
| AGP | 8.5.2 | Android Gradle Plugin |
| Compose BOM | 2024.12.01 | UI фреймворк |
| Material3 | (BOM) | Design System |
| Navigation Compose | 2.8.4 | Навигация |
| Room | 2.6.1 | Локальная БД |
| Hilt | 2.52 | DI |
| Firebase BOM | 33.5.1 | Cloud sync |
| WorkManager | 2.9.1 | Фоновые задачи |
| Coroutines | 1.9.0 | Асинхронность |
| KSP | 2.0.21-1.0.28 | Генерация кода |

## Шаги реализации

- [x] **Шаг 1** — Структура + build.gradle.kts
- [ ] **Шаг 2** — data/ (модели, Room, Firestore, Repository, DI)
- [ ] **Шаг 3** — ui/ (Navigation, Screens, ViewModels, Composables)
- [ ] **Шаг 4** — WorkManager + AlarmManager (уведомления)
- [ ] **Шаг 5** — Firebase подключение + инструкции по запуску
```
