# SaveBox development guide

This guide explains how SaveBox is put together and which files to edit when you want
to change something. It is updated at the end of every development phase.

---

## 1. The big picture (MVVM)

```
 ┌──────────────┐  draws   ┌───────────────┐  asks for data  ┌──────────────────┐
 │   Screen     │ ───────▶ │   ViewModel   │ ──────────────▶ │   Repository     │
 │ (Compose UI) │ ◀─────── │ (screen state)│ ◀────────────── │ (where data lives)│
 └──────────────┘  state   └───────────────┘   Flow updates  └──────────────────┘
                                   │
                                   ▼
                       ┌────────────────────────┐
                       │ domain/                │
                       │  SavingsCalculator     │  ← all money maths
                       │  GoalValidator         │  ← all form rules
                       └────────────────────────┘
```

- **Screen** (`ui/...Screen.kt`): only draws things and reports clicks. No maths, no database.
- **ViewModel** (`ui/...ViewModel.kt`): holds what the screen shows, reacts to clicks,
  calls the repository. Survives screen rotation.
- **Repository** (`data/repository/`): the only code that knows *where* data is stored.
- **Domain** (`domain/`): plain Kotlin rules (calculations, validation, models). No Android code,
  so it is easy to unit test.

Data flows **one way**: repository → `Flow` → ViewModel → `uiState` → screen.
When data changes (for example a goal is deleted), every screen that watches it updates by itself.

## 2. How objects are created (AppContainer)

`SaveBoxApplication` creates one `AppContainer` when the app starts. The container opens the
database once and builds the repositories. `ui/AppViewModelProvider.kt` hands those repositories
to each ViewModel.

`SavingsRepository` (an interface) is implemented by `RoomSavingsRepository`.
An in-memory version existed in Phases 2–3; it was removed in Phase 4 because keeping a second
copy of the box/undo rules in sync would invite bugs. Those rules are tested on the real database
instead (`app/src/androidTest/`).

## 2a. The database layer

```
data/
├── entities/     one class per table (+ GoalWithTotals, a query result)
├── dao/          the SQL: GoalDao, TransactionDao, DenominationDao
├── database/     SaveBoxDatabase (version, list of tables) + Migrations.kt
├── repository/   RoomSavingsRepository + EntityMappers (entity ⇄ domain model)
└── preferences/  UserPreferencesRepository (DataStore: small settings)
```

Why settings are not a Room table: settings are a few single values (a switch, a theme), not a
list of records. DataStore is made for exactly that and needs no SQL or migrations.

Why `savedAmountPaise` is not a column: it is calculated in SQL
(`COALESCE(SUM(t.amount_paise), 0)` in `GoalDao`) every time, so it can never be out of date.

Multi-step writes use `database.withTransaction { ... }` ("all or nothing"). Creating a goal with
a starting amount is one example: either both rows are saved or neither is.

### Changing the database (migrations)

Users already have data on their phones, so a table change needs a migration:

1. Change the entity (e.g. add a field with a default value).
2. In `SaveBoxDatabase`, change `version = 1` to `version = 2`.
3. In `data/database/Migrations.kt`, write `MIGRATION_1_2` (there is an example in the comments)
   and add it to `ALL`.
4. Build. Room creates `app/schemas/.../2.json`. Keep the `app/schemas` folder in git.
5. Add a test with `MigrationTestHelper` (from `androidx.room:room-testing`, already a dependency)
   that creates a version-1 database, runs the migration and checks the data.

**Never** add `fallbackToDestructiveMigration()`: it would silently delete everyone's savings.

## 3. Money

- Stored as **paise** in a `Long` (₹1 = 100). Never `Double`/`Float`.
- Text → paise: `MoneyFormatter.parseToPaise("1,500.50")`
- Paise → text: `MoneyFormatter.format(150050)` → `₹1,500.50`
- Progress / remaining / totals: **always** use `SavingsCalculator`, never calculate in a screen.

Rules implemented in `SavingsCalculator`:

| Value | Rule |
|---|---|
| Progress % | saved ÷ target × 100, rounded down (can exceed 100) |
| Progress bar | same, but capped at 100% |
| Remaining | target − saved, never below ₹0 |
| Extra | saved − target when above target (kept, and shown on the goal) |
| Dashboard remaining | sum of each goal's remaining (extra in one goal doesn't help another) |
| Grid summary | boxes, ticked, remaining, saved with boxes, planned total (`summarizeDenominations`) |
| Boxes to cover | ⌈amount ÷ box value⌉ (`boxesToCover`), used for the "Use N boxes" suggestion |
| By type | boxes vs. custom vs. starting amount (`totalsByType`) |
| Monthly | totals per calendar month in the phone's time zone (`monthlySummary`) |

## 3a. How undo works

Undo uses one app-wide snackbar (`ui/components/AppSnackbar.kt`), provided by `SaveBoxApp` as
`LocalAppSnackbar`. A screen calls:

```kotlin
snackbar.showUndo("₹500 entry deleted") { viewModel.restore(deleted) }
```

- Deleting returns a `DeletedTransaction` (the record + which box it came from, if any).
- `restoreTransaction(deleted)` re-inserts the same record and re-ticks the box if it's still free.
- The undo functions on ViewModels are `suspend` and don't use `viewModelScope`, because the
  snackbar may outlive the screen (e.g. after "Add savings" closes).

## 3b. Denomination rules (in `RoomSavingsRepository`)

| Action | What happens (all-or-nothing) |
|---|---|
| Tick box | insert DENOMINATION transaction + insert box entry linked to it |
| Untick box | delete the box's transaction (CASCADE removes the entry) |
| Delete entry in History | same as untick: the box becomes empty |
| Edit a box's entry | only note, source and date change; amount and goal stay tied to the box |
| Change row quantity | refused if a ticked box would disappear |
| Delete row | its box transactions are deleted, then the row |
| Add row with an existing value | refused (`DuplicateValue`) |

## 3c. App-wide background helpers (Phase 5)

`SaveBoxApplication.onCreate()` creates notification channels and starts `AppDataObserver`, which
runs in `AppContainer.applicationScope` (lives as long as the app process):

| Job | How |
|---|---|
| Currency | copies the setting into `AppCurrency.current` (Compose state → every screen redraws) |
| Milestones | watches all goals; announces 25/50/75/100% once per goal (`Milestones.kt`); remembers what was announced in DataStore |
| Widget | on any goal or widget-setting change, waits 0.5 s then refreshes the widget |

## 3d. Reminders

- `notifications/ReminderScheduler.kt` turns settings into one **unique periodic WorkManager job**
  (daily or weekly). Changing the settings replaces the job; turning them off cancels it.
- `notifications/ReminderWorker.kt` runs at the reminder time and stays quiet when the user already
  saved today (daily) / in the last 7 days (weekly), has no active goals, or blocked notifications.
- The time maths is pure Kotlin in `domain/reminders/ReminderSchedule.kt` (unit tested).
- Notification texts: `domain/model/MotivationMessages.kt` (`reminderMessages`) and `Milestones.message()`.

## 3e. Backup, restore, export

- `data/backup/BackupModels.kt`: the JSON file format (separate from the database tables so old
  files keep working). Change the format → raise `CURRENT_FORMAT_VERSION`.
- `data/backup/BackupManager.kt`: create / check / restore. Restore = delete everything and insert
  the file's rows inside **one transaction**; any problem rolls everything back.
- `data/backup/CsvExporter.kt`: history → CSV (escaping rules are unit tested).
- Files are read/written with Android's file picker (`rememberLauncherForActivityResult` in
  `SettingsScreen.kt`) - no storage permission is needed.

## 3f. Home-screen widget

- `widget/SaveBoxWidget.kt` (Jetpack Glance). It **collects a Flow** of goals + settings inside
  `provideContent`, so changes are drawn even while Glance keeps the widget session open.
- `widget/WidgetData.kt` chooses which goal to show and formats the text (unit tested).
- `widget/SaveBoxWidgetReceiver.kt` + `res/xml/savebox_widget_info.xml` (size, preview) +
  the `<receiver>` in `AndroidManifest.xml`.
- Tapping opens `MainActivity` with `DeepLinks.EXTRA_GOAL_ID`; `MainViewModel.pendingGoalId` →
  `SaveBoxApp` navigates to that goal. Notifications use the same deep link.

## 3g. Currency

`utils/Currency.kt` lists currencies; `AppCurrency.current` is the chosen one. `MoneyFormatter.format()`
uses it automatically. Amounts are never converted - only the symbol and digit grouping change.

## 4. Where things are

| I want to change… | Edit this file |
|---|---|
| Colours (light/dark) | `ui/theme/Color.kt`, `ui/theme/Theme.kt` |
| Fonts / text sizes | `ui/theme/Type.kt` |
| Corner roundness | `ui/theme/Shape.kt` |
| Spacing | `ui/theme/Spacing.kt` |
| Example goals and emoji list | `ui/goals/GoalPresets.kt` |
| Quick-amount chips, source suggestions | `ui/transactions/AddSavingsViewModel.kt` (`QuickAmounts`, `SavingsSources`) |
| Note values offered for boxes (₹10…₹2000) | `ui/denomination/DenominationDialogs.kt` |
| Box size / columns | `ui/denomination/DenominationScreen.kt` (`GridCells.Adaptive`) |
| Max boxes per row, note length… | `domain/validation/SavingsValidator.kt` |
| Add / edit savings form | `ui/transactions/AddSavingsScreen.kt` |
| History list | `ui/transactions/HistoryScreen.kt` |
| Entry details sheet | `ui/components/TransactionDetailsSheet.kt` |
| Settings screen / About & Privacy text | `ui/settings/SettingsScreen.kt` |
| Default reminder time, any setting default | `data/preferences/UserPreferencesRepository.kt` (`UserPreferences`) |
| Currencies offered | `utils/Currency.kt` |
| Milestone percentages and texts | `domain/calculations/Milestones.kt` |
| Reminder messages | `domain/model/MotivationMessages.kt` |
| Widget look | `widget/SaveBoxWidget.kt` (`WidgetContent`) |
| Widget size / picker preview | `res/xml/savebox_widget_info.xml`, `res/layout/widget_preview.xml` |
| Notification icon | `res/drawable/ic_notification.xml` |
| Motivational messages | `domain/model/MotivationMessages.kt` |
| Form rules (max name length, max amount…) | `domain/validation/GoalValidator.kt` |
| Date format | `utils/DateFormatter.kt` |
| Onboarding text | `ui/onboarding/OnboardingScreen.kt` |
| Dashboard layout | `ui/dashboard/DashboardScreen.kt` |
| Goal form | `ui/goals/GoalEditorScreen.kt` (+ `GoalEditorViewModel.kt`) |
| Goal details layout | `ui/goals/GoalDetailsScreen.kt` |
| Bottom tabs | `navigation/TopLevelDestination.kt` |
| Screens and navigation | `navigation/Routes.kt`, `navigation/SaveBoxNavHost.kt` |

Reusable building blocks are in `ui/components/`: `GoalCard`, `SavingsProgressBar`,
`ProgressRing`, `EmojiBadge`, `StatItem`, `SectionHeader`, `AmountTextField`,
`SaveBoxTopBar`, `ConfirmDialog`, `EmptyState`, `TransactionRow`. Change one of these and
every screen that uses it changes too.

## 5. Recipe: adding a new screen

1. **Route**: add `@Serializable data class MyRoute(val goalId: Long)` to `navigation/Routes.kt`.
2. **ViewModel**: create `ui/<feature>/MyViewModel.kt`. Read arguments with
   `savedStateHandle.toRoute<MyRoute>()`.
3. **Register the ViewModel**: add an `initializer { ... }` block in `ui/AppViewModelProvider.kt`.
4. **Screen**: create `ui/<feature>/MyScreen.kt` and get the ViewModel with
   `viewModel(factory = AppViewModelProvider.Factory)`.
5. **Navigation**: add `composable<MyRoute> { MyScreen(...) }` in `navigation/SaveBoxNavHost.kt`.
6. **Navigate**: `navController.navigate(MyRoute(goalId = 1))`.

## 6. Recipe: adding a new setting

1. Add a key in `UserPreferencesRepository.Keys` and a field in `UserPreferences`.
2. Read it in the `map { }` block and add a `suspend fun setMySetting(...)`.
3. Never rename an existing key: users would lose that setting.

## 7. Tests

- **Unit tests** live in `app/src/test/` and run on your PC: `./gradlew testDebugUnitTest`
  (or right-click a test file in Android Studio → **Run**).
  Covered: `MoneyFormatter`, `SavingsCalculator` (zero, partial, complete, above target,
  multiple goals, deleted and edited transactions, unmarked boxes, grid totals, by type,
  monthly incl. time zones), `GoalValidator`, `SavingsValidator`.
- **Database tests** live in `app/src/androidTest/` and need an emulator or phone:
  `./gradlew connectedDebugAndroidTest`. They use a temporary database, never your real data.
  Covered: every goal field is saved and read back, totals are calculated by SQL, update keeps
  savings, delete cascades to transactions/denominations/boxes, a box can't be ticked twice,
  a denomination value can't be added twice to one goal, savings for a missing goal are rejected,
  and data survives closing and reopening the database file.
  Phase 4 (`SavingsOperationsTest`): add/edit/move/delete/undo savings, the PS5 grid, tick/untick,
  undo re-ticks the box, deleting a box entry unticks it, box entries keep their amount when edited,
  out-of-range boxes, duplicate rows, quantity limits, deleting a row.
  Phase 5 (`BackupManagerTest`): backup → restore reproduces everything, non-backup files and newer
  formats are rejected, a damaged file leaves existing data untouched.
- Phase 5 unit tests (`Phase5LogicTest`): milestones, reminder timing and skipping, CSV escaping
  and format, widget goal choice and formatting; `MoneyFormatterTest` covers other currencies.
- When you change a rule, change its test first, see it fail, then fix the code.

## 8. Phase log

| Phase | What was added |
|---|---|
| 1 | Gradle project, version catalog, theme, navigation shell, `MoneyFormatter` |
| 2 | Onboarding (saved in DataStore), dashboard, goal create/edit/delete, goal details, calculator, validator, in-memory repository |
| 3 | Room database (4 tables, foreign keys, unique indexes, schema export), `RoomSavingsRepository`, database tests |
| 4 | Denomination grid, add/edit savings, History with monthly groups and filters, details sheet, app-wide Undo, grid/by-type/monthly calculations, more tests |
| 5 | Settings (theme, currency, reminders, milestones, widget goal), WorkManager reminders, milestone notifications, JSON backup/restore, CSV export, Glance widget with deep link |
| 6 | Release build (R8 shrinking, signing via keystore.properties), end-to-end UI test, layouts for tablets and large fonts, monkey stress test, final docs |

## 9. Release build notes (Phase 6)

- `app/build.gradle.kts` → `buildTypes.release`: `isMinifyEnabled` + `isShrinkResources` (R8).
- `app/proguard-rules.pro`: keeps kotlinx.serialization serializers (navigation routes, backup
  format) and `ReminderWorker`. If a release build crashes but debug doesn't, a class used by
  reflection is probably being removed: add a `-keep` rule there.
- Signing: `keystore.properties` (not in git) → see README "Release APK".

## 10. Responsive layout

- `ui/SaveBoxApp.kt`: content is limited to `MAX_CONTENT_WIDTH` (840 dp) and centred on tablets.
- Buttons use `heightIn(min = …)` (not a fixed height) and `StatItem` allows two lines, so large
  font sizes wrap instead of being cut off. Keep doing this for new UI.
- The box grid uses `GridCells.Adaptive(48.dp)`: more columns on wider screens automatically.

## 11. UI test

`app/src/androidTest/.../ui/MainFlowTest.kt` drives the real app. When you rename a button or
label, update the text in this test too. The helpers `clickButton`, `typeInto` and `waitForText`
keep the test readable.
