#!/usr/bin/env bash
# Builds a signed release APK for Monero Watchman without needing Android Studio.
# Prompts for the keystore path and passwords; the key alias is detected
# automatically. Signing is injected into the build, so no project files change
# and no password is written to disk.
set -euo pipefail

# Run from the directory this script lives in (the project root).
cd "$(dirname "$0")"

# JDK 17 is required: this Gradle version can't parse newer JDK version strings.
JAVA_HOME_DEFAULT="/usr/lib/jvm/java-17-openjdk"
read -e -r -p "JAVA_HOME [${JAVA_HOME_DEFAULT}]: " java_home_input
export JAVA_HOME="${java_home_input:-$JAVA_HOME_DEFAULT}"
if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "Error: no java executable at $JAVA_HOME/bin/java" >&2
    exit 1
fi

# Keystore location.
KEYSTORE_DEFAULT="$HOME/keys/monero-watchman-release.jks"
read -e -r -p "Keystore path [${KEYSTORE_DEFAULT}]: " keystore_input
KEYSTORE_FILE="${keystore_input:-$KEYSTORE_DEFAULT}"
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "Error: keystore not found at $KEYSTORE_FILE" >&2
    echo "Create one with:" >&2
    echo "  keytool -genkeypair -v -keystore \"$KEYSTORE_FILE\" -alias monero-watchman -keyalg RSA -keysize 2048 -validity 10000" >&2
    exit 1
fi

# Passwords (read silently, never stored).
read -r -s -p "Keystore password: " STORE_PASSWORD; echo
if [ -z "$STORE_PASSWORD" ]; then
    echo "Error: keystore password cannot be empty" >&2
    exit 1
fi

# Detect the key alias automatically.
KEY_ALIAS="$("$JAVA_HOME/bin/keytool" -list -keystore "$KEYSTORE_FILE" -storepass "$STORE_PASSWORD" 2>/dev/null \
    | awk -F', ' '/PrivateKeyEntry/{print $1; exit}')" || true
if [ -z "$KEY_ALIAS" ]; then
    echo "Error: could not read a key alias from the keystore (wrong password?)." >&2
    exit 1
fi
echo "Detected key alias: $KEY_ALIAS"

read -r -s -p "Key password (blank = same as keystore): " KEY_PASSWORD; echo
KEY_PASSWORD="${KEY_PASSWORD:-$STORE_PASSWORD}"

echo
echo "Building signed release APK..."
echo

./gradlew --no-daemon assembleRelease \
    -Pandroid.injected.signing.store.file="$KEYSTORE_FILE" \
    -Pandroid.injected.signing.store.password="$STORE_PASSWORD" \
    -Pandroid.injected.signing.key.alias="$KEY_ALIAS" \
    -Pandroid.injected.signing.key.password="$KEY_PASSWORD"

echo
echo "Done. Signed APK:"
echo "  $(pwd)/app/build/outputs/apk/release/app-release.apk"
