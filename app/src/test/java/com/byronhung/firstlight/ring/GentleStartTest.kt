package com.byronhung.firstlight.ring

import org.junit.Assert.assertEquals
import org.junit.Test

class GentleStartTest {
    @Test fun `starts at ten percent`() = assertEquals(0.1f, gentleLevel(0), 0.001f)

    @Test fun `halfway through is just over half`() = assertEquals(0.55f, gentleLevel(GENTLE_MS / 2), 0.001f)

    @Test fun `full at thirty seconds and stays there`() {
        assertEquals(1f, gentleLevel(GENTLE_MS), 0.001f)
        assertEquals(1f, gentleLevel(GENTLE_MS * 10), 0.001f)
    }

    @Test fun `a clock running backwards can't make it quieter than the start`() =
        assertEquals(0.1f, gentleLevel(-5_000), 0.001f)
}
