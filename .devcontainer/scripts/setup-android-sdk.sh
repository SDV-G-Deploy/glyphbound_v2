#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$PWD/.android-sdk}}"
CMDLINE_TOOLS_DIR="$SDK_ROOT/cmdline-tools/latest"
SDKMANAGER="$CMDLINE_TOOLS_DIR/bin/sdkmanager"

mkdir -p "$SDK_ROOT"
chmod +x "$CMDLINE_TOOLS_DIR/bin/"* 2>/dev/null || true

if [ ! -x "$SDKMANAGER" ]; then
  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TMP_DIR"' EXIT

  curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "$TMP_DIR/cmdline-tools.zip"
  mkdir -p "$SDK_ROOT/cmdline-tools"
  python3 - <<'PY' "$TMP_DIR/cmdline-tools.zip" "$TMP_DIR"
import sys
import zipfile
zip_path, out_dir = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(zip_path) as zf:
    zf.extractall(out_dir)
PY
  rm -rf "$CMDLINE_TOOLS_DIR"
  mkdir -p "$CMDLINE_TOOLS_DIR"
  cp -R "$TMP_DIR/cmdline-tools/"* "$CMDLINE_TOOLS_DIR/"
  chmod +x "$CMDLINE_TOOLS_DIR/bin/"* || true
fi

export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"
export PATH="$CMDLINE_TOOLS_DIR/bin:$SDK_ROOT/platform-tools:$PATH"

set +o pipefail
yes | "$SDKMANAGER" --licenses >/dev/null
set -o pipefail
"$SDKMANAGER" --install \
  "platform-tools" \
  "platforms;android-34" \
  "build-tools;34.0.0"

echo "Android SDK ready at: $SDK_ROOT"
