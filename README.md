# SaveBox

**Small savings. Bigger possibilities.**

SaveBox is a free, offline Android app for tracking money you save toward personal goals
(a new phone, a laptop, travel, an emergency fund...). You record your savings with
denomination boxes (for example ₹500 × 110) or custom amounts, and SaveBox shows your progress.

> SaveBox does **not** hold, move or verify money. It only records amounts you enter.
> It does not need the internet, an account, or a server. All data stays on your phone.

> **Status:** Version 1.0.0 - all six development phases are complete and tested.
> See [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) to understand and change the code.

## Features

- **Goals**: create, edit and delete goals with a name, emoji, target, optional deadline,
  description and starting amount. Example goals to get started quickly.
- **Dashboard**: total saved, total target, remaining, overall %, active and completed goals,
  recently updated goals, Quick add.
- **Goal details**: progress ring, saved / remaining / target, deadline countdown, savings
  breakdown (boxes vs. added by hand), extra savings above the target, savings history.
- **Denomination boxes**: build a grid such as ₹500 × 110, ₹200 × 60, ₹100 × 30. Tap a box to
  record that amount; tap again to remove it (with Undo). Shows total, ticked and remaining boxes,
  and how the grid compares with the goal's target. Rows can be resized or deleted.
- **Custom savings**: any amount, date (not in the future), optional source and note,
  quick-amount chips.
- **History**: every entry grouped by month with monthly totals, a filter per goal, totals by
  type, and a details sheet to edit or delete (with Undo).
- **Settings**: light / dark / system theme, currency (₹, $, €, £, AED - display only, no
  conversion), about and privacy information, app version.
- **Reminders**: optional daily or weekly reminder at a time you choose. Skipped automatically
  if you already saved today / this week. Optional milestone notifications at 25/50/75/100%.
- **Backup & restore**: save everything to a JSON file wherever you choose; restore replaces all
  data after a confirmation (all-or-nothing, a damaged file changes nothing).
- **Export**: savings history as a CSV file for Excel / Google Sheets.
- **Home-screen widget**: total saved, one goal's progress and remaining amount; tap to open
  that goal. Updates automatically. Choose the goal in Settings (or leave it on Automatic).
- **Offline and private**: no account, no internet permission, data stays on the phone.

### Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Reminders and milestone notifications (Android 13+ asks you first) |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE` | Added automatically by WorkManager so reminders survive restarts. None of these give internet access. |

There is **no `INTERNET` permission**, so SaveBox cannot send data anywhere.

---

## Technology stack

| Area | Library |
|---|---|
| Language | Kotlin 2.4 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Navigation | Navigation Compose (type-safe routes) |
| Database | Room |
| Preferences | DataStore |
| Reminders | WorkManager |
| Widget | Jetpack Glance |
| Backup files | kotlinx.serialization (JSON) |
| Tests | JUnit 4, Room in-memory tests, Compose UI test |
| Build | Gradle 9.6, Android Gradle Plugin 9.5, version catalog |

Minimum Android version: **8.0 (API 26)**. Target/compile SDK: 37.

All library versions are in **one file**: `gradle/libs.versions.toml`.

---

## Folder structure

```
SaveBox/
├── app/
│   ├── build.gradle.kts            ← app settings + dependencies
│   ├── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/savebox/app/
│       │   │   ├── MainActivity.kt          ← app entry point
│       │   │   ├── SaveBoxApplication.kt    ← runs once at app start
│       │   │   ├── AppContainer.kt          ← builds database, repositories, helpers
│       │   │   ├── AppDataObserver.kt       ← milestones, widget refresh, currency
│       │   │   ├── data/        database, entities, DAOs, repositories, preferences, backup
│       │   │   ├── domain/      models, calculations, validation, reminder timing
│       │   │   ├── navigation/  routes, NavHost, bottom-bar tabs, deep links
│       │   │   ├── notifications/ reminders (WorkManager) and notifications
│       │   │   ├── ui/
│       │   │   │   ├── SaveBoxApp.kt        ← root screen + bottom bar
│       │   │   │   ├── components/          ← reusable UI pieces
│       │   │   │   ├── theme/               ← colours, fonts, shapes, spacing
│       │   │   │   ├── dashboard/  goals/  denomination/
│       │   │   │   ├── transactions/  settings/  onboarding/
│       │   │   ├── widget/      home-screen widget (Glance)
│       │   │   └── utils/       MoneyFormatter, Currency, DateFormatter
│       │   └── res/             icons, strings, XML themes, widget info
│       ├── test/                unit tests (run on your PC)
│       └── androidTest/         database + UI tests (run on emulator/phone)
│   ├── schemas/                 Room database schema history (keep in git!)
│   └── proguard-rules.pro       release shrinking rules
├── keystore.properties.example  ← template for your release signing key
├── gradle/libs.versions.toml    ← ALL library versions
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── gradlew / gradlew.bat        ← Gradle wrapper (no Gradle install needed)
```

---

## Installation

1. Install **Android Studio** (it includes the JDK and Android SDK).
2. `File → Open` and choose the `SaveBox` folder.
3. Wait for "Gradle sync" to finish (first time downloads libraries, needs internet once).

## How to run the app

- **Android Studio:** pick an emulator or a USB-connected phone in the toolbar, press ▶ Run.
- **Command line** (from the `SaveBox` folder):

  ```bash
  ./gradlew installDebug
  ```

  If Gradle complains about Java, point `JAVA_HOME` at Android Studio's JDK first:

  ```bash
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
  ```

## How to run the tests

Unit tests (run on your PC, no phone needed):

```bash
./gradlew testDebugUnitTest
```

Results: `app/build/reports/tests/testDebugUnitTest/index.html`.

Device tests - database tests plus an end-to-end UI test that creates a goal, adds savings,
ticks a box, deletes the goal and opens every tab (start the emulator first):

```bash
./gradlew connectedDebugAndroidTest
```

If both a phone and an emulator are connected, the tests run on **all** of them. To use only the
emulator, set `ANDROID_SERIAL` first (the tests use their own temporary database, not your real data):

```bash
export ANDROID_SERIAL=emulator-5554
```

Results: `app/build/reports/androidTests/connected/debug/index.html`.

## How the database works

SaveBox uses **Room**, Android's official layer on top of SQLite. The database is one private
file on the phone (`savebox.db`). Nothing is sent anywhere.

| Table | One row is… | Linked to |
|---|---|---|
| `savings_goals` | a goal (name, emoji, target, deadline) | — |
| `savings_transactions` | one amount saved | its goal |
| `denominations` | a group of boxes, e.g. ₹500 × 110 | its goal |
| `denomination_entries` | one ticked box | its denomination **and** the transaction it created |

Important rules, enforced by the database itself:

- A goal's **saved amount is never stored**. It is always the sum of its transactions,
  so totals and history can't disagree.
- **Foreign keys with CASCADE**: deleting a goal deletes its transactions, denominations and ticked
  boxes. Deleting a box's transaction un-ticks the box.
- **Unique indexes**: the same box can't be ticked twice, and a goal can't have two "₹500" rows.
- **Ticking a box** creates a "Denomination box" transaction and links it to the box in one
  database transaction. **Unticking** deletes that transaction, so the history only ever shows
  money that is really counted. Undo puts the same record back (same id and time).
- Money is stored in **paise** (whole numbers).

Code: entities in `data/entities/`, SQL in `data/dao/`, database setup in `data/database/`,
conversion to app models in `data/repository/`. App settings (onboarding, later theme and
reminders) are stored separately with **DataStore** in `data/preferences/`.

**Changing the database later:** see "Changing the database (migrations)" in DEVELOPMENT_GUIDE.md.
Never delete `app/schemas/`.

## How to build an APK

### Debug APK (for testing)

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (~16 MB, not optimised).

### Release APK (to share or publish)

Release builds are shrunk and optimised by **R8** (~3 MB) and must be signed with **your own key**.

1. Create a key once (keep the file and passwords safe - you need them for every future update):

   ```bash
   keytool -genkeypair -v -keystore savebox-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias savebox
   ```

2. Copy `keystore.properties.example` to `keystore.properties` and fill in your passwords.
   Both `keystore.properties` and `*.jks` are in `.gitignore` - never commit them.
3. Build:

   ```bash
   ./gradlew assembleRelease
   ```

   Output: `app/build/outputs/apk/release/app-release.apk`.
4. For the Google Play Store, build an **App Bundle** instead:

   ```bash
   ./gradlew bundleRelease
   ```

   Output: `app/build/outputs/bundle/release/app-release.aab`.

Without `keystore.properties`, the release build is signed with the debug key so you can still
test it on your phone - but such an APK must **not** be published.

Before each release, increase `versionCode` (1, 2, 3...) and `versionName` ("1.0.1"...) in
`app/build.gradle.kts`.

### Release checklist

- [ ] `./gradlew testDebugUnitTest connectedDebugAndroidTest lintDebug` all pass
- [ ] Install the **release** APK and try: create goal, add savings, tick a box, History, Settings,
      backup + restore, widget (R8 problems only show up in release builds)
- [ ] `versionCode` / `versionName` increased
- [ ] Signed with your own key (not the debug key)

## Tested on

| Device (emulator) | Result |
|---|---|
| Phone 1080×2400, Android 16 (API 37), light + dark | ✅ all screens |
| Small phone 360×640 dp with 130% font size | ✅ text wraps, nothing cut off |
| Tablet 1280×800 dp landscape | ✅ content centred at a readable width |
| Release build (R8) + 9,000 random "monkey" taps | ✅ no crashes |

Older Android versions (8–12) were not tested on a device; the minimum supported version is Android 8.0.

---

## How to change colours and themes

- App colours: `app/src/main/java/com/savebox/app/ui/theme/Color.kt` (raw colours) and
  `Theme.kt` (which colour is used for which role, for light and dark mode).
- Fonts / text sizes: `ui/theme/Type.kt`
- Corner roundness: `ui/theme/Shape.kt`
- Spacing: `ui/theme/Spacing.kt`
- Launcher icon and splash colour: `res/values/colors.xml`, `res/drawable/ic_launcher_foreground.xml`

Screens use `MaterialTheme.colorScheme.primary` etc., never raw colours, so changing the
theme files updates the whole app.

## How money is stored

Every amount is stored as a whole number of **paise** in a `Long` (₹1 = 100 paise).
This avoids floating-point rounding mistakes. `utils/MoneyFormatter.kt` converts paise to
text like `₹1,00,000` and parses what the user types.

## How to add a new screen

1. Add a route in `navigation/Routes.kt`.
2. Create the screen composable in the right `ui/` folder.
3. Register it in `navigation/SaveBoxNavHost.kt` with `composable<YourRoute> { ... }`.
4. Navigate to it with `navController.navigate(YourRoute(...))`.

More detail (database, adding features, editing screens, reminders, widget, backup) is in
[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md).

## How to add a new feature (example: "savings streaks")

1. **Domain:** write the rule as plain Kotlin in `domain/` (e.g. `StreakCalculator.kt`) and unit-test it.
2. **Data:** if it needs stored data, add a DAO query (or a table + migration) and expose it in
   `SavingsRepository` / `RoomSavingsRepository`.
3. **ViewModel:** combine the repository Flows into a UI state.
4. **Screen:** show it using components from `ui/components/`.
5. **Test:** unit-test the rule; add a device test if it touches the database.

## Ideas for the future

Savings challenges, daily streaks, badges, goal-sharing cards, optional cloud backup and accounts,
a website version. The repository interface, the backup format version and Room migrations are
the places designed to make these possible without rewriting the app.

## License

Add a license of your choice (for example MIT) before publishing the source code.
