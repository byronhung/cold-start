package com.coldstart.app.ring

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietWhileSolvingTest {

    @Test fun `loud until the first tap`() {
        assertFalse(QuietWhileSolving.isQuiet(lastTapMs = null, nowMs = 5_000))
    }

    @Test fun `quiet right after a tap and for the whole window`() {
        assertTrue(QuietWhileSolving.isQuiet(lastTapMs = 1_000, nowMs = 1_000))
        assertTrue(QuietWhileSolving.isQuiet(lastTapMs = 1_000, nowMs = 10_999))
    }

    @Test fun `loud again once the taps stop for the window`() {
        assertFalse(QuietWhileSolving.isQuiet(lastTapMs = 1_000, nowMs = 11_000))
        assertFalse(QuietWhileSolving.isQuiet(lastTapMs = 1_000, nowMs = 60_000))
    }

    @Test fun `a tap from the future never silences it`() {
        assertFalse(QuietWhileSolving.isQuiet(lastTapMs = 5_000, nowMs = 1_000))
    }
}
