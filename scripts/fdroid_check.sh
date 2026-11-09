#!/usr/bin/env bash
# Runs fdroiddata's merge-request checks locally in Docker, before pushing the F-Droid MR.
#
#   scripts/fdroid_check.sh [lint|build|all]      (default: all)
#
#   lint   fdroid lint, fdroid rewritemeta (fails if it would reformat), JSON schema validation
#   build  fdroid build of the latest version in the metadata. With Binaries set, this also
#          downloads our published APK and checks that F-Droid's build reproduces it.
#
# Environment:
#   FDROIDDATA  path to the fdroiddata checkout   (default: ../f-droid-data next to this repo)
#   APPID       app to check                      (default: com.qspapps.remindermate)
#
# Mirrors .gitlab-ci.yml in fdroiddata: same image, fdroidserver from git at the commit that
# file pins. Caches (fdroidserver clone, Gradle, Android SDK) live in Docker volumes named
# fdroid-check-*; remove them with: docker volume rm $(docker volume ls -q -f name=fdroid-check-)
# See docs/fdroid-submission.md.
set -euo pipefail

MODE="${1:-all}"
ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
FDROIDDATA="$(cd "${FDROIDDATA:-$ROOT/../f-droid-data}" && pwd)"
APPID="${APPID:-com.qspapps.remindermate}"
IMAGE="registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie"

case "$MODE" in lint|build|all) ;; *) echo "usage: $0 [lint|build|all]" >&2; exit 2 ;; esac
[[ -f "$FDROIDDATA/metadata/$APPID.yml" ]] || { echo "No $FDROIDDATA/metadata/$APPID.yml" >&2; exit 1; }

# Use the fdroidserver commit fdroiddata's CI pins, so results match the pipeline.
FDROIDSERVER_COMMIT="$(sed -n 's/.*git -C \$fdroidserver checkout -B master \([0-9a-f]\{40\}\).*/\1/p' \
  "$FDROIDDATA/.gitlab-ci.yml" | head -1)"
[[ -n "$FDROIDSERVER_COMMIT" ]] || { echo "Could not find the pinned fdroidserver commit in .gitlab-ci.yml" >&2; exit 1; }

STATUS=0

# Schema validation runs in a plain python image, as in CI (the buildserver image has no pip).
if [[ "$MODE" != build ]]; then
  printf "\n\033[1m== schema validation\033[0m\n"
  docker run --rm -t -e PIP_DISABLE_PIP_VERSION_CHECK=1 -v "$FDROIDDATA:/fdroiddata:ro" -v fdroid-check-pip:/root/.cache/pip python:3-slim \
    sh -c "pip install -q --root-user-action=ignore check-jsonschema >/dev/null &&
           check-jsonschema --schemafile /fdroiddata/schemas/metadata.json /fdroiddata/metadata/$APPID.yml" \
    || { echo "FAILED: schema"; STATUS=1; }
fi

# The image is amd64 only; on Apple Silicon it runs under emulation (slower, but works).
docker run --rm -t --platform linux/amd64 \
  -v "$FDROIDDATA:/fdroiddata" \
  -v fdroid-check-fdroidserver:/cache/fdroidserver \
  -v fdroid-check-gradle:/root/.gradle \
  -v fdroid-check-android-sdk:/opt/android-sdk \
  -e MODE="$MODE" -e APPID="$APPID" -e FDROIDSERVER_COMMIT="$FDROIDSERVER_COMMIT" \
  -e HOST_UID="$(id -u)" -e HOST_GID="$(id -g)" \
  "$IMAGE" bash -c '
set -euo pipefail
source /etc/profile.d/bsenv.sh
export fdroidserver=/cache/fdroidserver
git config --global --add safe.directory "*"
git config --global advice.detachedHead false

step() { printf "\n\033[1m== %s\033[0m\n" "$*"; }
FAILED=()
# Files fdroid creates in the fdroiddata checkout should stay owned by the host user.
trap "chown -R $HOST_UID:$HOST_GID /fdroiddata/build /fdroiddata/tmp /fdroiddata/unsigned /fdroiddata/logs /fdroiddata/metadata 2>/dev/null || true" EXIT

step "fdroidserver @ ${FDROIDSERVER_COMMIT:0:10}"
if [ ! -d $fdroidserver/.git ]; then git clone -q https://gitlab.com/fdroid/fdroidserver.git $fdroidserver; fi
if ! git -C $fdroidserver cat-file -e "$FDROIDSERVER_COMMIT^{commit}" 2>/dev/null; then git -C $fdroidserver fetch -q origin; fi
git -C $fdroidserver checkout -q --force "$FDROIDSERVER_COMMIT"
export PATH="$fdroidserver:$PATH" PYTHONPATH="$fdroidserver:$fdroidserver/examples" PYTHONUNBUFFERED=true
export serverwebroot=/tmp   # config.yml references this env var
cd /fdroiddata
chmod 0600 config.yml 2>/dev/null || true

if [ "$MODE" != build ]; then
  step "fdroid lint $APPID"
  fdroid lint "$APPID" || FAILED+=("lint")

  step "fdroid rewritemeta $APPID"
  cp "metadata/$APPID.yml" /tmp/before.yml
  fdroid rewritemeta "$APPID"
  if ! diff -u /tmp/before.yml "metadata/$APPID.yml"; then
    echo "rewritemeta reformatted the file (shown above); it is now rewritten, review and commit it."
    FAILED+=("rewritemeta")
  else
    echo "no formatting changes"
  fi

fi

if [ "$MODE" != lint ]; then
  step "fdroid build $APPID (latest version)"
  # Same packages the CI job adds to make the image match the production buildserver.
  [ -d "$ANDROID_HOME/build-tools/31.0.0" ] || { yes || true; } | sdkmanager "platform-tools" "build-tools;31.0.0" >/dev/null
  rm -rf tmp/binaries "tmp/${APPID}_"*.apk
  # fdroid build exits 0 even when the build or the reproducibility comparison fails, so judge
  # by its summary line and by whether it kept the verified binary (it deletes it on mismatch).
  fdroid build --verbose --test --latest "$APPID" 2>&1 | tee /tmp/fdroid-build.log || true
  if grep -qE "INFO: [0-9]+ builds? failed" /tmp/fdroid-build.log; then
    echo; echo "Build failed. Reason (from the log above):"
    sed -n "/==== detail begin/,/==== detail end/p" /tmp/fdroid-build.log | cut -c1-300
    FAILED+=("build")
  elif grep -q "^Binaries:" "metadata/$APPID.yml"; then
    if ls tmp/binaries/"${APPID}"_*.binary.apk >/dev/null 2>&1; then
      echo "REPRODUCIBLE: F-Droid build matches the APK at the Binaries URL"
    else
      echo "Binaries is set, but no verified binary was kept: treat as not reproducible."
      FAILED+=("reproducible")
    fi
  else
    echo "Built (no Binaries in metadata, so no reproducibility check)"
  fi
fi

step "result"
if [ ${#FAILED[@]} -eq 0 ]; then echo "all checks passed"; else echo "FAILED: ${FAILED[*]}"; exit 1; fi
' || STATUS=1

exit $STATUS
