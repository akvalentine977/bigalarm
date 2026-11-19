# Big Alarm

A standalone alarm app for the Samsung Galaxy Watch5 (SM-R910, One UI 8 Watch / Wear OS 6) and any
other Wear OS 5+ watch. Built because the stock alarm app's names are too small to read without
glasses and it stops at 15 alarms.

## What it does

- **Big alarm names.** A text-size setting (1x to 3x, on top of the system font size) applies to the
  alarm name in the list, in the editor and on the ringing screen. The ringing screen shows the name
  in the biggest size with huge Dismiss and Snooze buttons. Names are shown in yellow on black and,
  by default, in capital letters (switchable in Settings). "Appointment" is abbreviated to "appt"
  automatically; the word list lives in `core/NameFormat.kt`.
- **Unlimited alarms.** Alarms live in a local SQLite database (Room). There is no cap.
- **Custom repeat rules.** Once, once on a specific date, every day, chosen days of the week, or every
  N days counted from a start date. Rules are computed in pure Kotlin and covered by unit tests.
- **Per-alarm snooze and persistence.** Each alarm can have its own snooze length or use the app
  default, and can be set to ring until dismissed instead of timing out.
- **Buttons snooze.** While an alarm is ringing, pressing either physical button snoozes it. The Back
  button is handled as a key press. The Home button is reserved by the system and never reaches an
  app, so the ringing screen snoozes when Android reports that the user's own action is taking them
  away from it (`onUserLeaveHint`), which is what a Home press produces.
- **Editing switches an alarm back on.** Changing anything about an alarm that is switched off turns
  it on when saved. Saving it untouched leaves it off. The editor says which will happen.
- **Voice control (does not work on the Galaxy Watch5, see Status).** With the switch in Settings on
  (it asks for microphone permission), the ringing
  screen listens with the watch's own speech recogniser: say "stop" or "snooze". It only listens while
  the ringing screen is showing, so it relies on that screen opening by itself. Works best with
  vibration-only alarms; an alarm tone playing at the same time makes recognition less reliable.
  The status line under the time on the ringing screen says what the recogniser is doing, and
  Settings has a "Test voice control" screen that lists the watch's speech services and shows what
  it hears. Recognisers are tried in order: on-device, default preferring offline, default online.
- **Reliable firing.** Alarms are registered with `AlarmManager.setAlarmClock`, the highest-priority
  alarm type Android has. They are re-registered after reboot, time or time-zone changes and app
  updates, and every time the app is opened.
- **Ringing.** A foreground service vibrates in a loop, optionally plays the watch's alarm ringtone
  (or synthesised beeps if the watch has none), wakes the screen, and posts a high-priority
  notification with Dismiss and Snooze actions. Two alarms firing at the same minute queue up
  instead of overwriting each other. After a configurable time the alarm gives up and posts a
  "Missed" notification.
- **Fast entry.** Names are typed or dictated with the watch's own input method. Names already used
  by other alarms are offered as one-tap choices. Any alarm can be duplicated.
- Standalone: no phone, no account, no store, no network.

## Status: builds and runs on the watch

Verified so far:

- `./gradlew :app:assembleDebug` succeeds with AGP 9.5.0, Gradle 9.6.0, the Kotlin 2.2.10 that AGP
  bundles, KSP 2.3.13 and compileSdk 37, with no Kotlin warnings. It produces
  `app/build/outputs/apk/debug/app-debug.apk`.
- The pure-Kotlin core (`core/` package: model, schedule computation, formatting) has 20 unit tests,
  all passing via `./gradlew :app:testDebugUnitTest`.
- Every library version in `gradle/libs.versions.toml` was checked against the official release
  pages on 4 Nov 2026.

Verified on the watch (Galaxy Watch5, One UI 8 Watch, Wear OS 6): the alarm fires on time, the
watch vibrates, the screen wakes, the ringing screen opens by itself,
the scaled text is readable, snooze works, pressing the physical buttons snoozes a ringing alarm,
editing an inactive alarm switches it back on, and the alarm name is readable on the edit screen in
the brighter yellow. Not yet exercised: sound, the every-N-days rule
and simultaneous alarms.

Notification panel, observed on the watch on 18 Nov 2026: the ringing notification's card comes out
yellowish, so the colour request is honoured there. The Dismiss and Snooze buttons that appear
after tapping the card stay gray.

The labels and icons of those two buttons are also requested in yellow, which is the only thing an
app can ask for on them. Tried on the watch the same day: the watch ignores it and the buttons stay
gray. The owner has accepted that, since that screen is rarely seen: the ringing screen normally
opens by itself. The request stays in the code in case other watch software honours it.

To see that screen for a test, deny the full-screen request with
`adb shell appops set com.akvalentine.bigalarm USE_FULL_SCREEN_INTENT deny`, let an alarm ring,
swipe right from the watch face and tap the card. Set it back to `default` afterwards, or alarms
will stop opening the ringing screen.

No items are open as of 18 Nov 2026.

Known not to work: voice control. Neither its first version nor the reworked listener responds on
this watch. The owner has decided to leave it as it is, so it is not being fixed. It is off by
default and nothing else depends on it.

Commit `3e070df` is the code of the build installed on the watch on 18 Nov 2026, where button snooze,
switching an edited alarm back on and the editor contrast fix were confirmed. It carries the tag
`watch-ok-2026-11-18`. A later build, from commit `58b3b09`, was installed the same day. It differs
only in the colour request for the notification buttons and in wording, and is the build on the
watch now.

See also the development-machine issue near the end of this file.

This project lives at <https://github.com/akvalentine977/bigalarm>. It was moved there on 17 Nov 2026
from a `projects/bigalarm` folder in an earlier repository that no longer exists. The commit history
was carried over with the project at the repository root, so commit hashes differ from the old ones.

If your Android Studio ships a different AGP, let the **AGP Upgrade Assistant** adjust `gradle/libs.versions.toml`; keep
the `kotlin` entry equal to the Kotlin Gradle plugin version that AGP release bundles (readable from
the AGP POM on maven.google.com).

## Requirements (Linux)

- A 64-bit x86 Linux with glibc 2.31 or newer. Android Studio does not support ARM Linux.
- [Android Studio](https://developer.android.com/studio) (Rabbit 2 or newer). It bundles a JDK and
  installs the SDK platform 37 and build tools on first sync. Everything is free and the SDK and
  Jetpack libraries are Apache 2.0.
- `adb` (comes with the SDK platform-tools; Android Studio installs it under
  `~/Android/Sdk/platform-tools`).
- Disk: roughly 10 GB for Studio, SDK and Gradle caches.

Command-line only is also possible: install `cmdline-tools`, run
`sdkmanager "platforms;android-37" "build-tools;36.0.0" "platform-tools"`, export `ANDROID_HOME`,
and use `./gradlew` as below.

## Build

Open the repository folder in Android Studio and let it sync, or from a terminal in that folder:

```bash
./gradlew :app:assembleDebug
```

Run the core unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

The debug APK ends up at `app/build/outputs/apk/debug/app-debug.apk`. Debug builds are fine for a
first look but noticeably sluggish on the watch; use a release build for daily wear.

## Release build (recommended for daily use)

Release builds are shrunk and optimised by R8 and carry the Compose baseline profiles, so the UI is
far smoother and the APK is about 2.5 MB instead of 45 MB. They need a signing key that stays the
same across builds, otherwise the watch refuses updates. One-time setup, in the project folder:

```bash
keytool -genkeypair -v -keystore bigalarm.jks -alias bigalarm -keyalg RSA -keysize 2048 -validity 10000
cp keystore.properties.example keystore.properties
```

Put the two passwords you chose into `keystore.properties`. Both files are git-ignored; back the
`.jks` up somewhere safe. Without `keystore.properties` the release build falls back to the debug
key, which works for a personal sideload but changes whenever the machine does.

Build, or build and install on a connected watch:

```bash
./gradlew :app:assembleRelease
./gradlew :app:installRelease
```

Switching from a build signed with a different key (for example a debug build) requires
`adb uninstall com.akvalentine.bigalarm` first, which also clears the alarms and the optional grant
described below. After that, every update is just `installRelease` and alarms are kept.

## Put it on the watch

Wear OS 3+ dropped Bluetooth debugging and the Watch5 charges wirelessly, so the route is Wi-Fi.
The watch and the Linux box must be on the same network.

1. On the watch: **Settings > About watch > Software** and tap **Software version** seven times
   to unlock Developer options.
2. **Settings > Developer options > ADB debugging** on, then **Wireless debugging** on.
3. Still under Wireless debugging, tap **Pair new device**. The watch shows a pairing code and an
   `IP:port`.
4. On the computer:

```bash
adb pair 192.168.1.50:41234
```

   Enter the code when prompted. Then connect using the second `IP:port` shown on the main
   Wireless debugging screen (a different port):

```bash
adb connect 192.168.1.50:37891
```

5. Install:

```bash
./gradlew :app:installDebug
```

   or, with an APK already built, `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Android Studio's device drop-down also lists the watch once `adb connect` has succeeded, so you can
use the Run button instead.

On first launch the app asks for notification permission. Allow it, otherwise the ringing
notification (and its Dismiss / Snooze buttons) cannot be shown.

## The ringing screen opening by itself

On the Galaxy Watch5 with One UI 8 Watch the big-text ringing screen opens by itself when an alarm
fires. Nothing has to be set up for that on this watch.

It gets there through the ringing notification's full-screen request. Google's docs state that
full-screen-intent notifications are not supported on Wear OS, but Samsung's watch software honours
them, as a developer report had suggested. The service also starts the ringing screen directly,
which Android only permits from the background with the "draw over other apps" grant.

Established on the watch on 18 Nov 2026:

- With that grant withdrawn (`adb shell appops get com.akvalentine.bigalarm SYSTEM_ALERT_WINDOW`
  printed `default`), the screen still opened by itself.
- With the full-screen request denied as well
  (`adb shell appops set com.akvalentine.bigalarm USE_FULL_SCREEN_INTENT deny`), it no longer did.
  Setting that back to `default` restores the pop-up.

Earlier notes in this project assumed the grant was required. It is not, on this watch.

If the ringing screen ever does not open by itself, on this or another watch, the watch still
buzzes, wakes and shows a notification with Dismiss and Snooze, and tapping it opens the ringing
screen. In that case the grant is worth trying. Wear OS has no settings page for it, but it can be
given once over adb:

```bash
adb shell appops set com.akvalentine.bigalarm SYSTEM_ALERT_WINDOW allow
```

The Settings screen in the app shows whether the grant is on. Withdraw it with `default` in place
of `allow`.

## How it works

```
AlarmManager.setAlarmClock ──► AlarmReceiver ──► AlarmRingService (foreground, type systemExempted)
                                                     ├─ Ringer: vibration loop, ringtone or tones
                                                     ├─ wake lock, screen on
                                                     ├─ notification: Dismiss / Snooze actions
                                                     └─ RingingState ──► AlarmRingActivity (big text)
Room (alarms.db) ◄──► AlarmRepository ◄──► AlarmActions ──► AlarmScheduler.rescheduleAll()
```

- `core/` is pure Kotlin: `Alarm`, `Repeat`, `Schedule.nextTrigger()`, `Format`, `NameFormat`
  (abbreviations and capitals), `VoiceCommands` (what counts as "stop" or "snooze") and `Edits` (an
  edited inactive alarm becomes active). Unit-tested.
- Build: AGP 9 built-in Kotlin (no `kotlin-android` plugin), Compose compiler plugin matched to the
  bundled Kotlin version, KSP 2.3.x for Room (older KSP releases are incompatible with built-in
  Kotlin's source-set rules).
- `data/` is Room plus a SharedPreferences-backed `Settings`.
- `alarm/` is scheduling and ringing. Every write goes through `AlarmActions`, which re-registers
  all system alarms afterwards, so the database and AlarmManager never drift apart.
- `ui/` is Compose for Wear OS Material 3 with swipe-to-dismiss navigation. Editing happens on an
  in-memory draft (`EditDraft`) and is written only on Save.

Permissions: `USE_EXACT_ALARM` (granted automatically to alarm-clock apps on API 33+),
`POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, foreground-service
permissions, the optional `SYSTEM_ALERT_WINDOW`, and `RECORD_AUDIO` only if voice control is turned
on. No network permission. Speech recognition runs on the watch's own recogniser; the app itself
sends nothing anywhere.

Database schema changes are handled by Room migrations in `data/AlarmDatabase.kt`, so updates keep
existing alarms.

## How changes land

- Changes are committed straight to `main`. This is a single-developer project with no review step.
- Everything pushed to `main` has been built, unit-tested and linted, but not necessarily tried on
  a watch.
- A build that has been confirmed working on the watch gets a tag on its commit, named
  `watch-ok-YYYY-MM-DD`. To rebuild a version known to work, check out the newest such tag.
- Tags are created and pushed from the owner's machine. The assistant sessions that work on this
  repository have been able to push branches but not tags.

## Open issue: desktop lock-ups on the development machine

Observed on the Linux development desktop (not on the watch): a few minutes after each
`./gradlew` build, the machine locked up hard and needed a power cycle. Since Gradle and adb
stopped being used on that machine there have been no further crashes (noted 10 Nov 2026).

That is a correlation, not a diagnosis. The cause was never identified: no journal output or memory
figures were captured, and it is not known whether Gradle, adb, or the two together trigger it.
Before building on that machine again:

- Read the previous boot's log after a lock-up: `journalctl -b -1 -p warning --no-pager | tail -n 80`.
  Out-of-memory kills point at the build's memory use; GPU resets, machine-check or thermal messages
  point at a driver or hardware fault that the build load merely triggers.
- Check memory and swap with `free -h` and `swapon --show`.
- Untested mitigation, in `~/.gradle/gradle.properties`: `org.gradle.daemon=false`,
  `org.gradle.jvmargs=-Xmx1536m`, `org.gradle.workers.max=2`, `kotlin.daemon.jvmargs=-Xmx1g`. This
  stops the Gradle and Kotlin daemons from staying resident after a build.
- To separate the two suspects, build with `./gradlew :app:assembleRelease` (no adb involved) and
  install the APK in a separate step with `adb install -r`.

The app on the watch is unaffected; it keeps running the last installed build.

## Known limitations and ideas

- The ringing screen opens by itself through a full-screen notification request, which Google
  documents as unsupported on Wear OS. One UI 8 Watch honours it; other watches may not.
- No phone companion yet. A phone app for editing alarms with a real keyboard would use the Wear
  Data Layer API; the standalone design leaves room for it.
- No tile or watch-face complication yet. A tile showing the next alarm would be a natural addition.
- The notification text uses the system font size. Only the app's own screens get the big text.
- The notification panel (swipe right from the watch face) is drawn by the watch, and Wear OS does
  not allow custom notification layouts. The app asks for its yellow there with `setColor` and
  `setColorized`, and for yellow labels and icons on the Dismiss and Snooze buttons. What the watch
  does with those requests is up to One UI Watch; see Status for what has been observed. The
  colours of the watch's own buttons belong to the watch's theme, which no app can change. Wear OS 6
  can take its system colours from the watch face, and a pre-release look at One UI 8 Watch showed a
  colour theme under Settings, Display. Whether the shipped software on the Galaxy Watch5 offers
  either has not been checked.
- Voice control cannot be reached through Bixby or Google Assistant; third-party watch apps have no
  hook into either. The in-app listener is the practical alternative.
- Button snooze only works while the ringing screen is showing. If that screen has not opened, tap
  the notification first. Anything else that takes the user away from the ringing
  screen by their own action, such as a long-press that opens an assistant, also snoozes.

## References

- Wear OS: create your first app, run on a physical watch:
  <https://developer.android.com/training/wearables/get-started/creating>
- Debug Wear OS over Wi-Fi:
  <https://developer.android.com/training/wearables/get-started/debug-wifi>
- Alarms, exact alarms and the `USE_EXACT_ALARM` permission:
  <https://developer.android.com/develop/background-work/services/alarms/schedule>
- Foreground service types (`systemExempted` allowed for exact-alarm apps):
  <https://developer.android.com/develop/background-work/services/fg-service-types>
- Background activity launch restrictions and exemptions:
  <https://developer.android.com/guide/components/activities/background-starts>
- Notifications on Wear OS (full-screen intents unsupported):
  <https://developer.android.com/training/wearables/notifications/creating>
- Developer report that Samsung watch builds honour full-screen notifications (forum thread):
  <https://slack-chats.kotlinlang.org/t/13254400/hi-i-want-to-be-able-to-display-fullscreen-notification-to-d>
- Compose for Wear OS releases: <https://developer.android.com/jetpack/androidx/releases/wear-compose>
- Android Gradle plugin 9 built-in Kotlin: <https://developer.android.com/build/migrate-to-built-in-kotlin>
- Text input on Wear OS, `RemoteInputIntentHelper`:
  <https://developer.android.com/reference/androidx/wear/input/RemoteInputIntentHelper>
