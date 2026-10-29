---
name: add-strings
description: Add, change or remove user-visible strings in ReminderMate across all 14 locales (ar bn de es fr gu hi ja kn ml mr ta te zh). Use whenever UI/notification text is added or edited.
---

# Localized strings

Base: `app/src/main/res/values/strings.xml`. Locales: `values-{ar,bn,de,es,fr,gu,hi,ja,kn,ml,mr,ta,te,zh}`.
Never hardcode UI text in Kotlin. Use `stringResource(R.string.x)` in Compose and
`context.getString` in notifications and workers.

## Add
1. Add the English `<string name="snake_case">` to `values/strings.xml`. Use positional args
   (`%1$s`, `%2$d`) and escape apostrophes (`\'`).
2. Add translations to all 14 locale files. Choose one:
   - **Script (needs network + `pip install deep-translator`, so ask the user before running):**
     put the new keys in the `NEW_STRINGS` dict in `scripts/append_translations.py`, writing
     placeholders as `{0}`/`{1}` (converted to `%1$s`/`%2$s`). Run `python3 scripts/append_translations.py`
     from the repo root, then restore `NEW_STRINGS` with `git checkout scripts/append_translations.py`.
     The script rewrites each XML file, so check `git diff --stat app/src/main/res`.
   - **By hand:** append translated entries to each file. Keep placeholders and escaping identical.
3. Verify every locale has the same keys:
   ```bash
   cd app/src/main/res; for d in values-*; do diff -q <(grep -o 'name="[^"]*"' values/strings.xml|sort) <(grep -o 'name="[^"]*"' $d/strings.xml|sort) >/dev/null || echo "$d differs"; done
   ```

## Remove
`python3 scripts/remove_strings.py --help` deletes IDs from every `strings.xml`. Grep for
`R.string.<id>` first to confirm the ID is unused.
