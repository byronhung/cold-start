package com.byronhung.firstlight.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeChecksTest {

    @Test fun `checks switched off means no first check`() {
        assertNull(WakeCheck.first(alarmId = 1, wakeId = 9, total = 0))
    }

    @Test fun `one check is first and last`() {
        val c = WakeCheck.first(1, 9, total = 1)!!
        assertEquals(1, c.index)
        assertTrue(c.isLast)
        assertNull(c.next())
    }

    @Test fun `three checks run 1, 2, 3 then stop`() {
        val first = WakeCheck.first(1, 9, total = 3)!!
        val indices = generateSequence(first) { it.next() }.map { it.index }.toList()
        assertEquals(listOf(1, 2, 3), indices)
    }

    @Test fun `more than three is capped`() {
        assertEquals(3, WakeCheck.first(1, 9, total = 7)!!.total)
    }

    @Test fun `phone in use passes silently`() {
        assertEquals(CheckAction.PASS_SILENTLY, decideCheck(alarmRinging = false, screenOn = true, locked = false))
    }

    @Test fun `locked or dark phone gets asked`() {
        assertEquals(CheckAction.ASK, decideCheck(alarmRinging = false, screenOn = false, locked = true))
        assertEquals(CheckAction.ASK, decideCheck(alarmRinging = false, screenOn = true, locked = true))
        assertEquals(CheckAction.ASK, decideCheck(alarmRinging = false, screenOn = false, locked = false))
    }

    @Test fun `nothing is checked while an alarm is ringing`() {
        assertEquals(CheckAction.SKIP, decideCheck(alarmRinging = true, screenOn = true, locked = false))
    }

    @Test fun `a replaced or cancelled check does nothing`() {
        assertEquals(CheckAction.SKIP, decideCheck(alarmRinging = false, screenOn = false, locked = true, isCurrent = false))
        assertEquals(CheckAction.SKIP, decideCheck(alarmRinging = false, screenOn = true, locked = false, isCurrent = false))
    }

    @Test fun `checks for different mornings are different checks`() {
        val first = WakeCheck.first(alarmId = 1, wakeId = 40, total = 1)
        val second = WakeCheck.first(alarmId = 2, wakeId = 42, total = 1)
        assertTrue(first != second)
    }
}
