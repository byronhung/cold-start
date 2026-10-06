package com.coldstart.app.alarm

import android.content.Context

/**
 * The one wake check that's currently waiting, if any. Only one at a time: solving a second alarm
 * replaces the first alarm's check, and any alarm starting to ring cancels it (the new ring takes
 * over). Kept in device-protected storage, like the database, so it survives a restart too.
 */
class PendingCheckStore(context: Context) {
    private val prefs = context.createDeviceProtectedStorageContext()
        .getSharedPreferences("wake_checks", Context.MODE_PRIVATE)

    fun get(): WakeCheck? {
        val wake = prefs.getLong(WAKE, -1L)
        if (wake < 0) return null
        return WakeCheck(
            alarmId = prefs.getLong(ALARM, -1L),
            wakeId = wake,
            index = prefs.getInt(INDEX, 1),
            total = prefs.getInt(TOTAL, 1),
        )
    }

    fun set(check: WakeCheck) {
        prefs.edit()
            .putLong(ALARM, check.alarmId)
            .putLong(WAKE, check.wakeId)
            .putInt(INDEX, check.index)
            .putInt(TOTAL, check.total)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val ALARM = "alarmId"
        const val WAKE = "wakeId"
        const val INDEX = "index"
        const val TOTAL = "total"
    }
}
