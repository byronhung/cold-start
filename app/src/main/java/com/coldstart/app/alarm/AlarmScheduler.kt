package com.coldstart.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.coldstart.app.MainActivity
import com.coldstart.app.data.Alarm
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Talks to Android's AlarmManager. Only [com.coldstart.app.data.AlarmRepository] calls this, so
 * the database and the system's alarms can't drift apart.
 *
 * setAlarmClock is the strongest alarm Android has: exempt from Doze, shows the alarm icon in the
 * status bar, and lets the ring service start from the background.
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarm: Alarm, now: LocalDateTime = LocalDateTime.now()) {
        val at = alarm.nextTrigger(now).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent()), fireIntent(alarm.id))
        } catch (e: SecurityException) {
            // Exact-alarm permission missing. The setup card on the list screen asks for it.
            Log.w(TAG, "Can't schedule alarm ${alarm.id}", e)
        }
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(fireIntent(alarmId))
    }

    fun canScheduleExact(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()

    /** One PendingIntent per alarm: the data URI makes each one distinct to Android. */
    private fun fireIntent(alarmId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        alarmId.toInt(),
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .setData(Uri.parse("coldstart://alarm/$alarmId"))
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** What opens if you tap the alarm icon in the status bar. */
    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val TAG = "AlarmScheduler"
    }
}
