<p align="center"><img src="icons/cold-start-logo.svg" width="120" alt="Cold Start logo"></p>

# Cold Start

An Android alarm with one way out: solve three quick puzzles. No snooze.

- **Three rounds, three different puzzles, shuffled every morning.** Stroop (tap the ink colour, not
  the word), pattern flash (tap back the cells that lit up), and odd one out (find the one shape with
  a colour + shape + size combination nothing else has).
- **Difficulty comes from reps, not hardness.** Each round is easy. It adapts quietly to how fast
  you've been solving.
- **Yesterday's first puzzle never opens today.**
- **Turning the volume down doesn't help.** While it rings, the alarm volume is held up and the
  volume buttons do nothing.
- **The only other exit is holding a button for 30 seconds.** Every give-up is logged in History.

## Install

1. Download [`apk/ColdStart-0.1.apk`](apk/ColdStart-0.1.apk) on an Android phone (Android 10 or newer).
2. Open it. Android will ask you to allow installing from that app (Files, Chrome, WhatsApp…). Allow it.
   It will also warn that the app isn't from the Play Store; that's expected.
3. Open Cold Start and tap **Allow** on everything in the *Before alarms can ring* card,
   especially battery. Without that, some phones put the app to sleep overnight.

## Build it yourself

Android Studio, JDK 17. `./gradlew installDebug` with a phone connected. Unit tests:
`./gradlew testDebugUnitTest`.

Kotlin, Jetpack Compose, Room. Alarms use `AlarmManager.setAlarmClock`; the ring runs in a
foreground service and a full-screen activity over the lock screen. The database lives in
device-protected storage so alarms survive a reboot before the phone is unlocked.

Fonts: Bricolage Grotesque, Instrument Sans, JetBrains Mono, all under the SIL Open Font License
(see `licenses/`).
