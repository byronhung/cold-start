package com.coldstart.app.alarm

import com.coldstart.app.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class NextTriggerTest {

    // 2026-10-02 is a Friday.
    private val friday2241 = LocalDateTime.of(2026, 10, 2, 22, 41)

    private val monday = 1 shl 0
    private val saturday = 1 shl 5

    @Test fun `one-off alarm later today rings today`() {
        val now = LocalDateTime.of(2026, 10, 2, 5, 0)
        assertEquals(LocalDateTime.of(2026, 10, 2, 6, 30), nextTrigger(6, 30, Weekdays.NONE, now))
    }

    @Test fun `one-off alarm already passed rings tomorrow`() {
        assertEquals(LocalDateTime.of(2026, 10, 3, 6, 30), nextTrigger(6, 30, Weekdays.NONE, friday2241))
    }

    @Test fun `alarm set for exactly now rings tomorrow, not immediately`() {
        val now = LocalDateTime.of(2026, 10, 2, 6, 30)
        assertEquals(LocalDateTime.of(2026, 10, 3, 6, 30), nextTrigger(6, 30, Weekdays.NONE, now))
    }

    @Test fun `seconds past the alarm minute count as passed`() {
        val now = LocalDateTime.of(2026, 10, 2, 6, 30, 1)
        assertEquals(LocalDateTime.of(2026, 10, 3, 6, 30), nextTrigger(6, 30, Weekdays.NONE, now))
    }

    @Test fun `weekday alarm on Friday night skips the weekend`() {
        assertEquals(LocalDateTime.of(2026, 10, 5, 6, 30), nextTrigger(6, 30, Weekdays.WEEKDAYS, friday2241))
    }

    @Test fun `weekend alarm on Friday night rings Saturday`() {
        assertEquals(LocalDateTime.of(2026, 10, 3, 8, 15), nextTrigger(8, 15, Weekdays.WEEKEND, friday2241))
    }

    @Test fun `single-day alarm whose time passed today rings in a week`() {
        val mondayNoon = LocalDateTime.of(2026, 10, 5, 12, 0)
        assertEquals(LocalDateTime.of(2026, 10, 12, 6, 30), nextTrigger(6, 30, monday, mondayNoon))
    }

    @Test fun `single-day alarm later today rings today`() {
        val saturdayDawn = LocalDateTime.of(2026, 10, 3, 5, 0)
        assertEquals(LocalDateTime.of(2026, 10, 3, 8, 15), nextTrigger(8, 15, saturday, saturdayDawn))
    }

    @Test fun `summary matches the brief's example`() {
        val alarm = Alarm(hour = 6, minute = 30, repeatDays = Weekdays.NONE)
        assertEquals("Next: tomorrow at 06:30 · in 7 h 49 m", nextAlarmSummary(listOf(alarm), friday2241))
    }

    @Test fun `summary picks the soonest enabled alarm and ignores disabled ones`() {
        val alarms = listOf(
            Alarm(id = 1, hour = 5, minute = 45, enabled = false),
            Alarm(id = 2, hour = 8, minute = 15, repeatDays = Weekdays.WEEKEND),
            Alarm(id = 3, hour = 6, minute = 30, repeatDays = Weekdays.WEEKDAYS),
        )
        assertEquals("Next: tomorrow at 08:15 · in 9 h 34 m", nextAlarmSummary(alarms, friday2241))
    }

    @Test fun `summary is null when every alarm is off`() {
        assertNull(nextAlarmSummary(listOf(Alarm(hour = 6, minute = 30, enabled = false)), friday2241))
    }

    @Test fun `summary under an hour shows minutes only, rounded up`() {
        val now = LocalDateTime.of(2026, 10, 2, 6, 0, 30)
        assertEquals("Next: today at 06:30 · in 30 m", describeNext(LocalDateTime.of(2026, 10, 2, 6, 30), now))
    }

    @Test fun `summary more than a day out names the weekday`() {
        assertEquals(
            "Next: Monday at 06:30 · in 2 d 7 h",
            describeNext(LocalDateTime.of(2026, 10, 5, 6, 30), friday2241),
        )
    }

    @Test fun `12-hour times read the way the phone shows them`() {
        assertEquals("12:05 AM", formatTime(0, 5, is24Hour = false))
        assertEquals("6:30 AM", formatTime(6, 30, is24Hour = false))
        assertEquals("12:00 PM", formatTime(12, 0, is24Hour = false))
        assertEquals("6:30 PM", formatTime(18, 30, is24Hour = false))
        assertEquals("18:30", formatTime(18, 30))
    }

    @Test fun `summary follows the 12-hour setting`() {
        val alarm = Alarm(hour = 6, minute = 30)
        assertEquals(
            "Next: tomorrow at 6:30 AM · in 7 h 49 m",
            nextAlarmSummary(listOf(alarm), friday2241, is24Hour = false),
        )
    }
}
