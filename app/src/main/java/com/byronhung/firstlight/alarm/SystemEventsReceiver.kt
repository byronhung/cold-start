package com.byronhung.firstlight.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.byronhung.firstlight.FirstLightApp
import kotlinx.coroutines.launch

/**
 * Android forgets every scheduled alarm on reboot, and a clock or time-zone change moves when
 * "06:30" is. Any of these → reschedule everything from the database.
 *
 * LOCKED_BOOT_COMPLETED arrives before the first unlock, which is why the database lives in
 * device-protected storage: a phone that restarts at 3am still rings at 6:30.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        val app = context.applicationContext as FirstLightApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                app.repository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
