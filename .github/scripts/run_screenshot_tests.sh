#!/usr/bin/env bash
set -euo pipefail

mkdir -p app/build/reports/screenshot-diagnostics
adb -s emulator-5554 shell settings put global hidden_api_policy 1
adb -s emulator-5554 logcat -c
adb -s emulator-5554 logcat -v threadtime > app/build/reports/screenshot-diagnostics/logcat.txt 2>&1 &
logcat_pid=$!
trap 'kill "$logcat_pid" 2>/dev/null || true' EXIT

./gradlew :app:debugExecuteScreenshotTests \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.deeplviewer.WebViewScreenshotTest \
  --max-workers=2 --stacktrace "$@"
