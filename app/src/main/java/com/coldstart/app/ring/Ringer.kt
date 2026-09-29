package com.coldstart.app.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
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

    fun start(scope: CoroutineScope) {
        originalVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        holdVolume()
        player = createPlayer()
        if (player != null) {
            player?.start()
        } else {
            // No ringtone readable (e.g. right after a reboot, before first unlock): a generated tone.
            tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) }.getOrNull()
        }
        @Suppress("DEPRECATION")
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), alarmAttributes)

        guard = scope.launch {
            while (isActive) {
                tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
                delay(1_000)
                holdVolume()
            }
        }
    }

    fun stop() {
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

    private fun createPlayer(): MediaPlayer? {
        val candidates = listOfNotNull(
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
    }
}
