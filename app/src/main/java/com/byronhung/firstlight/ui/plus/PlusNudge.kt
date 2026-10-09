package com.byronhung.firstlight.ui.plus

import com.byronhung.firstlight.ui.history.DayMark
import java.time.LocalDate

/**
 * When the Plus popup may show (decisions log, 9 Oct): only to people without Plus who finished
 * the welcome; only after their last three mornings all went well; at most once every 10 days;
 * and never again after three "Not now"s. The alarm list is the only screen that asks.
 */
object PlusNudge {
    const val GAP_MS = 10L * 24 * 60 * 60 * 1000
    const val MAX_DISMISSALS = 3
    const val GOOD_MORNINGS = 3

    /** Debug builds only: show the popup on the next visit to the alarm list, rules aside. */
    @Volatile var forceNextForDebug = false

    fun shouldShow(
        isPlus: Boolean,
        welcomeDone: Boolean,
        lastShownAt: Long,
        dismissals: Int,
        marks: Map<LocalDate, DayMark>,
        now: Long,
    ): Boolean {
        if (isPlus || !welcomeDone || dismissals >= MAX_DISMISSALS) return false
        if (lastShownAt > 0 && now - lastShownAt < GAP_MS) return false
        val recent = marks.entries.sortedByDescending { it.key }.take(GOOD_MORNINGS)
        return recent.size == GOOD_MORNINGS && recent.all { it.value == DayMark.UP }
    }
}
