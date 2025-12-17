

# Notes

## Using https://jules.google.com/

## Download latest APK
https://github.com/jmasalma/AthanAlarm/releases/latest


## Increment tag

```bash

git checkout main
git pull --prune
latest_tag=$(git describe --abbrev=0 --tags)
new_tag=$(echo "$latest_tag" | awk -F'.' -v OFS='.' '{$NF++; print $0}')
echo Moving tag from ${latest_tag} to ${new_tag}

git tag ${new_tag}
git push origin --tags

```





## Setup Repo

https://github.com/jmasalma/AthanAlarm/settings/actions

Workflow permissions -> Read and write permissions


## Add the following setup scripts

https://jules.google.com/repo/github/jmasalma/AthanAlarm/config


### V1 - seems to be causing jules error

Error: jules encountered an error when working on the task.

```bash


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




# --- 5. Accept licenses ---
echo "Accepting Android SDK licenses..."
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses







# --- 4. Install SDK packages (using separate commands) ---
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








# --- 6.5. My stuff
cat "$HOME/.bashrc"
echo $ANDROID_HOME
ls -latrh $ANDROID_HOME
ls -latrh $ANDROID_HOME/cmdline-tools
ls -latrh $ANDROID_HOME/cmdline-tools/latest
ls -latrh $ANDROID_HOME/cmdline-tools/latest/*


# --- 7. Verification ---
echo "Android SDK setup complete. Verifying installation..."
"$ANDROID_HOME/platform-tools/adb" --version
echo "Verification complete. adb command should be available in new shells."


```



### V2 - to fix jules error

```bash

#!/bin/bash
set -e

# --- 1. Set up constants ---
# Use the absolute path for safety
export ANDROID_HOME="$HOME/android-sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
TOOLS_ZIP="commandlinetools.zip"

echo "=== Starting Android SDK Setup ==="

# --- 2. Clean Install Directory ---
# Clear previous attempts to avoid "folder already exists" errors causing weird nesting
rm -rf "$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools"

# --- 3. Download and Extract ---
echo "Downloading Command-line Tools..."
wget -q --output-document="$TOOLS_ZIP" "$CMDLINE_TOOLS_URL"

echo "Extracting..."
unzip -q "$TOOLS_ZIP"
# The zip extracts to a folder named "cmdline-tools"
# We need the structure: $ANDROID_HOME/cmdline-tools/latest/bin
mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
mv cmdline-tools/* "$ANDROID_HOME/cmdline-tools/latest/"
rmdir cmdline-tools
rm "$TOOLS_ZIP"

# --- 4. Define SDK Manager Variable ---
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
chmod +x "$SDKMANAGER"

# --- 5. Install Packages & Accept Licenses ---
echo "Accepting Licenses and Installing Core Packages..."
# We combine the install command to save time and reduce output spam
# "platform-tools" : Required for adb
# "platforms;android-34" : The target SDK version
# "build-tools;34.0.0" : The build tools version
yes | "$SDKMANAGER" --licenses > /dev/null 2>&1
echo "Installing platform-tools, platforms;android-34, build-tools;34.0.0..."
yes | "$SDKMANAGER" "platform-tools" "platforms;android-34" "build-tools;34.0.0" > /dev/null

# --- 6. CRITICAL FIX: Create local.properties ---
# This ensures Gradle finds the SDK even if shell variables are lost
echo "Creating local.properties..."
PROJECT_ROOT=$(pwd) 
# Assumes the script runs in the repo root. If not, hardcode the path.
echo "sdk.dir=$ANDROID_HOME" > "$PROJECT_ROOT/local.properties"

# --- 7. Persistence (Backup method) ---
# We still update .bashrc for future interactive sessions
echo "export ANDROID_HOME=$ANDROID_HOME" >> "$HOME/.bashrc"
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools" >> "$HOME/.bashrc"

# --- 8. Verification ---
echo "=== Setup Complete ==="
echo "SDK Location: $ANDROID_HOME"
ls -la "$PROJECT_ROOT/local.properties"
"$ANDROID_HOME/platform-tools/adb" --version

```
