# FocusGuard

FocusGuard is an Android app that helps you limit time spent in distracting apps. It tracks app usage using the Android UsageStatsManager and enforces limits by blocking restricted apps in the foreground using an AccessibilityService. When a limit is reached the user can solve a quick math challenge to earn 5 extra minutes of usage.

---

## Key Features

- Tracks app usage using UsageStatsManager (PACKAGE_USAGE_STATS / Usage Access) with daily/session tracking.
- Accessibility-based foreground detection and immediate blocking when limits are exceeded.
- Fullscreen blocker screen (BlockerActivity) that shows a simple arithmetic challenge.
- Correct answer grants 5 minutes of extra time for that app; incorrect answers force a 1-minute retry delay.
- Extra time is persisted (SharedPreferences or local DB) and integrated into limit checks.
- Logging included for debugging: tags such as `FocusGuardAccessibility` and `AccessTimeManager`.

---

## Files and Components (high level)

- `FocusGuardAccessibilityService.kt` - AccessibilityService detecting foreground app changes and triggering blocks.
- `AccessTimeManager.kt` - Usage tracking and helpers (reads from UsageStatsManager, computes daily/session usage, checks limits).
- `UsageTrackingService.kt` - Periodic syncing of usage stats and session management.
- `BlockerActivity.kt` - Fullscreen overlay showing math challenge and granting extra time.
- `ui/theme/*` - Compose UI/theme files (typography, colors, etc.).
- `FOREGROUND_BLOCKING_IMPLEMENTATION.md` - Implementation notes for foreground blocking (present in repo).

---

## Permissions & Setup

1. Grant required runtime/settings permissions:
   - Accessibility: Settings → Accessibility → FocusGuard → Enable
   - Usage Access (PACKAGE_USAGE_STATS): Settings → Usage Access → FocusGuard → Allow usage access
   - (Optional) Overlay permission if your blocker uses SYSTEM_ALERT_WINDOW / overlays.

2. The manifest includes `PACKAGE_USAGE_STATS` request and AccessibilityService configuration. Usage Access must be enabled by the user in system settings (the app cannot prompt directly for it).

---

## How to build & install (local dev)

From the project root run (macOS / zsh):

```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If you need to uninstall previous debug builds first:

```bash
adb uninstall com.example.focusguard || true
```

---

## How to test the blocking & challenge flow

1. In the app, select one or more social apps to monitor and set a short daily limit (e.g. 1-2 minutes) for quick testing.
2. Grant Accessibility and Usage Access permissions as described above.
3. Launch a monitored app and use it until the limit is reached.
4. When the limit is exceeded the app should be immediately sent to background and `BlockerActivity` should appear.
5. Solve the arithmetic question shown in the blocker. If correct, you will receive 5 extra minutes for that app; if incorrect, you'll be shown a retry wait (1 minute) before attempting again.

---

## Developer tips & debugging

- Watch Logcat for these tags:
  - `FocusGuardAccessibility` — foreground detection, blocking events
  - `AccessTimeManager` — usage queries, limit checks, computed usage values
  - `UsageTrackingService` — periodic syncs and session lifecycle

Example to filter logs:

```bash
adb logcat -s FocusGuardAccessibility AccessTimeManager UsageTrackingService
```

- If UI changes (typography/layout) don't show, confirm `MainActivity.setContent {}` uses the app theme and that `MaterialTheme.typography` contains your custom fonts.
- If usage values look wrong, ensure Usage Access is granted and verify `UsageStatsManager.queryUsageStats()` returns non-empty results for the time interval.

---

## Important implementation notes

- Usage collection uses `UsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTimeMillis, endTimeMillis)` and summarizes app usage for the requested time range.
- Foreground detection is done via Accessibility events (TYPE_WINDOW_STATE_CHANGED) and periodic foreground checks using UsageEvents where necessary. Blocking is performed by launching `BlockerActivity` with `Intent.FLAG_ACTIVITY_NEW_TASK` from the AccessibilityService and sending the blocked app to the background with `performGlobalAction(GLOBAL_ACTION_HOME)`.
- Extra time granted after a successful challenge is stored and added to the effective limit when evaluating `isAppUsageLimitExceeded(packageName)`.

---

## Troubleshooting checklist

- If apps are not blocked even after the limit is reached:
  1. Confirm Accessibility service is enabled.
  2. Confirm the monitored app is correctly marked as monitored in the app's settings.
  3. Check logcat for limit-check messages and blocking logs.

- If `UsageStatsManager` returns empty results:
  - Ensure Usage Access is granted in Settings → Usage Access.

---

## Next steps / To do

- Add unit/instrumentation tests around usage calculations and `isAppUsageLimitExceeded`.
- Make blocker UI customizable (difficulty, reward minutes).
- Add analytics events for challenge success/failure attempts.

---

If you'd like, I can also add a short runbook with exact ADB commands, or wire up an in-app debug screen that dumps current usage values for each monitored app. Which would you prefer next?
