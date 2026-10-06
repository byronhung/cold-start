package com.coldstart.app.puzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PackPuzzlesTest {

    // ---------- Memory pairs ----------

    @Test fun `twelve cards, every shape exactly twice`() {
        repeat(100) { seed ->
            val deck = dealPairs(Random(seed))
            assertEquals(12, deck.size)
            assertTrue(deck.groupingBy { it }.eachCount().values.all { it == 2 })
        }
    }

    @Test fun `pairs to find is 2, 3, 4 by level and always fewer than on the table`() {
        assertEquals(listOf(2, 3, 4), (1..3).map(::pairsToFind))
        assertTrue((1..3).all { pairsToFind(it) < PAIRS_ON_TABLE })
    }

    // ---------- Connect the path ----------

    @Test fun `pipes rotate clockwise and four turns come home`() {
        assertEquals(Pipe.E, Pipe.rotate(Pipe.N, 1))
        assertEquals(Pipe.W or Pipe.N, Pipe.rotate(Pipe.S or Pipe.W, 1))
        for (m in 0..15) assertEquals(m, Pipe.rotate(m, 4))
    }

    @Test fun `every path board starts unsolved and is solved by turning tiles back`() {
        for (level in 1..3) repeat(150) { seed ->
            val board = pathFor(level, Random(seed))
            assertEquals(pathSize(level) * pathSize(level), board.solved.size)
            assertFalse("level $level seed $seed starts solved", board.isSolved)
            assertTrue("level $level seed $seed has no solution", board.copy(turns = List(board.turns.size) { 0 }).isSolved)
        }
    }

    @Test fun `turning a tile four times leaves the board as it was`() {
        val board = pathFor(2, Random(7))
        val spun = (1..4).fold(board) { b, _ -> b.turn(0) }
        assertEquals(board.lit(), spun.lit())
    }

    // ---------- Slide out ----------

    @Test fun `the key reaching the right wall solves it`() {
        val board = SlideBoard(listOf(Block(0, 2, SLIDE_EXIT_ROW, 2, 1, key = true)))
        assertTrue(board.isSolved)
    }

    @Test fun `blocks slide as far as they can, and not through each other`() {
        val board = SlideBoard(listOf(Block(0, 0, 1, 2, 1, key = true), Block(1, 3, 0, 1, 2)))
        val moved = board.slide(0, 1)!!
        assertEquals(1, moved.key.x) // stopped by the vertical block at x = 3
        assertEquals(null, board.slide(0, -1)) // already against the left wall
    }

    @Test fun `the prototype board takes four moves`() {
        val board = SlideBoard(
            listOf(
                Block(0, 0, 1, 2, 1, key = true),
                Block(1, 2, 0, 1, 2),
                Block(2, 3, 0, 1, 2),
                Block(3, 2, 2, 2, 1),
                Block(4, 0, 3, 2, 1),
            ),
        )
        assertEquals(4, solveSlide(board))
    }

    @Test fun `every generated slide board is solvable within its level's move range`() {
        for (level in 1..3) repeat(40) { seed ->
            val board = slideFor(level, Random(seed * 31 + level))
            val need = solveSlide(board)
            assertNotNull("level $level seed $seed unsolvable", need)
            assertTrue("level $level seed $seed took $need", need!! in slideMoves(level))
            assertFalse(board.isSolved)
        }
    }

    @Test fun `no two blocks ever overlap`() {
        for (level in 1..3) repeat(40) { seed ->
            val board = slideFor(level, Random(seed + 1000 * level))
            val cells = board.blocks.flatMap { b -> (0 until b.w).flatMap { dx -> (0 until b.h).map { dy -> (b.y + dy) * SLIDE_SIZE + b.x + dx } } }
            assertEquals(cells.size, cells.toSet().size)
        }
    }
}
