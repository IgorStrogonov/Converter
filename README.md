# Converter

Приложение-конвертер валют для Android (Java). Курсы валют берутся из открытого API
[open.er-api.com](https://www.exchangerate-api.com/docs/free), кэшируются локально в Room и
дополнительно синхронизируются с Firebase Realtime Database, чтобы у пользователя была история
конвертаций между устройствами.

## Возможности

- Конвертация RUB в EUR / USD / CNY / BYN по актуальному курсу
- Локальный кэш курсов (Room) — конвертер работает и без сети, если курсы уже когда-то загружались
- Авторизация через Firebase Auth (email/пароль) или анонимный доступ («Пропустить»)
- История конвертаций в Firebase Realtime Database, привязанная к аккаунту пользователя
- История загруженных курсов валют (Room) с датами обновления
- Firebase Analytics: события выбора валюты, конвертации, обновления курсов

## Стек

- Java, Android SDK (minSdk 24, target/compileSdk 36)
- Room (локальная БД)
- Firebase: Auth, Realtime Database, Analytics
- Gradle 9.1 / Android Gradle Plugin 9.0.1

## Сборка

```bash
./gradlew :app:assembleDebug
```

Для работы Firebase-функций (Auth/Realtime Database/Analytics) нужен свой `google-services.json`
от проекта в [Firebase Console](https://console.firebase.google.com/), положенный в `app/`.
Файл не хранится в репозитории (см. `.gitignore`) — у каждого, кто собирает проект, должен быть
свой.

## Известные ограничения

- Realtime Database Security Rules нужно настраивать отдельно в консоли Firebase — код пишет
  историю под `conversion_history/{uid}`, но сами правила доступа (кто может читать/писать чей uid)
  задаются на стороне Firebase, а не в этом репозитории.
- Донат-ссылка в `DonateActivity` — плейсхолдер, замените на свою при необходимости.
