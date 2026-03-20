#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
CMDLINE_TOOLS_ROOT="$SDK_ROOT/cmdline-tools"
LATEST_TOOLS_DIR="$CMDLINE_TOOLS_ROOT/latest"
TOOLS_BIN_DIR="$LATEST_TOOLS_DIR/bin"
PLATFORM_VERSION="${ANDROID_PLATFORM_VERSION:-34}"
BUILD_TOOLS_VERSION="${ANDROID_BUILD_TOOLS_VERSION:-34.0.0}"
COMMANDLINE_TOOLS_ZIP="${ANDROID_COMMANDLINE_TOOLS_ZIP:-commandlinetools-linux-11076708_latest.zip}"
COMMANDLINE_TOOLS_URL="${ANDROID_COMMANDLINE_TOOLS_URL:-https://dl.google.com/android/repository/${COMMANDLINE_TOOLS_ZIP}}"

mkdir -p "$SDK_ROOT" "$CMDLINE_TOOLS_ROOT"

if [ ! -x "$TOOLS_BIN_DIR/sdkmanager" ]; then
  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TMP_DIR"' EXIT

  echo "Installing Android command-line tools into $SDK_ROOT"
  curl -fsSL "$COMMANDLINE_TOOLS_URL" -o "$TMP_DIR/cmdline-tools.zip"
  unzip -q "$TMP_DIR/cmdline-tools.zip" -d "$TMP_DIR/unpacked"
  rm -rf "$LATEST_TOOLS_DIR"
  mkdir -p "$LATEST_TOOLS_DIR"
  cp -R "$TMP_DIR/unpacked/cmdline-tools/." "$LATEST_TOOLS_DIR/"
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$TOOLS_BIN_DIR:$SDK_ROOT/platform-tools:$PATH"

set +o pipefail
yes | sdkmanager --sdk_root="$SDK_ROOT" --licenses >/dev/null
set -o pipefail
sdkmanager --sdk_root="$SDK_ROOT" \
  "platform-tools" \
  "platforms;android-${PLATFORM_VERSION}" \
  "build-tools;${BUILD_TOOLS_VERSION}"

cat > "$ROOT_DIR/local.properties" <<EOF
sdk.dir=$SDK_ROOT
EOF

echo "Android SDK ready at $SDK_ROOT"
echo "local.properties written to $ROOT_DIR/local.properties"
