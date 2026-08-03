#!/bin/bash
# Provisions the Android SDK for Claude Code on the web sessions. The cloud
# sandbox ships a JDK but no Android toolchain, so without this every Gradle
# invocation dies at "Plugin [id: 'com.android.application'] was not found".
# Safe to re-run: sdkmanager skips packages that are already installed.
set -euo pipefail

# Local machines and dev containers already have their own SDK.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="13114758"

# Keep these in sync with app/build.gradle.kts: compileSdk release(37) needs
# platforms;android-37.0, and buildToolsVersion defaults to the AGP pin.
SDK_PACKAGES=(
  "platform-tools"
  "platforms;android-37.0"
  "platforms;android-36"
  "build-tools;36.0.0"
)

if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Installing Android cmdline-tools into $ANDROID_HOME"
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  curl -fsSL --retry 3 --retry-delay 2 \
    -o "$tmp/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi

export ANDROID_HOME
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# AGP also auto-downloads missing SDK components, which needs the licenses too.
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager "${SDK_PACKAGES[@]}" > /dev/null

# Gradle finds the SDK via local.properties (gitignored) or ANDROID_HOME.
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$CLAUDE_PROJECT_DIR/local.properties"

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    printf 'export ANDROID_HOME=%s\n' "$ANDROID_HOME"
    printf 'export ANDROID_SDK_ROOT=%s\n' "$ANDROID_HOME"
    printf 'export PATH="%s/cmdline-tools/latest/bin:%s/platform-tools:$PATH"\n' \
      "$ANDROID_HOME" "$ANDROID_HOME"
  } >> "$CLAUDE_ENV_FILE"
fi

echo "Android SDK ready at $ANDROID_HOME"
