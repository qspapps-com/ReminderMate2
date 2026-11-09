# Releasing to F-Droid

F-Droid builds ReminderMate from source and uses **reproducible builds**: it rebuilds the tagged
commit and publishes *our* signed APK only if its build matches it byte for byte. Two fields in
the fdroiddata metadata enable this:

- `Binaries`: where F-Droid downloads our signed APK
  (`https://github.com/qspapps-com/ReminderMate2/releases/download/release/%v/remindermate2-%v.apk`)
- `AllowedAPKSigningKeys`: the certificate SHA-256 that APK must be signed with
  (`402fc22d…`, the key in `~/personal/remindermate2.jks`)

That key is the Play **upload** key. Google re-signs Play builds with its own key, so F-Droid/GitHub
APKs and Play APKs have different signatures, and users can't switch between them without
reinstalling. Keep backups of the keystore and its passwords: if it is lost, F-Droid users can
never get another update.

Repos:

- `ReminderMate2` (this repo): app source, `scripts/`, APKs under `releases/download/<version>/`
- fdroiddata fork at `../f-droid-data`, branch `com.qspapps.remindermate`, app metadata in
  `metadata/com.qspapps.remindermate.yml`

## Requirements

- Docker Desktop, running. The F-Droid image is amd64 only; on Apple Silicon it runs under
  emulation, so the `build` check takes several minutes (longest on the first run, which downloads
  the Gradle distribution and all dependencies into a cache volume). To speed it up:
  - Settings → General: turn on "Use Rosetta for x86_64/amd64 emulation on Apple Silicon".
  - Settings → Resources: give Docker 12 GB+ memory; a release build with R8 needs about 6–7 GB.
  The first run also pulls a multi-GB image.
- `gh` CLI logged in to GitHub (for the release upload).

## Release steps

1. **Bump the version** in `app/build.gradle.kts` (`versionCode` and `versionName`), add
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, commit, and tag:

   ```sh
   git tag release/<version> && git push origin main release/<version>
   ```

2. **Build the signed APK** from that tag:

   ```sh
   scripts/build_release.sh <version>
   ```

   It builds a clean checkout of `release/<version>` from the command line, signs it with the upload
   key, and fails if the APK has a source stamp or the wrong signer. **Never** use Android Studio's
   "Generate Signed APK" for F-Droid releases: it adds a source stamp that F-Droid's build can't
   reproduce.

3. **Publish the GitHub Release** with the APK attached. The script prints this command:

   ```sh
   gh release create release/<version> releases/download/<version>/remindermate2-<version>.apk \
     --title <version> --notes-file fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
   ```

   Commit the APK under `releases/download/<version>/` too.

4. **Update the fdroiddata metadata** (only for the first submission or a manual update; after the
   app is merged, F-Droid's bot picks up new `release/*` tags by itself). Add a `Builds` entry with
   the new `versionName`, `versionCode` and full `commit` hash, and set `CurrentVersion` and
   `CurrentVersionCode`.

5. **Run the checks locally** (next section), then commit and push the fdroiddata branch.

## Checking before you submit

`scripts/fdroid_check.sh` runs the same checks as the fdroiddata merge-request pipeline, in the
same Docker image and with the same pinned fdroidserver version, against `../f-droid-data`:

```sh
scripts/fdroid_check.sh lint    # fast: lint, rewritemeta, schema validation
scripts/fdroid_check.sh build   # slow: full F-Droid build + reproducibility check
scripts/fdroid_check.sh         # both
```

| Check | What it catches |
|---|---|
| `fdroid lint` | Invalid or discouraged metadata (fields, URLs, categories, build flags) |
| `fdroid rewritemeta` | Formatting CI would reject. If it reformats the file, the script shows the diff and leaves the rewritten file in place; review it and commit it. |
| schema validation | Wrong field types or unknown fields (`schemas/metadata.json`) |
| `fdroid build` | Source doesn't build on F-Droid's buildserver, scanner findings (non-free deps, binaries), and, because `Binaries` is set, whether F-Droid's build matches our published APK |

Success looks like `all checks passed`; for `build`, also
`REPRODUCIBLE: F-Droid build matches the APK at the Binaries URL`.

Use the Docker checks, not a local `pip`/venv `fdroid`. A local fdroidserver can be older than the
one CI pins and pass files CI rejects (this happened with line wrapping of the `Binaries` URL).

Options:

- `FDROIDDATA=/path/to/fdroiddata scripts/fdroid_check.sh` if the fork is somewhere else.
- Build output (logs, the unsigned APK, the downloaded binary) is left in the fdroiddata checkout
  under `build/`, `tmp/`, `unsigned/` and `logs/`. All are gitignored there.
- Caches live in Docker volumes `fdroid-check-*`. Clear them with
  `docker volume rm $(docker volume ls -q -f name=fdroid-check-)`.

## If the build is not reproducible

`fdroid build` prints which files differ. Common causes:

- The APK wasn't built by `scripts/build_release.sh` from the exact tagged commit (the commit hash
  is embedded in `META-INF/version-control-info.textproto`).
- The `commit:` in the metadata doesn't match the tag the APK was built from.
- A different JDK or build-tools version. The build uses JDK 21 via `jvmToolchain(21)`.
- `assets/dexopt/baseline.prof` differs: a known source of non-determinism in some AGP versions.

To compare the two APKs yourself, use `diffoscope` on `tmp/binaries/*.binary.apk` and
`tmp/com.qspapps.remindermate_<versionCode>.apk` in the fdroiddata checkout.
