# FinanceTracker 💰

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg?logo=kotlin)
![Android API](https://img.shields.io/badge/API-29%2B-brightgreen.svg?logo=android)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Enabled-blue.svg?logo=android)

**FinanceTracker** — это современное Android-приложение для учета личных финансов, написанное на Kotlin с использованием Jetpack Compose и архитектурных компонентов Jetpack.

## 🚀 Основные возможности

* **📊 Учет бюджета:** Удобное отслеживание доходов и расходов, визуализация данных с помощью графиков (Vico).
* **🧾 Сканирование чеков:** Встроенная поддержка камеры (CameraX) и распознавания штрихкодов (ML Kit) для быстрого добавления покупок из чеков.
* **📉 Калькулятор инфляции:** Специальный раздел для расчета и отслеживания влияния инфляции на ваши сбережения.
* **📱 Приветственный экран (Onboarding):** Понятное введение в функционал для новых пользователей.
* **⚙️ Настройки:** Гибкая настройка приложения под ваши нужды (включая безопасное хранение данных с Security Crypto).

## 🛠 Технологический стек

Приложение построено с использованием современных подходов и библиотек:
* **Язык:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Внедрение зависимостей (DI):** [Dagger Hilt](https://dagger.dev/hilt/)
* **Локальная база данных:** [Room](https://developer.android.com/training/data-storage/room)
* **Работа с сетью:** [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/)
* **Асинхронность:** Coroutines & Flow
* **Камера и машинное обучение:** [CameraX](https://developer.android.com/training/camerax) + [ML Kit](https://developers.google.com/ml-kit)
* **Графики:** [Vico](https://patrykandpatrick.com/vico)
* **Безопасность:** EncryptedSharedPreferences (Security Crypto)

## 🏗 Сборка и запуск проекта

Для сборки и запуска приложения вам понадобится [Android Studio](https://developer.android.com/studio).

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/your-username/FinanceTracker.git
   ```
2. Откройте проект в Android Studio (File -> Open...).
3. Дождитесь окончания синхронизации Gradle.
4. Убедитесь, что у вас выбран эмулятор с Android 10 (API 29) или выше, либо подключено физическое устройство.
5. Нажмите **Run** (`Shift + F10`), чтобы собрать и запустить приложение.

### Требования
* JDK 17
* Min SDK: 29
* Target SDK: 34

## 🤝 Вклад в развитие (Contributing)

Будем рады вашим Pull Request'ам! Если вы нашли ошибку или хотите предложить новую функцию, пожалуйста, создайте Issue для обсуждения перед началом работы.
