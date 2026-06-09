# StickerPaster — Floating Sticky Note Overlay for Android

**StickerPaster** is a lightweight, floating overlay notepad that lives on top of your apps. Taking notes, jotting down ideas, or keeping a persistent to-do list has never been easier — your note is always just a tap away, no matter what app you're using.

---

## ✨ Features

- **📌 Floating Overlay** — Your note stays visible on top of all apps. Read, write, or glance at your note while browsing, watching videos, or working.
- **↕️ Drag & Move** — Grab the handle or the top bar and drag the note to any corner of your screen.
- **📐 Resizable** — Pull the bottom-right handle to resize the note to your liking.
- **📝 Bullet Mode** — Toggle bullet mode with the `•` button. Every new line starts with a bullet automatically.
- **↹ Tab Support** — Press the tab button or the physical Tab key to indent (inserts 4 spaces).
- **💾 Auto-Save** — Every keystroke is saved automatically after 300ms of inactivity. Your note is persisted across device reboots using DataStore.
- **➡️ Collapsible** — Tap the vertical handle to collapse the note into a slim tab; tap again to expand.
- **⌨️ Keyboard-Aware** — Tap the text area to type; tap outside or close to dismiss the keyboard and let touches pass through to apps behind.
- **🌙 Dark Theme** — Easy on the eyes with a near-black frosted card design.

---

## 📸 Behavior

- On first launch, the app requests **Overlay Permission** (`SYSTEM_ALERT_WINDOW`) and **Notification Permission** (Android 13+).
- Once granted, it starts a **Foreground Service** with a low-priority notification (so the system doesn't kill it).
- The overlay is displayed as a resizable card anchored to the right side of the screen.
- Tap the handle (`⋮`) to collapse/expand the note.
- Drag the handle or the top bar to reposition the overlay anywhere on screen.
- Pull the bottom-right resize handle to change width and height.
- The note text is persisted locally and restored the next time the app opens.

---

## 🚀 Installation

### Prerequisites
- Android device running **Android 8.0 (API 26)** or newer.
- "Install from unknown sources" enabled (for sideloading).

### Download
Grab the latest APK from the [Releases](https://github.com/trinnode/StickerApp/releases) page, or build it yourself.

### ADB Install
```bash
adb install StickerPaster.apk
```

---

## 🛠 Building from Source

### Requirements
- **JDK 17+**
- **Android SDK** (platform 34, build-tools 34.0.0)
- **Gradle 8.5** (wrapper included)

### Steps
```bash
git clone https://github.com/trinnode/StickerApp.git
cd StickerApp
export ANDROID_HOME=/path/to/your/sdk
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📁 Project Structure

```
StickerApp/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/stickynote/overlay/
│   │   │   ├── MainActivity.kt      — Permission flow & service entry
│   │   │   ├── StickerService.kt    — Foreground service lifecycle
│   │   │   ├── OverlayManager.kt    — WindowManager add/update/remove
│   │   │   ├── StickerNoteUI.kt     — Notepad UI with drag/resize/bullets
│   │   │   └── NoteRepository.kt    — DataStore persistence
│   │   └── res/
│   │       ├── values/strings.xml
│   │       ├── drawable/            — Icons & vectors
│   │       └── mipmap-anydpi-v26/   — Adaptive app icon
│   ├── build.gradle.kts
│   └── ...Gradle files...
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

---

## ⚙️ Technical Details

| Component | Implementation |
|---|---|
| **Overlay Window** | `WindowManager.LayoutParams` with `TYPE_APPLICATION_OVERLAY` |
| **UI** | Native Android Views (`FrameLayout`, `EditText`, `ImageButton`) |
| **Persistence** | Jetpack DataStore Preferences |
| **Service** | Foreground Service (`specialUse` type) |
| **Permissions** | `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS` |
| **Min / Target SDK** | 26 / 34 |

The overlay uses `FLAG_NOT_FOCUSABLE` by default so touches pass through to underlying apps. When you tap the text area, focus mode is toggled on to allow keyboard input, then toggled back off when you close or tap outside.

---

## 🤝 Contributing

Pull requests are welcome! If you find a bug or have a feature idea, open an issue or submit a PR.

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

Made with ☕ and Kotlin.
