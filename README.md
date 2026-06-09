# StickerPaster — Floating Notepad Overlay for Android

StickerPaster is a floating overlay notepad that lives on top of your other apps. Whether you need to quickly jot down a thought, keep a running to do list visible while browsing, or manage multiple notes side by side, StickerPaster keeps your notes right where you need them.

---

## What it does

- **Floating overlay** — your note stays visible on top of all apps. Read, write, or just glance at your note without switching away from what you're doing.
- **Multiple tabs** — create separate notes for different topics. Each tab keeps its own content and title.
- **Named tabs** — tabs start with default names like "Note 1", "Note 2". Long press any tab to give it a custom name.
- **Drag anywhere** — grab the handle bar or the top strip and drag the note to any corner of your screen. No restrictions, full freedom.
- **Resize to fit** — pull the bottom right corner handle to make the note bigger or smaller.
- **Collapse to a tab** — tap the handle to collapse the note into a slim tab on the edge of the screen. Tap again to bring it back.
- **Auto save** — every keystroke is saved automatically after a short pause. Your notes persist across reboots.
- **Bullet mode** — toggle bullet mode using the bullet button in the toolbar. Each new line starts with a bullet automatically.
- **Tab key support** — press the physical Tab key or the toolbar tab button to indent (four spaces).
- **Keyboard aware** — tap the text area to start typing. Tap outside or close to dismiss the keyboard and let touches pass through to the app behind.

---

## Getting started

### Requirements

An Android device running **Android 8.0 (API 26)** or newer.

### Install

Download the latest APK from the [Releases](https://github.com/trinnode/StickerApp/releases) page and sideload it.

Or via ADB:

```
adb install StickerPaster.apk
```

### First launch

1. The app will ask for **overlay permission** — this lets the note float on top of other apps.
2. On Android 13+ it also asks for **notification permission** — this is needed to keep the overlay service running in the background.
3. Once granted, the note appears on your screen. Start typing.

---

## Building from source

### Requirements

- JDK 17 or newer
- Android SDK (platform 34, build tools 34.0.0)
- Gradle 8.5 (wrapper included)

### Build steps

```
git clone https://github.com/trinnode/StickerApp.git
cd StickerApp
export ANDROID_HOME=/path/to/your/sdk
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## How it works under the hood

| Component | What it uses |
|---|---|
| Overlay window | `WindowManager.LayoutParams` with `TYPE_APPLICATION_OVERLAY` |
| User interface | Native Android views (FrameLayout, EditText, ImageButton, etc.) |
| Persistence | Jetpack DataStore Preferences with JSON serialization |
| Background service | Foreground Service (type `specialUse`) with a low priority notification |
| Permissions | `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS` |
| Minimum SDK | 26 |
| Target SDK | 34 |

The overlay uses `FLAG_NOT_FOCUSABLE` by default so touches pass through to whatever app is underneath. When you tap the text area, focus mode switches on to allow keyboard input. Tapping outside or pressing close switches it back off.

---

## Project structure

```
StickerApp/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/stickynote/overlay/
│   │   │   ├── MainActivity.kt          — permission flow and service launch
│   │   │   ├── StickerService.kt        — foreground service lifecycle
│   │   │   ├── OverlayManager.kt        — window management (add, move, resize, remove)
│   │   │   ├── StickerNoteUI.kt         — the full note UI with tabs, toolbar, drag, resize
│   │   │   └── NoteRepository.kt        — DataStore backed persistence with JSON
│   │   └── res/
│   │       ├── values/strings.xml
│   │       ├── drawable/                — icons and vectors
│   │       └── mipmap-anydpi-v26/       — adaptive app icon
│   ├── build.gradle.kts
│   └── remaining gradle files
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

---

## Contributing

Open an issue or send a pull request over on GitHub. All ideas and fixes are welcome.

---

Built with plain Android Views and Kotlin.
