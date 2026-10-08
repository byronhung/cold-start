package com.byronhung.firstlight.ui.history

import com.byronhung.firstlight.data.WakeLog
import com.byronhung.firstlight.data.WakeOutcome
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** How a day went, for the month calendar. A day with no alarm has no mark. Worse marks rank higher. */
enum class DayMark {
    /** Every alarm that day solved, and no wake check missed. */
    UP,

    /** Solved, but fell back asleep: a wake check was missed and it rang again. */
    RANG_AGAIN,

    /** Given up with the 30 s hold, or left ringing for the hour. */
    GAVE_UP,
}

/** One wake's mark, or null for a ring that never finished (the process was killed). */
fun markOf(wake: WakeLog): DayMark? = when (wake.outcome) {
    WakeOutcome.SOLVED, WakeOutcome.NO_SCAN -> if (wake.checksMissed > 0) DayMark.RANG_AGAIN else DayMark.UP
    WakeOutcome.GAVE_UP, WakeOutcome.TIMED_OUT -> DayMark.GAVE_UP
    null -> null
}

/**
 * Each day's mark, by the day the alarm rang. Several alarms on one day count once, as the worst of
 * them: a day is only "up" if every alarm that day was.
 */
fun dayMarks(wakes: List<WakeLog>, zone: ZoneId): Map<LocalDate, DayMark> {
    val out = HashMap<LocalDate, DayMark>()
    for (w in wakes) {
        val mark = markOf(w) ?: continue
        val day = Instant.ofEpochMilli(w.firedAt).atZone(zone).toLocalDate()
        val had = out[day]
        if (had == null || mark > had) out[day] = mark
    }
    return out
}

/** The epoch-millis range [start, end) a month covers in [zone]. */
fun monthRange(month: YearMonth, zone: ZoneId): LongRange {
    val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start until end
}

/** Blank cells before the 1st in a Monday-first grid. */
fun leadingBlanks(month: YearMonth): Int = month.atDay(1).dayOfWeek.value - 1
