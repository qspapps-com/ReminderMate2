---
name: room-migration
description: Change the Room schema of ReminderMate (add/rename/remove a column or table on Reminder or ReminderAction, or change how a stored type is encoded). Covers version bump, Migration, schema export, migration test and backup compatibility.
---

# Room schema change checklist

Current DB version: see `@Database(version = N)` in `data/local/ReminderDatabase.kt`.
Exported schemas: `app/schemas/com.qspapps.remindermate.data.local.ReminderDatabase/<N>.json`.

1. **Edit the entity** (`data/model/Reminder.kt` or `ReminderAction.kt`). New fields need a
   Kotlin default so old backup JSON still decodes (`BackupAndRestore.decode`) and so
   `DataConverter` (legacy import) still compiles.
2. **Bump** `version` in `ReminderDatabase.kt` to N+1.
3. **Write `MIGRATION_N_N+1`** in `data/local/Migrations.kt`, following `MIGRATION_3_4`:
   - Use plain SQL (`ALTER TABLE ... ADD COLUMN ... NOT NULL DEFAULT ...`). Copy the exact column
     definitions from the newly exported schema JSON so Room's validation matches.
   - For data rewrites, read all rows into memory first, then write. Do not write while a cursor is open.
   - Never throw on bad rows. Degrade instead, as MIGRATION_3_4 nulls undecodable recurrences.
4. **Register** it in `di/AppModule.kt` → `.addMigrations(MIGRATION_3_4, MIGRATION_N_N+1)`.
   Do not extend `fallbackToDestructiveMigrationFrom`; v3+ installs exist.
5. **Export the schema**: `./gradlew compileDebugKotlin -q` writes `<N+1>.json`. Commit it.
   Do not edit existing schema JSONs.
6. **Test**: add `MigrationNToN+1Test` in `app/src/androidTest/.../data/local/`, modeled on
   `Migration3To4Test.kt`: create DB at N with raw SQL inserts, `runMigrationsAndValidate`, and
   assert the rows. Run: `./gradlew connectedDebugAndroidTest -q` (needs a device; if none,
   tell the user it was not run).
7. If a `TypeConverter` encoding changed (`data/local/Converters.kt`), the migration must rewrite
   existing rows, and backups produced by older versions must still restore.
