package com.byronhung.firstlight.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * The noise: alarm tone, vibration, and the volume lock.
 *
 * Everything plays on the ALARM stream, which silent mode and Do Not Disturb (by default) don't
 * mute. The volume lock holds that stream at 80% or more for as long as it rings, checking once a
 * second, so turning it down in Settings bounces straight back. The original volume is restored
 * when the ring ends.
 *
 * [setQuiet] silences the sound and vibration while the puzzle is being solved (see
 * [QuietWhileSolving]). The volume lock keeps running underneath, so it comes back at full volume.
 *
 * On a call (or with a call ringing in): no volume lock and only a soft ring, so the alarm isn't
 * blasted into someone's ear. The moment the call ends it's back to full, lock and all.
 *
 * The original volume is kept on disk (device-protected, readable before first unlock) for as long
 * as it rings, so if Android kills the app mid-ring, [restoreAfterCrash] puts it back next start.
 *
 * Gentle start: the player's own volume rises from 10% to 100% over [GENTLE_MS] while the stream
 * stays locked, so the fade can't be undone by a volume key. Coming back from quiet returns to
 * wherever the fade has reached. The generated fallback tone has no volume control and stays loud.
 */
class Ringer(private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val alarmAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var player: MediaPlayer? = null
    private var tone: ToneGenerator? = null
    private var guard: Job? = null
    private var originalVolume = -1
    private var scope: CoroutineScope? = null
    private var quiet = false
    private var fade: Job? = null
    private var startedAt = 0L
    private var gentle = false

    private var wasInCall = false

    /** The loud volume right now: the fade-in's level, or full. */
    private fun loudLevel(): Float = if (!gentle) 1f else gentleLevel(SystemClock.elapsedRealtime() - startedAt)

    /** On a call, in a voice/video chat, or with a call ringing in. */
    private fun inCall(): Boolean = audio.mode.let {
        it == AudioManager.MODE_IN_CALL || it == AudioManager.MODE_IN_COMMUNICATION || it == AudioManager.MODE_RINGTONE
    }

    /** What the player plays at when it isn't quieted: the loud level, held down during a call. */
    private fun level(): Float = if (inCall()) minOf(loudLevel(), IN_CALL_LEVEL) else loudLevel()

    private val prefs get() = context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** [soundUri]: the alarm's own sound, or null for the phone's default alarm sound. */
    fun start(scope: CoroutineScope, soundUri: String? = null, gentle: Boolean = false) {
        this.scope = scope
        this.gentle = gentle
        startedAt = SystemClock.elapsedRealtime()
        // A volume left over from a ring that never finished is the real original.
        originalVolume = prefs.getInt(KEY_ORIGINAL, -1).takeIf { it >= 0 } ?: audio.getStreamVolume(AudioManager.STREAM_ALARM)
        prefs.edit().putInt(KEY_ORIGINAL, originalVolume).commit()
        wasInCall = inCall()
        holdVolume()
        player = createPlayer(soundUri)
        if (player != null) {
            level().let { v -> runCatching { player?.setVolume(v, v) } }
            player?.start()
        } else {
            // No ringtone readable (e.g. right after a reboot, before first unlock): a generated tone.
            tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) }.getOrNull()
        }
        vibrate()

        guard = scope.launch {
            while (isActive) {
                // The fallback tone has no volume of its own, so it simply waits out a call.
                if (!quiet && !inCall()) tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
                delay(1_000)
                val call = inCall()
                if (call != wasInCall) {
                    wasInCall = call
                    Log.d(TAG, if (call) "call: ringing softly" else "call ended: full volume")
                    if (!quiet) level().let { v -> runCatching { player?.setVolume(v, v) } }
                }
                holdVolume()
            }
        }
        if (gentle) scope.launch {
            // Step the fade-in four times a second until it reaches full.
            while (isActive && loudLevel() < 1f) {
                if (!quiet) level().let { v -> runCatching { player?.setVolume(v, v) } }
                delay(250)
            }
            if (!quiet) level().let { v -> runCatching { player?.setVolume(v, v) } }
        }
    }

    /** Quiet fades out over [FADE_MS]; loud comes back at full volume at once. */
    fun setQuiet(quiet: Boolean) {
        if (quiet == this.quiet) return
        this.quiet = quiet
        Log.d(TAG, if (quiet) "quiet: puzzle tapped" else "loud: no tap for a while")
        fade?.cancel()
        val p = player
        if (quiet) {
            vibrator?.cancel()
            tone?.stopTone()
            val from = level()
            if (p != null) fade = scope?.launch {
                for (i in FADE_STEPS - 1 downTo 0) {
                    val v = from * i.toFloat() / FADE_STEPS
                    runCatching { p.setVolume(v, v) }
                    delay(FADE_MS / FADE_STEPS)
                }
            }
        } else {
            level().let { v -> runCatching { p?.setVolume(v, v) } }
            vibrate()
        }
    }

    private fun vibrate() {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), alarmAttributes)
    }

    fun stop() {
        fade?.cancel()
        guard?.cancel()
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
        tone?.release()
        tone = null
        vibrator?.cancel()
        if (originalVolume >= 0) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0) }
        }
        prefs.edit().remove(KEY_ORIGINAL).commit()
    }

    private fun holdVolume() {
        if (inCall()) return
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val floor = ceil(max * VOLUME_FLOOR).toInt()
        if (audio.getStreamVolume(AudioManager.STREAM_ALARM) < floor) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, floor, 0) }
        }
    }

    /**
     * The alarm's own sound first. If it can't be read (deleted, or the phone restarted and hasn't
     * been unlocked yet), the phone's default alarm sound, then its ringtone, then a generated tone.
     */
    private fun createPlayer(soundUri: String?): MediaPlayer? {
        val candidates = listOfNotNull(
            soundUri?.let { runCatching { Uri.parse(it) }.getOrNull() },
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
        )
        for (uri in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(alarmAttributes)
                mp.setDataSource(context, uri)
                mp.isLooping = true
                mp.prepare()
                return mp
            } catch (e: Exception) {
                Log.w(TAG, "Can't play $uri", e)
                mp.release()
            }
        }
        return null
    }

    companion object {
        /** How loud the player is during a call: present, not painful. */
        private const val IN_CALL_LEVEL = 0.15f
        private const val PREFS = "ringer"
        private const val KEY_ORIGINAL = "originalAlarmVolume"

        /**
         * The app was killed mid-ring last time, so the alarm volume is still held up: put it back.
         * Called on every app start, before any ring can begin.
         */
        fun restoreAfterCrash(context: Context) {
            val prefs = context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val original = prefs.getInt(KEY_ORIGINAL, -1)
            if (original < 0) return
            runCatching { context.getSystemService(AudioManager::class.java).setStreamVolume(AudioManager.STREAM_ALARM, original, 0) }
            prefs.edit().remove(KEY_ORIGINAL).commit()
        }


        private const val TAG = "Ringer"
        private const val VOLUME_FLOOR = 0.8
        private const val FADE_MS = 300L
        private const val FADE_STEPS = 6
    }
}

/** Gentle start: 10% to full over 30 s. */
const val GENTLE_MS = 30_000L
const val GENTLE_FROM = 0.1f

/** The player volume [elapsedMs] into a gentle start: a straight rise from [GENTLE_FROM] to 1. */
fun gentleLevel(elapsedMs: Long): Float =
    (GENTLE_FROM + (1f - GENTLE_FROM) * elapsedMs.coerceAtLeast(0) / GENTLE_MS).coerceAtMost(1f)
