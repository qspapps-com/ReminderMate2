#!/usr/bin/env bash
# Builds a signed, F-Droid-reproducible release APK for a tagged version.
#
#   scripts/build_release.sh <version> [keystore] [key-alias]
#   e.g. scripts/build_release.sh 2.0.26 ~/personal/remindermate2.jks remindermate2
#
# Passwords come from KEYSTORE_PASSWORD / KEY_PASSWORD, or are prompted for.
#
# Why this script exists: F-Droid rebuilds the app from the tagged commit with plain Gradle and
# only publishes our APK if its own build matches byte for byte. So the release must be built
#   - from the exact tagged commit with a clean tree (the commit hash is embedded in the APK),
#   - from the command line, not Android Studio's "Generate Signed APK" (which adds a source
#     stamp, stamp-cert-sha256, that F-Droid's build does not have).
# The signing key is passed as AGP's android.injected.signing.* properties, so no signing
# config lives in the build files.
set -euo pipefail

VERSION="${1:?usage: $0 <version> [keystore] [key-alias]}"
KEYSTORE="${2:-$HOME/personal/remindermate2.jks}"
ALIAS="${3:-}"
TAG="release/$VERSION"
EXPECTED_CERT="402fc22d03ace3e64aafe431d5a5477f5f2955396317a98611d878519d0a21d8"
EXPECTED_CERT="${EXPECTED_CERT_SHA256:-$EXPECTED_CERT}"

ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
OUT_DIR="$ROOT/releases/download/$VERSION"
OUT_APK="$OUT_DIR/remindermate2-$VERSION.apk"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
APKSIGNER="$(ls -d "$SDK"/build-tools/*/apksigner | sort -V | tail -1)"

[[ -f "$KEYSTORE" ]] || { echo "Keystore not found: $KEYSTORE" >&2; exit 1; }
git -C "$ROOT" rev-parse -q --verify "refs/tags/$TAG" >/dev/null || { echo "Tag $TAG not found" >&2; exit 1; }

if [[ -z "${KEYSTORE_PASSWORD:-}" ]]; then read -rsp "Keystore password: " KEYSTORE_PASSWORD; echo; fi
if [[ -z "$ALIAS" ]]; then
  ALIASES="$(keytool -list -keystore "$KEYSTORE" -storepass "$KEYSTORE_PASSWORD" \
    | sed -n 's/^\([^,]*\), .*PrivateKeyEntry.*/\1/p')"
  [[ -n "$ALIASES" && "$(wc -l <<<"$ALIASES")" -eq 1 ]] \
    || { echo "Pass the key alias; found: ${ALIASES:-none}" >&2; exit 1; }
  ALIAS="$ALIASES"
fi
if [[ -z "${KEY_PASSWORD:-}" ]]; then read -rsp "Key password (enter = same as keystore): " KEY_PASSWORD; echo; fi
KEY_PASSWORD="${KEY_PASSWORD:-$KEYSTORE_PASSWORD}"

# Build from a pristine checkout of the tag, never from the working tree.
WORKTREE="$(mktemp -d)/remindermate-$VERSION"
trap 'git -C "$ROOT" worktree remove --force "$WORKTREE" >/dev/null 2>&1 || true' EXIT
git -C "$ROOT" worktree add --detach "$WORKTREE" "$TAG" >/dev/null
cp "$ROOT/local.properties" "$WORKTREE/" 2>/dev/null || true
echo "Building $TAG ($(git -C "$WORKTREE" rev-parse HEAD))"

# Passwords go through the environment (ORG_GRADLE_PROJECT_*), not argv, so they don't show in ps.
( cd "$WORKTREE" && env \
    "ORG_GRADLE_PROJECT_android.injected.signing.store.file=$KEYSTORE" \
    "ORG_GRADLE_PROJECT_android.injected.signing.store.password=$KEYSTORE_PASSWORD" \
    "ORG_GRADLE_PROJECT_android.injected.signing.key.alias=$ALIAS" \
    "ORG_GRADLE_PROJECT_android.injected.signing.key.password=$KEY_PASSWORD" \
    ./gradlew --no-daemon --no-build-cache -q clean :app:assembleRelease )

VERSION_CODE="$(sed -n 's/^ *versionCode = //p' "$WORKTREE/app/build.gradle.kts")"
BUILT="$(ls "$WORKTREE"/app/build/outputs/apk/release/*.apk | grep -v unsigned | head -1)"

# Checks F-Droid would otherwise fail on.
if unzip -l "$BUILT" | grep -q 'stamp-cert-sha256'; then
  echo "FAIL: APK contains a source stamp (stamp-cert-sha256); F-Droid cannot reproduce it." >&2; exit 1
fi
CERTS="$("$APKSIGNER" verify --print-certs "$BUILT")"
if grep -qi 'Source Stamp' <<<"$CERTS"; then echo "FAIL: APK has a Source Stamp signer." >&2; exit 1; fi
CERT="$(grep -v 'Source Stamp' <<<"$CERTS" | sed -n 's/.*Signer.*certificate SHA-256 digest: //p' | head -1)"
if [[ "$CERT" != "$EXPECTED_CERT" ]]; then
  echo "FAIL: signed with $CERT, expected $EXPECTED_CERT (AllowedAPKSigningKeys)." >&2; exit 1
fi

mkdir -p "$OUT_DIR"
cp "$BUILT" "$OUT_APK"
echo "OK: $OUT_APK"
echo "    signer SHA-256 $CERT, no source stamp"
echo "Next: gh release create '$TAG' '$OUT_APK' --title '$VERSION' --notes-file fastlane/metadata/android/en-US/changelogs/$VERSION_CODE.txt"
