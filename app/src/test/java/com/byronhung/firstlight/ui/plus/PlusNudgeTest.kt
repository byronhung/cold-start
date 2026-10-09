package com.byronhung.firstlight.ui.plus

import com.byronhung.firstlight.ui.history.DayMark
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlusNudgeTest {
    private val now = 1_800_000_000_000L
    private fun day(d: Int) = LocalDate.of(2026, 10, d)
    private val threeGood = mapOf(day(7) to DayMark.UP, day(8) to DayMark.UP, day(9) to DayMark.UP)

    private fun show(
        isPlus: Boolean = false,
        welcomeDone: Boolean = true,
        lastShownAt: Long = 0,
        dismissals: Int = 0,
        marks: Map<LocalDate, DayMark> = threeGood,
    ) = PlusNudge.shouldShow(isPlus, welcomeDone, lastShownAt, dismissals, marks, now)

    @Test fun `three good mornings in a row asks`() = assertTrue(show())

    @Test fun `fewer than three mornings doesn't ask`() =
        assertFalse(show(marks = mapOf(day(8) to DayMark.UP, day(9) to DayMark.UP)))

    @Test fun `any of the last three not clean doesn't ask`() {
        assertFalse(show(marks = threeGood + (day(8) to DayMark.RANG_AGAIN)))
        assertFalse(show(marks = threeGood + (day(9) to DayMark.GAVE_UP)))
    }

    @Test fun `only the latest three count`() =
        assertTrue(show(marks = threeGood + (day(1) to DayMark.GAVE_UP)))

    @Test fun `never with Plus or before the welcome is done`() {
        assertFalse(show(isPlus = true))
        assertFalse(show(welcomeDone = false))
    }

    @Test fun `ten days apart at most`() {
        assertFalse(show(lastShownAt = now - PlusNudge.GAP_MS + 1))
        assertTrue(show(lastShownAt = now - PlusNudge.GAP_MS))
    }

    @Test fun `three Not nows and it stops for good`() {
        assertTrue(show(dismissals = 2))
        assertFalse(show(dismissals = 3))
    }
}
