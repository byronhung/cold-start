package com.coldstart.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.coldstart.app.ring.RingActivity
import com.coldstart.app.ring.RingService

/** AlarmManager fires this at the alarm's time. It hands straight over to the ring service. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return
        ContextCompat.startForegroundService(context, RingService.startIntent(context, alarmId))
        // Also open the ringing screen directly. On a locked phone the notification's full-screen
        // intent does this anyway; on a phone in use, Android would otherwise show only a banner.
        // An alarm-clock broadcast is allowed to do this; if Android refuses, the banner remains.
        runCatching {
            context.startActivity(
                Intent(context, RingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            )
        }
    }

    companion object {
        const val ACTION_FIRE = "com.coldstart.app.FIRE"
        const val EXTRA_ALARM_ID = "alarmId"
    }
}
