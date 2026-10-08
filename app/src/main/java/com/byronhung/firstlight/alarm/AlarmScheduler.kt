package com.byronhung.firstlight.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.byronhung.firstlight.MainActivity
import com.byronhung.firstlight.data.Alarm
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Talks to Android's AlarmManager. Only [com.byronhung.firstlight.data.AlarmRepository] calls this, so
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

    /** A wake check [WakeCheck.delayMs] from now. */
    fun scheduleWakeCheck(check: WakeCheck, now: Long = System.currentTimeMillis()) =
        setClock(now + WakeCheck.delayMs, checkIntent(check, WakeCheckReceiver.ACTION_CHECK))

    /** The end of a check's answer window: if this fires, the check was missed. */
    fun scheduleCheckDeadline(check: WakeCheck, at: Long) =
        setClock(at, checkIntent(check, WakeCheckReceiver.ACTION_DEADLINE))

    fun cancelWakeCheck(check: WakeCheck) {
        alarmManager.cancel(checkIntent(check, WakeCheckReceiver.ACTION_CHECK))
        alarmManager.cancel(checkIntent(check, WakeCheckReceiver.ACTION_DEADLINE))
    }

    fun cancelCheckDeadline(check: WakeCheck) {
        alarmManager.cancel(checkIntent(check, WakeCheckReceiver.ACTION_DEADLINE))
    }

    // setAlarmClock for checks too: it is the alarm type Doze can't delay, and it lets the
    // receiver re-ring the alarm from the background.
    private fun setClock(at: Long, operation: PendingIntent) {
        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent()), operation)
        } catch (e: SecurityException) {
            Log.w(TAG, "Can't schedule wake check", e)
        }
    }

    private fun checkIntent(check: WakeCheck, action: String): PendingIntent {
        val kind = if (action == WakeCheckReceiver.ACTION_CHECK) 0 else 1
        return PendingIntent.getBroadcast(
            context,
            CHECK_REQUEST_BASE + ((check.wakeId * 8 + check.index * 2 + kind) % 1_000_000).toInt(),
            WakeCheckReceiver.intent(context, check, action)
                .setData(Uri.parse("firstlight://check/${check.wakeId}/${check.index}/$kind")),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun canScheduleExact(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()

    /** One PendingIntent per alarm: the data URI makes each one distinct to Android. */
    private fun fireIntent(alarmId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        alarmId.toInt(),
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .setData(Uri.parse("firstlight://alarm/$alarmId"))
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
        const val CHECK_REQUEST_BASE = 1_000_000
    }
}
