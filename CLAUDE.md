@AGENTS.md

# Working notes for Claude

AGENTS.md covers stack, architecture and conventions. This file adds what you would otherwise
have to rediscover by reading code. Keep it short; update it when the facts below change.

## Fast commands (prefer these, they keep output small)

| Goal | Command |
|---|---|
| Unit tests (~8s warm) | `./gradlew testDebugUnitTest -q` |
| One test class | `./gradlew testDebugUnitTest -q --tests '*.ReminderScheduleTest'` |
| Which tests failed | `grep -l '<failure' app/build/test-results/testDebugUnitTest/*.xml` |
| Compile check only | `./gradlew compileDebugKotlin -q` |
| Instrumented tests (needs device) | `./gradlew connectedDebugAndroidTest -q` |

Ignore the `WARNING: The option setting 'android.*'` and JVM "Sharing is only supported" lines;
they are always printed and are not errors. Do not read `build/`, `releases/` (APKs) or `deps.txt`.

## Code map (`app/src/main/java/com/qspapps/remindermate/`)

- `data/model/Reminder.kt`: entity plus the occurrence logic: `getOccurrences(from, to)` and
  `getNextOccurrence(actions, after)` (exclusive `after`, 10-year horizon, 5-occurrence snooze window).
- `data/model/RecurrenceRule.kt`: `frequency/interval/daysOfWeek/count`, `getNextOccurrence`,
  `WEEKDAYS`/`WEEKENDS`, `decodeLegacy` (only for MIGRATION_3_4). Stored as JSON via `Converters`.
- `data/model/ReminderAction.kt`: PK `(reminderId, originalScheduledTime)`, FK cascade delete.
  `ActionType` = COMPLETED | SNOOZED (`rescheduledTime`) | DELETED (one instance only).
- `domain/ReminderSchedule.kt`: `remindersForDay`, `overdueReminders` (1-year lookback). Pure.
- `data/repository/ReminderRepository.kt`: DAO wrapper, `cleanupOldReminders`, `restoreBackupData`.
- `data/repository/UserPreferencesRepository.kt`: DataStore: theme, hideCompleted, default
  times, last error, worker run history.
- `data/local/BackupAndRestore.kt`: JSON `BackupData` over a `Uri`. `data/legacy/DataConverter.kt`
  reads the old v1 app's backup format (sample: `app/src/test/resources/reminder_mate_*.json`).
- `notifications/`: `ReminderAlarmScheduler` (AlarmManager), `NotificationReceiver` (trigger,
  complete and snooze intents), `NotificationService` (builds notifications, ID/request-code scheme).
- `ui/core/ReminderViewModel.kt`: abstract base for the Home, AllReminders and Overdue VMs. It holds
  complete, snooze and delete actions; changes there affect all three screens.
- `ui/navigation/AppScreens.kt`: routes; `AddEditReminder` takes optional `reminderId`.
- `workers/`: `CleanupWorker` (daily), `OverdueWorker` (6 AM), both extend `BaseReminderWorker`
  (records run time + errors). Enqueued in `MyApplication.scheduleTasks()`.
- `utils/BootReceiver.kt`: reschedules every reminder after reboot.

## Core model: how scheduling works

Instances are **never stored**. A `ReminderInstance` = a `Reminder` occurrence + at most one
`ReminderAction` keyed by its *original* time. Snoozing changes `displayTime`, never `originalTime`.

Only **one alarm per reminder** exists at a time (request code `alarmRequestCode(id)`). Every
state change must leave the alarm in the right place:
- trigger → show notification → `schedule(reminder, after = triggerTime)` (chains the next one)
- complete or delete instance → cancel notification → `schedule(reminder, after = displayTime)`
- snooze → `scheduleInstance(instance.copy(displayTime = newTime))`
- edit or restore → `schedule(reminder)`; delete reminder → `cancel(reminder)`

If you add a mutation, check both `ReminderViewModel` and `NotificationReceiver`, because
both paths exist.

## Rules not to break

- Notification IDs and PendingIntent request codes come from the row id
  (`NotificationService.requestCode`, 4 slots per reminder). Add a slot by widening `REQUEST_CODE_SLOTS`.
- Changing `Reminder`, `ReminderAction`, or `RecurrenceRule` changes **three formats**: the Room
  schema, the backup JSON (`BackupData`), and the stored recurrence JSON. Old backups must still
  restore, so new fields need defaults. For a DB change, use the `room-migration` skill.
- New user-visible text goes in `strings.xml` for **all 14 locales**; use the `add-strings` skill.
- Use `LocalDateTime` (system zone) throughout. Format with `DateTimeUtils`, and use
  `DateTimeUtils.minsFromNow` for "now + N".
- `domain/` and `data/model/` stay free of Android APIs (only Room/serialization annotations).

## Tests

- Unit tests in `app/src/test`: plain JUnit4, hand-built `Reminder(...)` fixtures with fixed
  `LocalDateTime`s; MockK for repository tests. Turbine is only on the **androidTest**
  classpath; add it to `testImplementation` if you need it in unit tests.
- Instrumented in `app/src/androidTest`: `ReminderIntegrationTest` (Hilt, `HiltTestRunner`),
  `Migration3To4Test` (`MigrationTestHelper` against `app/schemas/`).
- Logic changes to recurrence or scheduling go with a unit test in `RecurrenceRuleTest`,
  `ReminderTest` or `ReminderScheduleTest`.

## Release

Version is `versionCode`/`versionName` in `app/build.gradle.kts` (bump both). Store listing lives in
`fastlane/`; built APKs are committed under `releases/download/<version>/`.
