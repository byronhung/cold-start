<p align="center"><img src="icons/first-light-logo.svg" width="120" alt="First Light logo"></p>

# First Light

On Google Play (soon) as **First Light: QR & Puzzle Alarm**.

An Android alarm you can only turn off by solving puzzles, or by getting up and scanning a barcode.
No snooze.

- **A few quick rounds, shuffled every morning.** Stroop (tap the ink colour or the word, whichever
  it asks), pattern flash (tap back the cells that lit up), and odd one out (find the one shape with
  a colour + shape + size combination nothing else has).
- **Difficulty per alarm.** Gentle (3 easy rounds), Normal (5 rounds that adapt to how fast you've
  been solving), Hard (4 rounds at the top level). Set a default in Settings.
- **Or scan to stop it.** Register any barcode in the house (a shampoo bottle works) and an alarm can
  ask for it instead of, or after, the puzzles. Out and about? *Can't scan now?* swaps the scan for
  2 hard puzzles, logged in History.
- **Quiet while you solve.** Tapping the screen silences it; stop for 10 seconds and it's back at
  full volume. For the alarm you forgot about that goes off on the train.
- **Wake checks.** A few minutes after you solve it, it asks "Still awake?". Miss it and the alarm
  rings again.
- **Yesterday's first puzzle never opens today.**
- **Turning the volume down doesn't help.** While it rings, the alarm volume is held up and the
  volume buttons do nothing.
- **The only other exit is holding a button for 30 seconds.** Every give-up is logged in History.
- **Your month at a glance.** History shows a calendar of mornings (up, rang again, gave up) and the
  time you were actually up each day.
- **Tidy up fast.** Long-press an alarm to select several and delete them at once, with Undo.
- **Gentle start.** Fades in over 30 seconds (per alarm). A re-ring after a missed wake check starts loud.
- **Test this alarm.** Ring it from the editor, exactly as set, without logging anything.
- **Skip next.** Skip just the coming ring of a repeating alarm, for a holiday.
- **Kind on calls.** An alarm that goes off mid-call rings softly, then full once you hang up.
- **Light or dark.** By the hour, always light, always dark, or match your phone. The sky still moves
  through the day either way.
- **Colour-blind friendly.** Puzzle colours are checked against the three main kinds of colour blindness.
- **A short welcome** on first launch: try a puzzle, a wake check, register a code.

*Coming with First Light Plus (one payment, not on sale yet): three more puzzles and four skies you can
touch: Aurora, Monsoon, Neon city and Coast.*

## Install

1. Download [`apk/FirstLight-0.6.apk`](apk/FirstLight-0.6.apk) on an Android phone (Android 10 or newer).
2. Open it. Android will ask you to allow installing from that app (Files, Chrome, WhatsApp…). Allow it.
   It will also warn that the app isn't from the Play Store; that's expected.
3. Open First Light and tap **Allow** on everything in the *Before alarms can ring* card,
   especially battery. Without that, some phones put the app to sleep overnight.

## Build it yourself

Android Studio, JDK 17. `./gradlew installDebug` with a phone connected. Unit tests:
`./gradlew testDebugUnitTest`.

Kotlin, Jetpack Compose, Room. Alarms use `AlarmManager.setAlarmClock`; the ring runs in a
foreground service and a full-screen activity over the lock screen. The database lives in
device-protected storage so alarms survive a reboot before the phone is unlocked.

Fonts: Bricolage Grotesque, Instrument Sans, JetBrains Mono, all under the SIL Open Font License
(see `licenses/`).
