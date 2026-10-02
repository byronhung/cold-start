package com.coldstart.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.coldstart.app.alarm.WakeCheck
import com.coldstart.app.data.Alarm
import com.coldstart.app.data.WakeOutcome
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Debug builds only (src/debug). Lets a test drive the real alarm path from adb:
 *
 *   adb shell am broadcast -n com.coldstart.app/.DebugReceiver -a com.coldstart.app.debug.ADD --ei minutes 1
 *   adb shell am broadcast -n com.coldstart.app/.DebugReceiver -a com.coldstart.app.debug.STOP
 *   adb shell am broadcast -n com.coldstart.app/.DebugReceiver -a com.coldstart.app.debug.SOLVE   (as if all rounds were solved: starts wake checks)
 *
 * ADD also takes --ei checks N (wake checks, default 0), --ei checkDelaySec S (seconds between
 * checks, instead of 5 minutes; lasts until the app process dies) and --ez scan true.
 *
 * ADD saves a one-off alarm through the repository, so it's scheduled exactly like a real one.
 */
class DebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as ColdStartApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_ADD -> {
                        val at = LocalDateTime.now().plusMinutes(intent.getIntExtra("minutes", 1).toLong())
                        if (intent.hasExtra("checkDelaySec")) {
                            WakeCheck.delayOverrideMs = intent.getIntExtra("checkDelaySec", 300) * 1000L
                        }
                        val id = app.repository.save(
                            Alarm(
                                hour = at.hour,
                                minute = at.minute,
                                label = "Test",
                                wakeChecks = intent.getIntExtra("checks", 0),
                                finishWithScan = intent.getBooleanExtra("scan", false),
                            ),
                        )
                        Log.i(TAG, "Added test alarm $id at ${at.hour}:${at.minute}")
                    }
                    ACTION_SOLVE -> {
                        app.ringController.finish(WakeOutcome.SOLVED, emptyList())
                        Log.i(TAG, "Solved ring")
                    }
                    ACTION_STOP -> {
                        app.ringController.finish(WakeOutcome.GAVE_UP, emptyList())
                        Log.i(TAG, "Stopped ring")
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "ColdStartDebug"
        const val ACTION_ADD = "com.coldstart.app.debug.ADD"
        const val ACTION_STOP = "com.coldstart.app.debug.STOP"
        const val ACTION_SOLVE = "com.coldstart.app.debug.SOLVE"
    }
}
