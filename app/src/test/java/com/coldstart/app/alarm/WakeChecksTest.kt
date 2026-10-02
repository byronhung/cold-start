package com.coldstart.app.alarm

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
}
