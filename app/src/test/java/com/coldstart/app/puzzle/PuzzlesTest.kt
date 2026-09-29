package com.coldstart.app.puzzle

import com.coldstart.app.alarm.formatDuration
import com.coldstart.app.data.RoundType
import com.coldstart.app.data.RoundType.ODD_ONE_OUT
import com.coldstart.app.data.RoundType.PATTERN_FLASH
import com.coldstart.app.data.RoundType.STROOP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzlesTest {

    private val pool = RoundType.MORNING_DEFAULT

    // ---------- the morning ----------

    @Test fun `a morning is one of each type`() {
        repeat(200) { seed ->
            val plan = planMorning(pool, lastOpener = null, rng = Random(seed))
            assertEquals(pool.toSet(), plan.toSet())
            assertEquals(3, plan.size)
        }
    }

    @Test fun `yesterday's opener never opens today`() {
        for (opener in pool) {
            repeat(200) { seed ->
                assertNotEquals(opener, planMorning(pool, opener, Random(seed)).first())
            }
        }
    }

    @Test fun `the order actually varies`() {
        val orders = (0 until 200).map { planMorning(pool, null, Random(it)) }.toSet()
        assertEquals(6, orders.size) // all 3! orders turn up
    }

    // ---------- Stroop ----------

    @Test fun `the ink never matches the word`() {
        val rng = Random(1)
        repeat(1_000) {
            val w = stroopWord(rng)
            assertNotEquals(w.word, w.ink)
        }
    }

    @Test fun `stroop rounds get longer with level`() {
        assertEquals(listOf(2, 3, 4), (1..3).map(::stroopWordCount))
    }

    // ---------- pattern flash ----------

    @Test fun `pattern lights the right number of distinct cells inside the grid`() {
        val expected = mapOf(1 to (3 to 3), 2 to (3 to 4), 3 to (4 to 5))
        for ((level, sizeAndCount) in expected) {
            repeat(100) { seed ->
                val p = patternFor(level, Random(seed))
                assertEquals(sizeAndCount.first, p.gridSize)
                assertEquals(sizeAndCount.second, p.lit.size)
                assertTrue(p.lit.all { it in 0 until p.gridSize * p.gridSize })
            }
        }
    }

    // ---------- odd one out ----------

    @Test fun `odd one out grid is 4x4, 5x5, 6x6 by level`() {
        for (level in 1..3) {
            val spec = oddOneOutFor(level, Random(level))
            assertEquals(level + 3, spec.gridSize)
            assertEquals(spec.gridSize * spec.gridSize, spec.items.size)
        }
    }

    @Test fun `exactly one shape has the target combination`() {
        repeat(300) { seed ->
            val spec = oddOneOutFor(Levels.DEFAULT, Random(seed))
            val target = spec.items[spec.targetIndex]
            assertEquals(1, spec.items.count { it == target })
        }
    }

    @Test fun `every decoy differs from the target in exactly one feature`() {
        repeat(300) { seed ->
            val spec = oddOneOutFor(3, Random(seed))
            val t = spec.items[spec.targetIndex]
            spec.items.forEachIndexed { i, s ->
                if (i == spec.targetIndex) return@forEachIndexed
                val diffs = listOf(s.colour != t.colour, s.form != t.form, s.size != t.size).count { it }
                assertEquals(1, diffs)
            }
        }
    }

    @Test fun `no feature gives the target away on its own`() {
        // The V1 flaw: if the target's colour (or shape, or size) were unique, it would pop out.
        repeat(300) { seed ->
            val spec = oddOneOutFor(Levels.DEFAULT, Random(seed))
            val t = spec.items[spec.targetIndex]
            assertTrue(spec.items.count { it.colour == t.colour } > 1)
            assertTrue(spec.items.count { it.form == t.form } > 1)
            assertTrue(spec.items.count { it.size == t.size } > 1)
        }
    }

    // ---------- adaptive difficulty ----------

    private fun rounds(vararg ms: Long, misses: Int = 0) = ms.map { PastRound(it, misses) }

    @Test fun `fewer than five rounds never moves the level`() {
        assertEquals(2, Difficulty.nextLevel(STROOP, 2, rounds(1_000, 1_000, 1_000, 1_000)))
    }

    @Test fun `fast and clean goes up a level`() {
        assertEquals(3, Difficulty.nextLevel(STROOP, 2, rounds(3_000, 4_000, 4_500, 2_000, 9_000)))
    }

    @Test fun `fast but sloppy stays put`() {
        assertEquals(2, Difficulty.nextLevel(ODD_ONE_OUT, 2, rounds(3_000, 3_000, 3_000, 3_000, 3_000, misses = 1)))
    }

    @Test fun `slow goes down a level`() {
        assertEquals(1, Difficulty.nextLevel(PATTERN_FLASH, 2, rounds(20_000, 18_000, 17_000, 5_000, 30_000)))
    }

    @Test fun `levels stay between 1 and 3`() {
        assertEquals(3, Difficulty.nextLevel(STROOP, 3, rounds(1_000, 1_000, 1_000, 1_000, 1_000)))
        assertEquals(1, Difficulty.nextLevel(STROOP, 1, rounds(60_000, 60_000, 60_000, 60_000, 60_000)))
    }

    @Test fun `only the newest five count`() {
        // Newest first: five fast rounds, then old slow ones that must be ignored.
        val history = rounds(3_000, 3_000, 3_000, 3_000, 3_000) + rounds(60_000, 60_000, 60_000)
        assertEquals(3, Difficulty.nextLevel(STROOP, 2, history))
    }

    // ---------- formatting ----------

    @Test fun `durations read naturally`() {
        assertEquals("24 s", formatDuration(24_900))
        assertEquals("1 m 12 s", formatDuration(72_000))
        assertEquals("1 h 3 m", formatDuration(3_780_000))
    }
}
