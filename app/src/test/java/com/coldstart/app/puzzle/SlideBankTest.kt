package com.coldstart.app.puzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class SlideBankTest {

    private fun bank(level: Int) = File("src/main/assets/slide_bank_$level.txt")

    /**
     * Writes the bank. Off by default; run with COLDSTART_GEN_BANK=1 in the environment.
     * Slow on purpose: it's the work the phone shouldn't do at 6am.
     */
    @Test fun generateBank() {
        assumeTrue(System.getenv("COLDSTART_GEN_BANK") == "1")
        for ((level, count) in listOf(2 to 250, 3 to 400)) {
            val rng = Random(20261006L + level)
            val seen = HashSet<String>()
            val lines = mutableListOf<String>()
            var tries = 0
            while (lines.size < count && tries < count * 40) {
                tries++
                val b = generateSlide(level, rng, attempts = 60)
                val need = solveSlide(b) ?: continue
                if (need !in slideMoves(level)) continue
                if (seen.add(b.signature())) lines += SlideBank.encode(b)
            }
            bank(level).parentFile.mkdirs()
            bank(level).writeText(lines.joinToString("\n") + "\n")
            println("level $level: ${lines.size} boards in $tries tries")
        }
    }

    @Test fun `every bank board is solvable, unsolved, and in its level's range`() {
        for (level in 2..3) {
            val lines = bank(level).readLines().filter { it.isNotBlank() }
            assertTrue("level $level bank too small: ${lines.size}", lines.size >= 100)
            lines.forEachIndexed { i, line ->
                val b = SlideBank.decode(line)
                assertEquals(level + 3, b.size)
                assertFalse(b.isSolved)
                val need = solveSlide(b)
                assertNotNull("level $level line $i unsolvable", need)
                assertTrue("level $level line $i took $need", need!! in slideMoves(level))
            }
        }
    }

    @Test fun `flipping keeps the move count`() {
        val lines = bank(3).readLines().filter { it.isNotBlank() }
        repeat(20) { i ->
            val b = SlideBank.decode(lines[i])
            assertEquals(solveSlide(b), solveSlide(SlideBank.flip(b)))
        }
    }

    @Test fun `encode and decode round trip`() {
        val b = generateSlide(1, Random(3))
        assertEquals(b, SlideBank.decode(SlideBank.encode(b)))
    }

    @Test fun `hard picks from the bank need at least seven moves`() {
        val lines = bank(3).readLines().filter { it.isNotBlank() }
        repeat(30) { seed -> assertTrue(solveSlide(slideFor(3, Random(seed), lines))!! >= 7) }
    }
}
