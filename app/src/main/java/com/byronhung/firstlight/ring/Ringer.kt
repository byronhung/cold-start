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

    /** The loud volume right now: the fade-in's level, or full. */
    private fun loudLevel(): Float = if (!gentle) 1f else gentleLevel(SystemClock.elapsedRealtime() - startedAt)

    /** [soundUri]: the alarm's own sound, or null for the phone's default alarm sound. */
    fun start(scope: CoroutineScope, soundUri: String? = null, gentle: Boolean = false) {
        this.scope = scope
        this.gentle = gentle
        startedAt = SystemClock.elapsedRealtime()
        originalVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        holdVolume()
        player = createPlayer(soundUri)
        if (player != null) {
            loudLevel().let { v -> runCatching { player?.setVolume(v, v) } }
            player?.start()
        } else {
            // No ringtone readable (e.g. right after a reboot, before first unlock): a generated tone.
            tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) }.getOrNull()
        }
        vibrate()

        guard = scope.launch {
            while (isActive) {
                if (!quiet) tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
                delay(1_000)
                holdVolume()
            }
        }
        if (gentle) scope.launch {
            // Step the fade-in four times a second until it reaches full.
            while (isActive && loudLevel() < 1f) {
                if (!quiet) loudLevel().let { v -> runCatching { player?.setVolume(v, v) } }
                delay(250)
            }
            if (!quiet) runCatching { player?.setVolume(1f, 1f) }
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
            val from = loudLevel()
            if (p != null) fade = scope?.launch {
                for (i in FADE_STEPS - 1 downTo 0) {
                    val v = from * i.toFloat() / FADE_STEPS
                    runCatching { p.setVolume(v, v) }
                    delay(FADE_MS / FADE_STEPS)
                }
            }
        } else {
            loudLevel().let { v -> runCatching { p?.setVolume(v, v) } }
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
    }

    private fun holdVolume() {
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

    private companion object {

        const val TAG = "Ringer"
        const val VOLUME_FLOOR = 0.8
        const val FADE_MS = 300L
        const val FADE_STEPS = 6
    }
}

/** Gentle start: 10% to full over 30 s. */
const val GENTLE_MS = 30_000L
const val GENTLE_FROM = 0.1f

/** The player volume [elapsedMs] into a gentle start: a straight rise from [GENTLE_FROM] to 1. */
fun gentleLevel(elapsedMs: Long): Float =
    (GENTLE_FROM + (1f - GENTLE_FROM) * elapsedMs.coerceAtLeast(0) / GENTLE_MS).coerceAtMost(1f)
