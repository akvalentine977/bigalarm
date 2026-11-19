# Handoff for a local session

Written 19 Nov 2026 by the cloud session that built this app, for a session that picks it up on the
owner's own Linux machine. The README explains what the app does, how it is built and what has been
confirmed on the watch. This file covers what the README does not: the hazards of working on this
particular machine and watch, the agreements made with the owner, and what was learned the hard
way. It is a snapshot. When something here stops being true, change it or delete it.

## Read this before running anything

Three things can do real damage, and none of them is visible in the code.

**A Gradle build may freeze this desktop.** While the owner was building locally, the machine
locked up hard a few minutes after every `./gradlew` run and needed a power cycle. The freezes
stopped when they stopped using Gradle and adb. On 18 Nov the app was installed with `apksigner`
and `adb` alone, and no freeze was reported, which makes Gradle the stronger suspect. That is an
inference. Nothing was ever diagnosed: no journal output and no memory figures were captured. So do
not start a Gradle build as a routine step. Tell the owner first, make sure their other work is
saved, and push your commits beforehand so a freeze costs nothing. The README section "Open issue:
desktop lock-ups on the development machine" lists what to check and an untested set of
lower-memory Gradle settings. The timing fits the Gradle and Kotlin daemons, which stay resident
after a build, so `./gradlew --stop` and a look at leftover `java` processes after a build are
worth doing. If the cause turns out to be found, record it in the README; that would be the most
useful thing a local session could establish.

**The watch may currently be unable to open the ringing screen.** During a test on 18 Nov the owner
denied the app's full-screen permission. They were reminded to restore it but did not confirm that
they had. While it is denied, alarms buzz but the ringing screen does not appear. Check before
anything else:

```bash
adb shell appops get com.akvalentine.bigalarm USE_FULL_SCREEN_INTENT
```

If that reports `deny`, restore it and confirm with a test alarm:

```bash
adb shell appops set com.akvalentine.bigalarm USE_FULL_SCREEN_INTENT default
```

**The alarms exist only on the watch, and only the owner's key can update the app.** The owner's
alarms live in the app's database on the watch. The app has no export feature, and whether the
watch's own backup covers its data has never been checked, so treat an uninstall as deleting them. The installed copy is signed with the owner's release key, so a build signed with
anything else, including any debug build, is refused with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. The
only way past that error is an uninstall. Never uninstall, and never run `installDebug`, without
the owner agreeing to lose their alarms. For the same reason a change to the database schema needs
a Room migration: none is destructive by design, so a missing one crashes the app on start rather
than wiping data.

## What this is and who it is for

Big Alarm is a standalone Wear OS alarm app for the owner's Samsung Galaxy Watch5 (SM-R910, One UI
8 Watch, Wear OS 6, API 36). The owner runs their day on many named alarms. The stock alarm app
caps them at 15 and shows names too small for them to read without glasses. Everything follows
from that: legibility first, then reliability. A missed or silent alarm is the worst failure this
app can have, so weigh any change to the scheduling or ringing path accordingly.

## Where things stand

- **Repository.** `github.com/akvalentine977/bigalarm`, default branch `main`. The project moved
  here on 17 Nov from a repository that no longer exists, and the history was rewritten on the way,
  so clones of the old one cannot pull.
- **On the watch.** A release build of commit `58b3b09`, signed with the owner's key.
- **Confirmed.** The tag `watch-ok-2026-11-18` marks commit `3e070df`. Alarms ring and open the
  ringing screen, both physical buttons snooze, editing an inactive alarm switches it on, and names
  are readable in yellow. The README's Status section is the full record.
- **Accepted as they are.** Voice control does not work on this watch. The notification card comes
  out yellowish but its Dismiss and Snooze buttons stay gray. The owner has said to leave both.
- **Open items.** None.
- **Leftover.** The branch `claude/busy-clarke-x81amk` is unused. Deleting it is the owner's call.

## What you can do that the previous session could not

The cloud session never touched the watch. It built APKs in a sandbox and the owner reported back
in a few words. That has consequences you can now undo.

- **Nobody who wrote this UI has seen it.** Every screen was written from library signatures and the
  owner's descriptions. `adb exec-out screencap -p > shot.png` would be the first real look. Expect
  problems nobody has reported, such as clipping at the largest text size or at the edges of the
  round screen.
- **You can read the logs.** The app logs under the tags `AlarmScheduler`, `AlarmReceiver`,
  `AlarmRingService`, `Ringer`, `AlarmRingActivity` and `VoiceCommandListener`.
- **You can sign, install and tag.** The cloud session could push branches but not tags, and had no
  access to the owner's key.

## Agreements with the owner

- Changes go straight to `main`. There are no review branches or pull requests.
- A build gets an annotated tag `watch-ok-YYYY-MM-DD` only after the owner says it works on the
  watch. The tag message says what was confirmed.
- Before pushing, `./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintDebug` should
  pass, subject to the freeze risk above. There are 31 unit tests. Lint reports two warnings about
  newer versions being available; both are expected, see below.
- Keep the README's Status section truthful. It separates what has been confirmed on the watch from
  what has merely been built, and it is updated whenever the owner reports a result.
- Do what was asked and stop there. Features are added when the owner asks for them.
- The owner wants sources for technical claims, preferably official documentation. The README ends
  with a References section for that reason.
- When the owner has to run something, give exact commands with real paths, one command per block.
  Their clone is at `~/Projects/bigalarm`.

## Building and installing

The README has the full instructions. The short version, once building locally is judged safe:

```bash
./gradlew :app:assembleRelease
```

This signs with the owner's key only if `keystore.properties` and `bigalarm.jks` are present in the
repository root. Both are ignored by git. Without them the build silently falls back to the debug
key and the result will not install over the watch's copy, so check the signature first. The
certificate should name the owner, not "Android Debug":

```bash
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

```bash
adb -s IP:PORT install -r app/build/outputs/apk/release/app-release.apk
```

If building locally stays off the table, the route that has worked is: build elsewhere, re-sign
with `apksigner sign --ks bigalarm.jks --ks-key-alias bigalarm`, then `adb install -r`.

Connecting to the watch has its own quirks:

- Debugging is over Wi-Fi only. The watch has no USB data and Wear OS dropped Bluetooth debugging.
- The port changes every time wireless debugging is switched on. Read the address and port from the
  watch's Wireless debugging screen each time. Pairing normally persists, so reconnecting is enough.
- The watch goes to sleep and switches wireless debugging off. Putting it on its charger keeps it
  awake long enough.
- Stale connections leave several entries in `adb devices`, and adb then refuses to pick one. Name
  the device with `-s IP:PORT`.
- A distribution `adb` and the SDK's `adb` on the same machine kill each other's server. Keep one
  of them first on `PATH`.

## Learned the hard way

**Toolchain**

- Android Gradle Plugin 9.5.0 has built-in Kotlin. The `kotlin-android` plugin must not be applied.
- The `kotlin` version in `gradle/libs.versions.toml` is the Compose compiler plugin's version and
  must equal the Kotlin version that AGP bundles, currently 2.2.10. Lint's notice that a newer one
  exists is expected. Do not raise it on its own; raise it when AGP is raised.
- KSP is 2.3.13 because the KSP release that AGP bundles is rejected by built-in Kotlin's rules on
  source sets.
- The SDK platform package is named `platforms;android-37.0`, with the minor version.
- Overriding `dispatchKeyEvent` in an AndroidX activity fails lint. Use `onKeyDown` and `onKeyUp`.
- The `core` package is pure Kotlin so its tests run on the JVM. Keep Android types out of it.

**This watch**

- The ringing screen opens because One UI 8 Watch honours the notification's full-screen request,
  although Google's documentation says Wear OS does not support it. The overlay grant described in
  older notes is not needed.
- Apps targeting API 36 no longer receive the Back button as a key event. The ringing activity opts
  out of that in the manifest so that a press can snooze.
- The Home button never reaches an app. The ringing screen snoozes on `onUserLeaveHint` instead, and
  the service opens that screen with `FLAG_ACTIVITY_NO_USER_ACTION` so its own launch does not count
  as the user leaving.
- The notification panel is drawn by the watch. Custom layouts are stripped. The colour request
  tints the card and nothing else, and an app cannot change the watch's own theme colours.
- Debug builds are noticeably sluggish on the watch. Release builds are not.

## Never exercised on the watch

None of these has been reported on. Treat them as unverified, not as broken.

- Alarm sound, including the fallback tones for a watch with no alarm ringtone.
- The every-N-days rule and one-off alarms on a chosen date.
- Two alarms in the same minute, which should queue.
- The auto-silence timeout and its "Missed" notification, and ring-until-dismissed.
- Re-registering alarms after a reboot or a time zone change.
- Behaviour under Do Not Disturb, bedtime mode and power saving.

## Parked, and ideas nobody has asked for

Voice control is parked by the owner's decision. If they ask to revisit it, start with the "Test
voice control" screen in the app's Settings. It lists the watch's speech services and shows what
the recogniser hears, and the owner never reported what it displayed.

These were noted along the way. None has been requested, so raise them rather than build them:

- A way to export or back up alarms, given that an uninstall loses them.
- A build in CI, so that an APK can be produced without running Gradle on this machine.
- A tile or watch-face complication showing the next alarm.
- An ongoing-activity indicator on the watch face that leads back to the ringing screen.
- A phone companion for editing alarms with a real keyboard.
