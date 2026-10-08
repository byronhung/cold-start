package com.byronhung.firstlight.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.byronhung.firstlight.FirstLightApp
import com.byronhung.firstlight.ring.Notifications
import com.byronhung.firstlight.ring.RingActivity
import com.byronhung.firstlight.ring.RingService
import com.byronhung.firstlight.ring.WakeCheckActivity
import kotlinx.coroutines.launch

/**
 * Two moments of a wake check:
 * - ACTION_CHECK, [WakeCheck.delayMs] after the solve: pass it silently if the phone is in use,
 *   otherwise show "Still awake?" and set the deadline.
 * - ACTION_DEADLINE, [WakeCheck.WINDOW_MS] later: only fires if nobody answered (answering cancels
 *   it). Rings the alarm again.
 */
class WakeCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val check = intent.toCheck() ?: return
        val app = context.applicationContext as FirstLightApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_CHECK -> onCheck(context, app, check)
                    ACTION_DEADLINE -> onDeadline(context, app, check)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun onCheck(context: Context, app: FirstLightApp, check: WakeCheck) {
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val current = app.repository.isPendingCheck(check)
        when (decideCheck(app.ringController.isActive, power.isInteractive, keyguard.isKeyguardLocked, current)) {
            CheckAction.SKIP -> Unit
            CheckAction.PASS_SILENTLY -> app.repository.checkPassed(check)
            CheckAction.ASK -> {
                val deadline = System.currentTimeMillis() + WakeCheck.WINDOW_MS
                app.scheduler.scheduleCheckDeadline(check, deadline)
                context.getSystemService(NotificationManager::class.java)
                    .notify(Notifications.ID_CHECK, Notifications.wakeCheck(context, check, deadline))
                // Like the alarm itself: open the screen directly where Android allows it.
                runCatching {
                    context.startActivity(
                        WakeCheckActivity.intent(context, check, deadline)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
                    )
                }
            }
        }
    }

    private suspend fun onDeadline(context: Context, app: FirstLightApp, check: WakeCheck) {
        // Replaced or cancelled since it was asked: nothing to ring.
        if (!app.repository.isPendingCheck(check)) return
        Notifications.cancelWakeCheck(context)
        app.repository.checkMissed(check)
        ContextCompat.startForegroundService(context, RingService.startIntent(context, check.alarmId, isRering = true))
        runCatching {
            context.startActivity(
                Intent(context, RingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            )
        }
    }

    companion object {
        const val ACTION_CHECK = "com.byronhung.firstlight.WAKE_CHECK"
        const val ACTION_DEADLINE = "com.byronhung.firstlight.WAKE_CHECK_DEADLINE"

        fun intent(context: Context, check: WakeCheck, action: String): Intent =
            Intent(context, WakeCheckReceiver::class.java).setAction(action).putCheck(check)
    }
}

private const val EXTRA_ALARM = "check.alarmId"
private const val EXTRA_WAKE = "check.wakeId"
private const val EXTRA_INDEX = "check.index"
private const val EXTRA_TOTAL = "check.total"

fun Intent.putCheck(check: WakeCheck): Intent = this
    .putExtra(EXTRA_ALARM, check.alarmId)
    .putExtra(EXTRA_WAKE, check.wakeId)
    .putExtra(EXTRA_INDEX, check.index)
    .putExtra(EXTRA_TOTAL, check.total)

fun Intent.toCheck(): WakeCheck? {
    val wake = getLongExtra(EXTRA_WAKE, -1L)
    if (wake < 0) return null
    return WakeCheck(
        alarmId = getLongExtra(EXTRA_ALARM, -1L),
        wakeId = wake,
        index = getIntExtra(EXTRA_INDEX, 1),
        total = getIntExtra(EXTRA_TOTAL, 1),
    )
}
