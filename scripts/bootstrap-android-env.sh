#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Keep JAVA_HOME if already set by caller; otherwise try common mise path used in CI/dev shells.
if [[ -z "${JAVA_HOME:-}" && -d "/root/.local/share/mise/installs/java/17.0.2" ]]; then
  export JAVA_HOME="/root/.local/share/mise/installs/java/17.0.2"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ROOT_DIR/.android-sdk}"
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"

echo "[bootstrap] ROOT_DIR=$ROOT_DIR"
echo "[bootstrap] JAVA_HOME=${JAVA_HOME:-<not-set>}"
echo "[bootstrap] ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
echo "[bootstrap] ANDROID_HOME=$ANDROID_HOME"

bash .devcontainer/scripts/setup-android-sdk.sh
bash .devcontainer/scripts/ensure-local-properties.sh

echo "[bootstrap] Android environment is ready."
echo "[bootstrap] Next: ./gradlew :app:assembleDebug"
