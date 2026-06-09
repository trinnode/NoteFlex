# NoteFlex

A floating notepad that stays on top of your other apps. Write quick notes, keep a to do list visible while you browse, or manage multiple notes without switching windows.

## Features

- Lives on top of everything. Read and write without leaving the app you are in.
- Multiple tabs, each with its own name and content. Long press a tab to rename it.
- Drag the handle or the top bar to move the note anywhere on screen.
- Resize from the bottom right corner.
- Tap the handle to collapse the note into a slim strip. Tap again to expand it back.
- Bullet mode. Tap the bullet button and every new line starts with a bullet.
- Tab key works. Press the hardware Tab key or the tab button to indent.
- Everything saves automatically after a short pause. Your notes stay after closing the app.

## Install

Download the APK from the [Releases](https://github.com/trinnode/NoteFlex/releases) page and sideload it. Or use ADB:

```
adb install NoteFlex.apk
```

You need Android 8.0 or newer.

## First launch

The app asks for overlay permission so it can draw on top of other apps. On Android 13 and up it also asks for notification permission, which keeps the note alive in the background. Once both are granted the note appears and you can start typing.

## Building

```
git clone https://github.com/trinnode/NoteFlex.git
cd NoteFlex
export ANDROID_HOME=/path/to/your/sdk
./gradlew assembleDebug
```

The APK ends up at `app/build/outputs/apk/debug/app-debug.apk`.

JDK 17 or newer works. Android SDK platform 34 and build tools 34.0.0 are expected.

## How it works

The overlay is a regular Android `FrameLayout` added through `WindowManager` with `TYPE_APPLICATION_OVERLAY`. Touches pass through by default because of `FLAG_NOT_FOCUSABLE`. When you tap the text area the flag is removed so the keyboard can appear, and when you tap outside it goes back.

Notes are stored with Jetpack DataStore as JSON. The service runs in the foreground with a low priority notification so Android does not kill it.

## Files

```
app/src/main/java/com/noteflex/overlay/
  MainActivity.kt        permissions
  NoteFlexService.kt     foreground service
  OverlayManager.kt      window management
  NoteFlexUI.kt          the overlay itself
  NoteRepository.kt      data persistence
```

Built with Kotlin and plain Android Views.
