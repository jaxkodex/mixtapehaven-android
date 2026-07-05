#!/usr/bin/env bash
# Install the Android SDK (cmdline-tools + platform for this project) into
# $ANDROID_HOME. Idempotent: the SDK lives on a named volume, so re-creating
# the container skips the download.
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708"  # "latest" as of 2024-01; pinned for reproducibility
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

if ! command -v tmux &> /dev/null; then
  sudo apt-get update && sudo apt-get install -y tmux
fi
mkdir -p "$HOME/.claude"
sudo chown -R "$(id -u):$(id -g)" "$HOME/.claude"

mkdir -p "$ANDROID_HOME"
sudo chown -R "$(id -u):$(id -g)" "$ANDROID_HOME"

if [ ! -x "$SDKMANAGER" ]; then
  echo "Installing Android cmdline-tools..."
  tmp=$(mktemp -d)
  curl -fsSL -o "$tmp/tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$tmp"
fi

yes | "$SDKMANAGER" --licenses > /dev/null || true
# compileSdk is release(37) in app/build.gradle.kts; platform 37 ships under
# the minor-versioned name "android-37.0"
"$SDKMANAGER" --install \
  "platform-tools" \
  "platforms;android-37.0" \
  "build-tools;37.0.0"

echo "Android SDK ready at $ANDROID_HOME"
