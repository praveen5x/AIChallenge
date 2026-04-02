# Emoji Dodge

## Project overview

**Emoji Dodge** is a small arcade game for Android. The player moves a character along the bottom of the screen and avoids emojis that fall from the top. The run ends on collision; the score reflects survival time and emojis successfully dodged.

## Technology stack

- **Language:** Kotlin  
- **UI:** Jetpack Compose (Material 3)  
- **Min SDK:** 24  
- **Target / compile SDK:** 36  

No backend, network, or API keys are required.

## How to run

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended) with Android SDK and a JDK bundled by the IDE.
2. Open the **`project-source`** folder in Android Studio as an existing project (File → Open → select `project-source`).
3. Let Gradle sync finish. If `gradle-wrapper.jar` is missing, use Android Studio’s Gradle wrapper repair or run **File → Sync Project with Gradle Files**.
4. Create or select an **Android Virtual Device** (AVD) or connect a physical device with USB debugging.
5. Click **Run** (▶) with the `app` configuration.

Alternatively, from `project-source` in a terminal (with a proper JDK on `PATH`):

```text
gradlew assembleDebug
```

Install the generated APK from `app/build/outputs/apk/debug/` on a device or emulator.

## Development approach

- **Screens:** A small app state machine switches between Home, Playing, and Game Over.
- **Game loop:** `LaunchedEffect` + `withFrameNanos` drives frame updates, emoji movement, spawning, and collision checks.
- **Input:** Horizontal drag updates the player’s X position (clamped to the screen).
- **Scoring:** Score increases with elapsed time and with each emoji that leaves the bottom of the screen without hitting the player.
- **Difficulty:** Spawn interval shortens and fall speed increases slightly as the run continues.

Creative touches include varied emoji obstacles, a soft surface background, and readable score typography.
