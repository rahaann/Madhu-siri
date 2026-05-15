# MadhuSiri

**MadhuSiri** is an Android app built for beekeepers and farmers to improve hive safety, communication, and pesticide awareness.

It combines:

- Google Maps hive location tracking
- spray alert messaging for nearby beekeepers
- an AI-powered assistant
- Firebase-backed app state and data sync
- modern Jetpack Compose UI

---

## 🚀 What this app does

- Beekeepers can register hive locations and monitor nearby pesticide spraying.
- Farmers can create spray alerts with location and timing details.
- Nearby beekeepers receive alerts so they can protect their colonies.
- The app provides a health-focused dashboard and assistant tools.

---

## ✨ Key features

- **Hive Map**: see hive pins on Google Maps
- **Spray Alerts**: notify nearby beekeepers before pesticide spraying
- **AI Assistant**: contextual tips and smart recommendations
- **Health Dashboard**: track hive and farm status
- **Firebase Integration**: cloud-backed persistence and notifications
- **Jetpack Compose UI**: clean, modern Android design

---

## 🧩 Project structure

```text
MadhuSiri-main/
├── app/
│   ├── src/main/java/com/example/madhusiri/
│   ├── src/main/res/
│   ├── src/main/AndroidManifest.xml
│   └── google-services.json  # not included in repo
├── gradle/
├── gradlew
├── gradlew.bat
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🛠️ Setup

1. Clone the repo:

```bash
git clone https://github.com/rahaann/Madhu-siri.git
```

2. Open the project in Android Studio.
3. Let Gradle sync.
4. Place `google-services.json` inside `app/`.
5. Add your Google Maps API key and Firebase configuration if required.

---

## ▶️ Run the app

- Open `Tools > AVD Manager` and start an emulator, or connect a physical Android device with USB debugging enabled.
- Press the green `Run` button in Android Studio.

---

## ⚠️ Important notes

- Do not commit private API keys or sensitive Firebase files.
- `local.properties` is ignored by Git and should contain your Android SDK path.
- `.idea/` and `.codex/` files are excluded from the repo.

---

## 📌 Tips for contributors

- Keep `google-services.json` local.
- Use a separate Firebase project for testing.
- Add custom Hive and alert data only through the app UI.

---

## 👤 Author

Muhammed Rahan

---

## 📄 License

This repository is intended for project development and learning. Adjust the license as needed for your release.
