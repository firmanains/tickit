# AGENTS.md

Guidance for AI coding agents working on Tickit. Read this before changing code.

## Project

- Android app, single `:app` module. Kotlin 2.2, AGP 8.11, Gradle 8.14 wrapper, minSdk 27, targetSdk 35.
- Run Gradle on JDK 17 or 21 (`JAVA_HOME=/opt/homebrew/opt/openjdk@21` on the maintainer's Mac). Newer JDKs and Gradle 9 break AGP 8.
- UI copy is Indonesian, sentence case, plain verbs. No all-caps labels, no "A · B · C" meta strings.

## Architecture

- Layers: `domain` (pure Kotlin: models, repository interfaces, use cases, pure functions in `Schedule.kt`), `data` (Room, SharedPreferences, photo files), `alarm` (AlarmManager, notifications, receivers), `presentation` (ViewModels + Compose). Dependencies point inward: `domain` imports nothing from Android.
- DI is manual in `di/AppContainer.kt`. This is deliberate: the graph is small. Don't add Hilt or Koin unless the graph outgrows one file.
- Navigation is state in `MainActivity` (two tabs plus a detail screen). Don't add a navigation library for one more screen.
- Each screen exposes one immutable `UiState` as a `StateFlow` (`stateIn(WhileSubscribed(5_000))`) and collects it with `collectAsStateWithLifecycle()`. Add a `SharedFlow` only for real one-off events.
- Use cases exist where logic is shared or orchestrates several pieces. Trivial reads can go straight to a repository interface.

## Data rules

- Room schema is version 2 (`MIGRATION_1_2` adds `habits.emoji`) and the app is installed on real phones. Any schema change needs a version bump and a `Migration` registered in `AppContainer`; never use destructive fallback. Verify on an emulator by installing over a database of the previous version.
- A check-in is one row per habit per day: `done = 1` holds proof (`note` required, `photoPath` optional); `done = 0` holds `reason` and is a Wall of Shame entry.
- Habits are soft-deleted (`archived = 1`) so the Wall of Shame keeps its history.
- `RoomHabitRepository` owns photo cleanup: replacing or clearing a check-in deletes the photo it no longer references. Photos live in `filesDir/proofs`, are shrunk on save, and are excluded from backup (`res/xml/*backup*`).
- File I/O goes through an injected `CoroutineDispatcher` (default `Dispatchers.IO`).

## Alarms

- Use `setAlarmClock` when exact alarms are allowed, `setAndAllowWhileIdle` otherwise.
- PendingIntents are told apart by action plus the data URI `tickit://target/<habitId|eod>`; extras don't count for equality.
- `RescheduleReceiver` restores every alarm after boot, a time or time zone change, an app update, or an exact-alarm permission change. Keep it working when you add alarm types.
- A tick needs proof, so notifications never tick directly; their "done" action opens `AlarmActivity`.

## UI

- Colours and type come from `presentation/Theme.kt`: `Chalk` tokens, `Paper` tokens (sticky notes, polaroids), `habitColors`, `habitEmojis`. Use `MaterialTheme` roles, not hard-coded colours, except for tile, red-pen and paper semantics.
- Three type roles: `Scoreboard` (Big Shoulders Display) for display styles, numbers and the alarm clock; `HandStyle` (Patrick Hand) **only** for words the user wrote (proof notes, excuses); system Roboto for everything else.
- Shared playful pieces live in `common/Playful.kt` (`EmojiBadge`, `ProgressRing`, `Polaroid`, `tiltFor`, `dashedOutline`). Reuse them instead of re-drawing.
- Motion answers user actions (ring fills, tick pop and burst). The alarm screen's emoji wiggle is the only motion that runs on its own; don't add idle animations elsewhere.
- Screens not inside a `Scaffold` must sit in a `Surface`, or text falls back to black on the dark board.
- README screenshots get new file names when they change; GitHub caches images by path.
- Accessibility: 48dp touch targets, a `contentDescription` that names the action, `semantics { heading() }` on screen titles.

## Verify

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

- New logic in `domain` gets a plain JUnit test (see `app/src/test`).
- UI or alarm changes: run on an emulator and look at screenshots. Navigate with `uiautomator dump` text and content descriptions, not guessed coordinates. To test alarms, turn off auto time and set the clock just before the alarm (`adb root`, `adb shell date MMDDhhmmYYYY.ss`).

## Skills

Local skills in `.claude/skills/` (not committed) come from [awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills), plus `frontend-design` for visual work. Where this file and a skill disagree, this file wins: it records the choices made for this app (manual DI, no navigation library, Kotlin 2.2 without explicit backing fields).
