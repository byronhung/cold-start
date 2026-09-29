package com.coldstart.app.alarm

import com.coldstart.app.data.Alarm
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Pure time logic, no Android: when does an alarm ring next, and how do we say so.
 * Chunk 03 schedules real alarms from [nextTrigger], so it's unit-tested (see NextTriggerTest).
 */

/** Repeat days as bits, Monday = bit 0 … Sunday = bit 6. */
object Weekdays {
    const val NONE = 0
    const val WEEKDAYS = 0b0011111
    const val WEEKEND = 0b1100000

    fun has(mask: Int, day: DayOfWeek): Boolean = mask and (1 shl (day.value - 1)) != 0
}

/**
 * The next moment strictly after [now] that the alarm should ring.
 * A one-off alarm (no repeat days) rings at the next occurrence of its time: today if that's
 * still ahead, otherwise tomorrow.
 */
fun nextTrigger(hour: Int, minute: Int, repeatDays: Int, now: LocalDateTime): LocalDateTime {
    val time = LocalTime.of(hour, minute)
    // 0..7, not 0..6: a Monday-only alarm checked on Monday after its time rings next Monday.
    for (offset in 0L..7L) {
        val date = now.toLocalDate().plusDays(offset)
        val candidate = date.atTime(time)
        if (!candidate.isAfter(now)) continue
        if (repeatDays == Weekdays.NONE || Weekdays.has(repeatDays, date.dayOfWeek)) return candidate
    }
    error("No trigger within 8 days for repeatDays=$repeatDays")
}

fun Alarm.nextTrigger(now: LocalDateTime): LocalDateTime = nextTrigger(hour, minute, repeatDays, now)

fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

/** "Next: tomorrow at 06:30 · in 7 h 49 m", or null if every alarm is off. */
fun nextAlarmSummary(alarms: List<Alarm>, now: LocalDateTime): String? {
    val next = alarms.filter { it.enabled }.minOfOrNull { it.nextTrigger(now) } ?: return null
    return describeNext(next, now)
}

fun describeNext(next: LocalDateTime, now: LocalDateTime): String {
    val day = when (ChronoUnit.DAYS.between(now.toLocalDate(), next.toLocalDate())) {
        0L -> "today"
        1L -> "tomorrow"
        else -> next.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }
    // Round up: an alarm 30 seconds away is "in 1 m", never "in 0 m".
    val minutes = (Duration.between(now, next).seconds + 59) / 60
    val until = when {
        minutes >= 24 * 60 -> "${minutes / (24 * 60)} d ${(minutes % (24 * 60)) / 60} h"
        minutes >= 60 -> "${minutes / 60} h ${minutes % 60} m"
        else -> "$minutes m"
    }
    return "Next: $day at ${formatTime(next.hour, next.minute)} · in $until"
}
