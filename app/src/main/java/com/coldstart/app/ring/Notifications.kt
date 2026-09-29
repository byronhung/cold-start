package com.coldstart.app.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import com.coldstart.app.R
import com.coldstart.app.alarm.formatTime

object Notifications {
    private const val CHANNEL_RINGING = "ringing"
    const val ID_RINGING = 1

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_RINGING, "Ringing alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Shown while an alarm is ringing."
            // The ring service plays the sound and vibration itself, on the alarm stream.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * The full-screen intent is what puts the ringing screen over the lock screen. If the phone is
     * unlocked and in use, Android shows it as a banner instead; tapping it opens the same screen.
     */
    fun ringing(context: Context, session: RingSession?): Notification {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, RingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = session?.let {
            val time = formatTime(it.hour, it.minute, DateFormat.is24HourFormat(context))
            if (it.label.isBlank()) time else "$time · ${it.label}"
        } ?: "Alarm"
        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText("Solve the puzzles to stop it")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .build()
    }
}
