#!/usr/bin/env bash
# Copyright (C) 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
# Independent controller checks; this does NOT build Launcher3QuickStep.
set -euo pipefail
: "${ANDROID_JAR:?Set to an Android SDK android.jar (tested: API 35)}"
: "${ANDROID_BUILD_TOOLS:?Set to the SDK build-tools directory}"
: "${JAVA_HOME:?Set to a JDK directory}"
: "${ANDROID_SERIAL:?Set explicitly to the test device ADB serial}"
script_dir=$(cd "$(dirname "$0")" && pwd)
repo_root=$(git -C "$script_dir" rev-parse --show-toplevel)
checks_out=$(mktemp -d "${TMPDIR:-/tmp}/thor-controller-checks.XXXXXX")
mkdir -p "$checks_out/classes" "$checks_out/dex"
controller="$repo_root/src/com/android/launcher3/secondarydisplay/SecondaryDrawerResizeController.java"
shasum -a 256 "$controller" > "$checks_out/source-sha256.txt"
"$JAVA_HOME/bin/javac" -source 8 -target 8 -cp "$ANDROID_JAR" -d "$checks_out/classes" \
  "$controller" "$script_dir"/src/com/android/launcher3/secondarydisplay/*.java
"$ANDROID_BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --output "$checks_out/dex" \
  "$checks_out"/classes/com/android/launcher3/secondarydisplay/*.class
"$ANDROID_BUILD_TOOLS/aapt2" link -I "$ANDROID_JAR" --manifest "$script_dir/AndroidManifest.xml" -o "$checks_out/unsigned.apk"
(cd "$checks_out/dex" && zip -q ../unsigned.apk classes.dex)
"$ANDROID_BUILD_TOOLS/zipalign" -f 4 "$checks_out/unsigned.apk" "$checks_out/aligned.apk"
"$JAVA_HOME/bin/keytool" -genkeypair -keystore "$checks_out/test.keystore" -alias controller-test \
  -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 30 -dname 'CN=Local Controller Test'
"$ANDROID_BUILD_TOOLS/apksigner" sign --ks "$checks_out/test.keystore" --ks-pass pass:android \
  --out "$checks_out/controller-checks.apk" "$checks_out/aligned.apk"
# This fixed package name belongs only to this disposable test harness.
adb -s "$ANDROID_SERIAL" uninstall dev.kevinhowe.thordrawertests >/dev/null 2>&1 || true
adb -s "$ANDROID_SERIAL" install --no-incremental -t -r "$checks_out/controller-checks.apk"
component=dev.kevinhowe.thordrawertests/com.android.launcher3.secondarydisplay.DrawerChecks
adb -s "$ANDROID_SERIAL" shell am instrument -w "$component" | tee "$checks_out/first-run.txt"
adb -s "$ANDROID_SERIAL" shell am force-stop dev.kevinhowe.thordrawertests
adb -s "$ANDROID_SERIAL" shell am instrument -w -e verifySaved true "$component" | tee "$checks_out/restart-run.txt"
if grep -q 'FAIL' "$checks_out/first-run.txt" "$checks_out/restart-run.txt"; then
  echo "Controller check failure; see $checks_out" >&2
  exit 1
fi
grep -q 'Assertions: 31' "$checks_out/first-run.txt"
grep -q 'Assertions: 33' "$checks_out/restart-run.txt"
echo "Results and test-only APK: $checks_out"
echo 'Return to the normal launcher after testing. This app uses logical display 1.'
