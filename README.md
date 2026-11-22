# Tickit

An Android habit tracker you can't ignore. Reminders ring like a real alarm, every tick needs proof (a note and optionally a photo), and when you skip a day you have to write down why. Those reasons go up on the **Wall of Shame**.

Inspired by the TickOff habit tracker, rebuilt with Kotlin and Jetpack Compose using MVVM and clean architecture. The app's UI is in Indonesian.

## Screenshots

| Today (tiles) | Compact, last 7 days | Habit detail | Proof journal |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/home.png" width="200"> | <img src="docs/screenshots/compact.png" width="200"> | <img src="docs/screenshots/detail.png" width="200"> | <img src="docs/screenshots/journal.png" width="200"> |
| **Adding proof** | **Alarm** | **Wall of Shame** | |
| <img src="docs/screenshots/proof.png" width="200"> | <img src="docs/screenshots/alarm.png" width="200"> | <img src="docs/screenshots/wall-of-shame.png" width="200"> | |

The screenshots use sample data.

## Features

- **GitHub-style progress tiles.** Each habit gets a contribution grid: one column per week, Monday at the top, the current week on the right. A tile in the habit's colour means done, red means skipped with a reason, dark means missed. Today is outlined until you deal with it.
- **Two layouts.** Tiles show months of history; compact shows every habit across the last seven days. Switch with the icon in the top right.
- **Every tick needs proof.** A note on what you did is required; a camera photo is optional. Undoing a tick deletes its note and photo, after a confirmation.
- **Habit detail.** Current streak, longest streak, consistency since the habit was created, a large tile grid with a legend, and a journal of every proof and excuse, newest first.
- **Reminders are alarms.** Looping alarm sound, vibration, screen wake-up, and a full-screen alarm over the lock screen until you act. If the phone is in use, it shows as a heads-up notification instead.
- **Snooze.** 10 minutes, at most 3 times. After that the only options are proof or a reason.
- **No progress means a reason.** Reasons go to the Wall of Shame, and deleting a habit doesn't remove them.
- **End-of-day review.** An alarm at a time you choose (default 21:00) listing whatever is still open. It only rings if something is.
- **Alarms that survive.** Habits already dealt with stay quiet. Alarms are rescheduled after a reboot, a clock or time zone change, an app update, or a permission change.

## Proof photos: why not Firebase

Since 3 February 2026, [Cloud Storage for Firebase](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024) requires the **Blaze** pay-as-you-go plan, which means attaching a billing account. A no-cost quota still exists on Blaze, but the free Spark plan has no Storage access at all.

Tickit is fully offline, so photos are kept in the **app's private storage on the phone**. That's free and needs no account. The trade-offs:

- Photos are shrunk automatically to about 1600 px on the long edge at JPEG quality 82. A 3–5 MB camera shot becomes roughly 200–400 KB, and the EXIF rotation is kept.
- Photos are **excluded from Android backup**, because Auto Backup stops at 25 MB and photos would push the database out. Habits, notes and reasons are still backed up. Photos are lost if you uninstall the app or move to a new phone.

## Design

The concept is a **blackboard**: a slate background, chalk-coloured text, a pastel chalk colour per habit, and red pen for excuses. The tile grid is the one loud element; everything else stays quiet.

| Token | Hex | Used for |
|---|---|---|
| Board | `#17201C` | background |
| Slate | `#212C27` | dialogs, navigation |
| Chalk | `#EDEBE3` | primary text |
| Dust | `#8C978F` | secondary text |
| Alarm | `#FFB547` | primary actions, alarm clock |
| Red pen | `#FF6B5E` | excuses, Wall of Shame |

Type: **Big Shoulders Display**, a condensed scoreboard face, for dates, habit names, numbers and the alarm clock; the system's Roboto for body text. The font is licensed under SIL OFL 1.1, see [`licenses/`](licenses/).

## Spec

| | |
|---|---|
| Language / UI | Kotlin 2.2, Jetpack Compose, Material 3 (custom dark theme) |
| Architecture | MVVM + clean architecture (domain / data / presentation), one `UiState` per screen, manual DI |
| Storage | Room 2.7 (SQLite), SharedPreferences for settings, private files for photos |
| Images | Coil 3 (`AsyncImage`) |
| Alarms | `AlarmManager.setAlarmClock` (exact, Doze-proof), `CATEGORY_ALARM` notification with `FLAG_INSISTENT` and a full-screen intent |
| Camera | `ActivityResultContracts.TakePicture` + `FileProvider`, no `CAMERA` permission |
| Min / target SDK | 27 (Android 8.1) / 35 (Android 15) |
| Build | AGP 8.11, Gradle 8.14 (wrapper), JDK 17+ (tested on JDK 21) |
| Application ID | `io.github.firmanains.tickit` |

### Layout

```
app/src/main/java/io/github/firmanains/tickit/
├── domain/                 # pure Kotlin, no Android
│   ├── model/              # Habit, CheckIn, Proof, HabitToday, HabitDetail, DayStatus, ShameEntry
│   ├── repository/         # HabitRepository, SettingsRepository, PhotoStore, ReminderScheduler interfaces
│   ├── usecase/            # ObserveToday, ObserveHabitDetail, AddHabit, DeleteHabit, CompleteHabit,
│   │                       # UndoCheckIn, Excuse, Snooze, RescheduleAll, SetEndOfDay
│   └── Schedule.kt         # nextTrigger, streak, weekColumns, dayStatus, stats, AlarmPolicy
├── data/
│   ├── local/              # Room entities, DAO, database
│   ├── photo/              # LocalPhotoStore: new path, shrink, delete
│   └── repository/         # RoomHabitRepository (also cleans up replaced photos), PrefsSettingsRepository
├── alarm/                  # platform side: scheduler, notifier, receivers, PendingIntents
├── di/AppContainer.kt      # dependency wiring + ViewModel factories
└── presentation/
    ├── home/               # Today: tiles / compact, permission banners
    ├── habit/              # detail: stats, large tiles, journal
    ├── shame/              # Wall of Shame
    ├── alarm/              # AlarmActivity (full-screen)
    ├── common/             # TileGrid, ProofDialog, ReasonDialog, time picker
    └── Theme.kt            # colour tokens + typography
```

Dependencies point `presentation → domain ← data / alarm`. The domain layer knows nothing about Android.

### Alarm flow

```
AlarmManager.setAlarmClock ──► AlarmReceiver (RING)
                                 ├─ schedule tomorrow's alarm
                                 ├─ already dealt with? → stay quiet
                                 └─ still open → alarm notification (looping, full-screen intent)
                                              │
                                 AlarmActivity / notification buttons
                                  ├─ Done, add proof → note (+ photo) → alarm stops
                                  ├─ Can't do it     → reason → Wall of Shame → alarm stops
                                  └─ Snooze          → RING_AGAIN in 10 min (max 3x)
```

Left alone, an alarm goes quiet after 10 minutes. The habit still shows up in the end-of-day review.

### Data

- `habits(id, name, reminderMinute?, colorIndex, createdEpochDay, archived)`: deleting is a soft delete (`archived = 1`)
- `check_ins(habitId, epochDay, done, reason?, note?, photoPath?)`: one row per habit per day. `done = 1` holds the proof (`note`, `photoPath`); `done = 0` holds the `reason` and appears on the Wall of Shame.

## Build and install

```bash
# needs the Android SDK (platform 35) and JDK 17+
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
./gradlew testDebugUnitTest assembleDebug

# phone with USB debugging enabled
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Permissions

| Permission | Android 13 | Android 14+ |
|---|---|---|
| Notifications | requested from a banner on the home screen | same |
| Exact alarms | granted automatically | must be allowed manually (banner → Settings) |
| Full-screen alarms | granted automatically | must be allowed manually (banner → Settings) |

The camera needs no permission: the system camera app takes the photo and writes it straight into Tickit's private storage.

**Xiaomi / Redmi / POCO (MIUI/HyperOS)** and other phones with aggressive battery management: open the app info and turn on **Autostart**, set **Battery saver → No restrictions**, and enable **Show on lock screen** and **Display pop-up windows while running in background**.

## Tests

- Unit tests (`./gradlew testDebugUnitTest`): next alarm time, streaks, weekly tile layout, per-day status, stats (longest streak, consistency), and the rule that a tick needs a note.
- Checked by hand on an Android 15 emulator: tiles and compact layout, ticking with a camera photo, the journal showing that photo, the end-of-day alarm from a locked screen, and adding proof from the alarm screen until the alarm stops.

## For AI agents

[`AGENTS.md`](AGENTS.md) describes this project's conventions for AI coding agents: the architecture, the deliberate choices, and how to verify changes. The Android skills come from [awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills), plus the `frontend-design` skill for visual direction. Skills and MCP config live locally in `.claude/` and aren't committed, because they contain machine-specific paths.

## Not built yet

- Editing a habit (for now: delete and add it again)
- Per-weekday schedules (every habit is daily)
- Adding proof for a past day
- Missed days without a tick or reason aren't added to the Wall of Shame automatically
- Cloud sync or backup for photos
- UI text is hardcoded in Indonesian (no `strings.xml` yet)
