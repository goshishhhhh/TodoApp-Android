# 🚀 TodoApp — Инструкция по запуску

## Предварительные требования

| Инструмент | Минимальная версия |
|---|---|
| Android Studio | Hedgehog 2023.1.1+ (или Iguana / Jellyfish) |
| JDK | 17 |
| Android SDK | API 35 (compileSdk) |
| Устройство/эмулятор | Android 8.0+ (minSdk 26) |

---

## ШАГ 1 — Открыть проект в Android Studio

1. Запустить **Android Studio**
2. **File → Open** → выбрать папку `андроид` (эта папка)
3. Дождаться завершения Gradle sync (первый раз ~5 минут, качает зависимости)
4. Если появится диалог «Trust project» → нажать **Trust Project**

---

## ШАГ 2 — Создать Firebase проект

### 2.1 Создание проекта

1. Открыть [https://console.firebase.google.com](https://console.firebase.google.com)
2. Нажать **Add project** / «Создать проект»
3. Название проекта: `TodoApp` (или любое)
4. Google Analytics: можно отключить
5. Нажать **Create project**

### 2.2 Подключение Android приложения

1. В Firebase Console нажать иконку **Android** (</>)
2. **Android package name**: `com.example.todoapp`
3. **App nickname**: TodoApp
4. **Debug signing certificate SHA-1**: получить командой:
   ```bash
   # Windows PowerShell:
   cd ~\.android
   keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   Скопировать строку `SHA1:` → вставить в поле
5. Нажать **Register app**
6. Нажать **Download google-services.json**
7. Переместить скачанный `google-services.json` в папку **`app/`**:
   ```
   андроид/
   └── app/
       └── google-services.json   ← сюда
   ```
8. Нажать **Next → Next → Continue to console**

### 2.3 Включить Firestore

1. В левом меню Firebase Console → **Firestore Database**
2. Нажать **Create database**
3. Выбрать **Start in test mode** (потом заменим на продакшн rules)
4. Выбрать регион (например `europe-west3` — Франкфурт)
5. Нажать **Done**

### 2.4 Включить Anonymous Authentication

1. В левом меню → **Authentication**
2. Нажать **Get started**
3. Вкладка **Sign-in method**
4. Нажать **Anonymous** → включить → **Save**

> Это позволяет пользователям использовать приложение без регистрации.
> Данные привязываются к анонимному UID.

### 2.5 Развернуть Firestore Rules (опционально, для продакшна)

```bash
# Установить Firebase CLI
npm install -g firebase-tools

# Авторизация
firebase login

# Инициализация (из папки проекта)
firebase use --add   # выбрать созданный проект

# Деплой правил
firebase deploy --only firestore:rules
```

---

## ШАГ 3 — Настройка разрешений на точные будильники (Android 12+)

На устройствах с Android 12 (API 31+) требуется ручное разрешение:

1. Установить приложение на устройство
2. **Настройки → Приложения → TodoApp → Особые разрешения → Будильники и напоминания**
3. Включить переключатель

Или в коде добавить переход к настройкам при первом запуске:
```kotlin
// В MainActivity.onCreate()
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val alarmManager = getSystemService(AlarmManager::class.java)
    if (!alarmManager.canScheduleExactAlarms()) {
        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
    }
}
```

---

## ШАГ 4 — Запуск приложения

### Debug-сборка (рекомендуется для разработки)

1. Подключить устройство или запустить эмулятор
2. В Android Studio нажать кнопку **Run ▶** (Shift+F10)
3. Выбрать устройство → OK

### Запуск через Gradle (терминал)

```bash
# из папки проекта
./gradlew installDebug

# запустить на устройстве
adb shell am start -n com.example.todoapp.debug/com.example.todoapp.MainActivity
```

---

## ШАГ 5 — Проверка функциональности

### Чеклист тестирования

- [ ] **Создание задачи**: нажать «+», ввести заголовок, сохранить
- [ ] **Важная задача**: нажать звёздочку в TopAppBar — задача отображается с красной меткой
- [ ] **Фильтр**: нажать звёздочку в списке задач — показываются только важные
- [ ] **Дедлайн**: установить дедлайн через DatePicker + TimePicker
- [ ] **Подзадачи**: добавить несколько подзадач, отметить выполненными
- [ ] **Свайп для удаления**: свайп влево по карточке задачи
- [ ] **Экран «Дедлайны»**: вкладка внизу — задачи сортированы по времени
- [ ] **Просроченные**: задача с прошедшим дедлайном выделяется оранжевым
- [ ] **Синхронизация**: создать задачу → проверить в Firebase Console → Firestore

### Проверка уведомлений

```bash
# Установить дедлайн через 2 часа 5 минут от текущего времени
# или протестировать через adb:

# Посмотреть запланированные алармы:
adb shell dumpsys alarm | grep com.example.todoapp

# Форсировать срабатывание (только debug):
adb shell am broadcast -a com.example.todoapp.TEST_ALARM \
  --es extra_task_id "test-id" \
  --es extra_task_title "Тестовая задача"
```

---

## Структура package после Шага 5

```
com.example.todoapp/
├── TodoApplication.kt          ← @HiltAndroidApp + WorkManager config
├── MainActivity.kt             ← единственная Activity
│
├── data/
│   ├── model/                  ← Task, Subtask, SyncStatus
│   ├── local/                  ← Room: Entity, DAO, Database
│   ├── remote/                 ← FirestoreService
│   └── repository/             ← TaskRepository (offline-first)
│
├── domain/
│   └── usecase/                ← Add/Update/Delete/Get TaskUseCase
│
├── ui/
│   ├── theme/                  ← Color, Type, Theme (Material3)
│   ├── navigation/             ← AppNavigation (NavHost + BottomBar)
│   ├── components/             ← TaskCard, SubtaskItem, PriorityBadge
│   └── screens/
│       ├── tasklist/           ← TaskListScreen + ViewModel
│       ├── taskdetail/         ← TaskDetailScreen + ViewModel
│       └── deadlines/          ← DeadlinesScreen + ViewModel
│
├── worker/
│   ├── NotificationScheduler.kt        ← интерфейс
│   ├── AlarmManagerScheduler.kt        ← реализация (setExactAndAllowWhileIdle)
│   ├── AlarmReceiver.kt                ← BroadcastReceiver → WorkManager
│   ├── DeadlineNotificationWorker.kt   ← @HiltWorker, показывает уведомление
│   ├── BootReceiver.kt                 ← восстановление алармов после reboot
│   └── SyncWorker.kt                   ← периодическая синхронизация Firestore
│
└── di/
    ├── AppModule.kt            ← Firebase instances
    ├── DatabaseModule.kt       ← Room
    ├── NotificationModule.kt   ← AlarmManager, NotificationManager
    └── WorkerModule.kt         ← биндинг NotificationScheduler
```

---

## Частые проблемы

| Проблема | Решение |
|---|---|
| `google-services.json not found` | Положить файл в `app/`, не в корень проекта |
| `Hilt component not found` | Убедиться, что `@HiltAndroidApp` есть в `TodoApplication`, класс указан в `AndroidManifest` как `android:name=".TodoApplication"` |
| `Room schema error` | Удалить `fallbackToDestructiveMigration()` в проде и добавить Migration |
| `WorkManager not initializing` | Проверить что `InitializationProvider` отключён в Manifest (мы уже сделали это) |
| `Exact alarm not working (API 31+)` | Выдать разрешение вручную (см. Шаг 3) |
| `Firestore permission denied` | Проверить Authentication → Anonymous sign-in включён |
| `Build fails: KSP error` | `Build → Clean Project`, затем `Build → Rebuild Project` |

---

## Следующие шаги (для продакшна)

1. **Замените** `applicationId` с `com.example.todoapp` на свой: `com.yourname.todoapp`
2. **Создайте** release keystore: `keytool -genkey -v -keystore release.jks`
3. **Настройте** Firestore Rules (файл `firestore.rules` уже подготовлен)
4. **Добавьте** Migration вместо `fallbackToDestructiveMigration()` в `DatabaseModule`
5. **Включите** ProGuard для release-сборки (`isMinifyEnabled = true` уже выставлен)
6. **Замените** `com.example` на свой пакет во всех файлах (Find → Replace All)
