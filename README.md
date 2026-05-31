# 📱 TodoApp Android

Приложение «Список дел» на Kotlin с облачной синхронизацией.

**Стек:** Kotlin · Jetpack Compose · Material 3 · MVVM · Room · Firebase Firestore · Hilt · WorkManager

---

## Возможности

- ✅ Создание, редактирование, удаление задач
- 📋 Подзадачи с чекбоксами
- ⭐ Пометка важных задач + фильтрация
- 📅 Дедлайны с DatePicker и TimePicker
- 🔔 Уведомление за 2 часа до дедлайна
- ☁️ Синхронизация с Firebase Firestore
- 📶 Работает офлайн (данные кэшируются в Room)
- 🔴 Просроченные задачи выделяются на отдельном экране

---

## Требования

| Инструмент | Версия |
|---|---|
| Android Studio | Hedgehog 2023.1.1 или новее |
| JDK | 17 (встроен в Android Studio) |
| Android SDK | API 35 |
| Устройство / Эмулятор | Android 8.0+ (API 26+) |

---

## Установка и запуск

### Шаг 1 — Клонируй репозиторий

```bash
git clone https://github.com/goshishhhhh/TodoApp-Android.git
cd TodoApp-Android
```

---

### Шаг 2 — Создай Firebase проект

1. Открой [console.firebase.google.com](https://console.firebase.google.com)
2. Нажми **Add project** → введи название → **Create project**
3. В проекте нажми иконку **Android** (`</>`)
4. Заполни:
   - **Package name:** `com.example.todoapp`
   - **App nickname:** TodoApp
5. Нажми **Register app**
6. Скачай **`google-services.json`**
7. Положи файл в папку **`app/`** проекта:
   ```
   TodoApp-Android/
   └── app/
       └── google-services.json   ← сюда
   ```
8. Нажми **Next → Next → Continue to console**

---

### Шаг 3 — Включи Firestore

1. В Firebase Console → **Databases & Storage → Firestore**
2. Нажми **Create database**
3. Выбери **Start in test mode**
4. Выбери регион (например `europe-west3`) → **Create**

---

### Шаг 4 — Включи Anonymous Authentication

1. В Firebase Console → **Security → Authentication**
2. Нажми **Get started**
3. Вкладка **Sign-in method** → **Anonymous**
4. Включи переключатель → **Save**

---

### Шаг 5 — Открой проект в Android Studio

1. **File → Open** → выбери папку `TodoApp-Android`
2. Дождись завершения **Gradle sync** (5–10 минут, первый раз)

> ⚠️ Если появится «Android SDK path not specified» — укажи путь к SDK:
> - Windows: `C:\Users\ИМЯ\AppData\Local\Android\Sdk`
> - Mac: `~/Library/Android/sdk`
> - Linux: `~/Android/Sdk`

---

### Шаг 6 — Запусти приложение

**На эмуляторе:**
1. В Android Studio → **Device Manager** → **Create device**
2. Выбери Pixel 6 → API 35 → **Finish**
3. Нажми кнопку **Run ▶**

**На физическом устройстве:**
1. На телефоне включи **Режим разработчика** и **USB отладку**
2. Подключи кабелем к компьютеру
3. Нажми **Run ▶** → выбери устройство

---

## Структура проекта

```
app/src/main/java/com/example/todoapp/
│
├── data/
│   ├── model/          — Task, Subtask, SyncStatus
│   ├── local/          — Room: Entity, DAO, Database
│   ├── remote/         — FirestoreService
│   └── repository/     — TaskRepository (offline-first)
│
├── domain/
│   └── usecase/        — Add/Update/Delete/Get TaskUseCase
│
├── ui/
│   ├── theme/          — Цвета, типографика, Material3 тема
│   ├── navigation/     — Навигация + нижняя панель
│   ├── components/     — TaskCard, SubtaskItem, PriorityBadge
│   └── screens/
│       ├── tasklist/   — Список задач + фильтр
│       ├── taskdetail/ — Создание и редактирование
│       └── deadlines/  — Экран дедлайнов
│
├── worker/
│   ├── AlarmManagerScheduler  — Точные будильники
│   ├── AlarmReceiver          — Обработчик будильника
│   ├── DeadlineNotificationWorker — Показ уведомления
│   ├── BootReceiver           — Восстановление после перезагрузки
│   └── SyncWorker             — Фоновая синхронизация
│
└── di/                 — Hilt модули (DI)
```

---

## Частые проблемы

| Проблема | Решение |
|---|---|
| `google-services.json not found` | Убедись, что файл лежит в папке `app/`, не в корне |
| Gradle sync завис | Подожди, первый раз качает ~400 MB зависимостей |
| `Build failed: KSP error` | **Build → Clean Project** → **Build → Rebuild Project** |
| Уведомления не приходят (Android 12+) | **Настройки → Приложения → TodoApp → Особые разрешения → Будильники** → включить |
| Firestore не синхронизирует | Проверь что Anonymous Auth включён в Firebase Console |

---

## Разработка

Сборка debug APK из терминала:
```bash
# Windows
.\gradlew.bat assembleDebug

# Mac / Linux
./gradlew assembleDebug
```

APK будет в:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Технологии

| Библиотека | Версия | Назначение |
|---|---|---|
| Kotlin | 2.0.21 | Язык |
| Jetpack Compose BOM | 2024.12.01 | UI |
| Material3 | (BOM) | Дизайн-система |
| Navigation Compose | 2.8.4 | Навигация |
| Room | 2.6.1 | Локальная БД |
| Hilt | 2.52 | Dependency Injection |
| Firebase BOM | 33.5.1 | Облако |
| WorkManager | 2.9.1 | Фоновые задачи |
| Coroutines | 1.9.0 | Асинхронность |
