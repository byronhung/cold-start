package com.coldstart.app.ui.history

import com.coldstart.app.data.WakeLog
import com.coldstart.app.data.WakeOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

class MonthMarksTest {
    private val zone = ZoneOffset.UTC

    private fun wake(day: Int, hour: Int, outcome: WakeOutcome?, missed: Int = 0) = WakeLog(
        alarmId = 1,
        firedAt = LocalDateTime.of(2026, 10, day, hour, 30).toInstant(zone).toEpochMilli(),
        outcome = outcome,
        checksMissed = missed,
    )

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)

    @Test fun `solved with no missed check is up, and so is solving without the scan`() {
        val marks = dayMarks(listOf(wake(1, 6, WakeOutcome.SOLVED), wake(2, 6, WakeOutcome.NO_SCAN)), zone)
        assertEquals(DayMark.UP, marks[oct(1)])
        assertEquals(DayMark.UP, marks[oct(2)])
    }

    @Test fun `a missed wake check makes it rang again`() {
        assertEquals(DayMark.RANG_AGAIN, dayMarks(listOf(wake(3, 6, WakeOutcome.SOLVED, missed = 1)), zone)[oct(3)])
    }

    @Test fun `giving up or leaving it ringing both count as gave up`() {
        val marks = dayMarks(listOf(wake(4, 6, WakeOutcome.GAVE_UP), wake(5, 6, WakeOutcome.TIMED_OUT)), zone)
        assertEquals(DayMark.GAVE_UP, marks[oct(4)])
        assertEquals(DayMark.GAVE_UP, marks[oct(5)])
    }

    @Test fun `two alarms on one day count once, as the worse of the two`() {
        // The missed check rings again, and that re-ring is solved: the day still reads rang again.
        val day = listOf(wake(6, 6, WakeOutcome.SOLVED, missed = 1), wake(6, 7, WakeOutcome.SOLVED))
        assertEquals(DayMark.RANG_AGAIN, dayMarks(day, zone)[oct(6)])
        val worse = listOf(wake(7, 6, WakeOutcome.SOLVED), wake(7, 9, WakeOutcome.GAVE_UP))
        assertEquals(DayMark.GAVE_UP, dayMarks(worse, zone)[oct(7)])
    }

    @Test fun `days with no alarm and unfinished rings have no mark`() {
        val marks = dayMarks(listOf(wake(8, 6, null)), zone)
        assertNull(marks[oct(8)])
        assertNull(marks[oct(9)])
    }

    @Test fun `October 2026 starts on a Thursday, so three blanks`() {
        assertEquals(3, leadingBlanks(YearMonth.of(2026, 10)))
    }

    @Test fun `a month's range ends where the next begins`() {
        val oct = monthRange(YearMonth.of(2026, 10), zone)
        val nov = monthRange(YearMonth.of(2026, 11), zone)
        assertEquals(oct.last + 1, nov.first)
    }
}
