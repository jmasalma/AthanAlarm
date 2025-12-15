#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- 1. Set up constants and directory structure ---
ANDROID_HOME="$HOME/android-sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
TOOLS_ZIP="commandlinetools.zip"

echo "Creating Android SDK directory at $ANDROID_HOME..."
mkdir -p "$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd "$ANDROID_HOME"

# --- 2. Download and extract command-line tools ---
echo "Downloading Android SDK Command-line Tools..."
wget --output-document="$TOOLS_ZIP" "$CMDLINE_TOOLS_URL"
unzip "$TOOLS_ZIP" -d cmdline-tools/latest
rm "$TOOLS_ZIP"

# --- 3. Configure the command-line tool structure ---
# Required directory structure: .../cmdline-tools/latest/bin
echo "Configuring command-line tool directory structure..."
mv "$ANDROID_HOME/cmdline-tools/latest/cmdline-tools/"* "$ANDROID_HOME/cmdline-tools/latest/"
rm -rf "$ANDROID_HOME/cmdline-tools/latest/cmdline-tools"




# --- 4. Accept licenses ---
echo "Accepting Android SDK licenses..."
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses




# --- 5. Install SDK packages (using separate commands) ---
echo "Installing Android SDK packages individually..."
# Ensure the sdkmanager is executable
chmod +x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

# Install platform-tools separately
echo "Installing platform-tools..."
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platform-tools"

"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-33"


# Install build-tools
echo "Installing build-tools;33.0.0..."
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "build-tools;33.0.0"

# Install a recent Android platform
echo "Installing platforms;android-34..."
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-34"

# Install build-tools
echo "Installing build-tools;34.0.0..."
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "build-tools;34.0.0"


# --- 6. Set environment variables ---
echo "Setting environment variables..."
echo "export ANDROID_HOME=$ANDROID_HOME" >> "$HOME/.bashrc"
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools" >> "$HOME/.bashrc"

# Refresh the shell's environment variables
source "$HOME/.bashrc"



# --- 7. Verification ---
echo "Android SDK setup complete. Verifying installation..."
"$ANDROID_HOME/platform-tools/adb" --version
echo "Verification complete. adb command should be available in new shells."


