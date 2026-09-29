package com.coldstart.app.puzzle

import com.coldstart.app.data.RoundType
import kotlin.random.Random

/**
 * Pure puzzle logic: no Android, no Compose. What a morning contains, what each round looks like,
 * and how difficulty moves. Every generator takes a [Random] so tests can pin it.
 */

object Levels {
    const val MIN = 1
    const val MAX = 3
    const val DEFAULT = 2
}

// ---------- the morning ----------

/**
 * One round of each type, shuffled. Yesterday's first type can't be first today:
 * a one-day memory is enough to kill the sense of repetition.
 */
fun planMorning(types: List<RoundType>, lastOpener: RoundType?, rng: Random): List<RoundType> {
    val order = types.distinct().shuffled(rng).toMutableList()
    if (order.size > 1 && order[0] == lastOpener) {
        val swap = 1 + rng.nextInt(order.size - 1)
        order[0] = order[swap].also { order[swap] = order[0] }
    }
    return order
}

// ---------- Stroop ----------

enum class InkColour { RED, BLUE, GREEN, YELLOW }

/** The word names one colour; the ink is always a different one. */
data class StroopWord(val word: InkColour, val ink: InkColour)

/** Words per round: 2, 3, 4 at levels 1–3. */
fun stroopWordCount(level: Int): Int = level.coerceIn(Levels.MIN, Levels.MAX) + 1

fun stroopWord(rng: Random): StroopWord {
    val word = InkColour.entries.random(rng)
    val ink = InkColour.entries.filter { it != word }.random(rng)
    return StroopWord(word, ink)
}

// ---------- Pattern flash ----------

/** [lit] are cell indices, row by row, in a [gridSize]×[gridSize] grid. */
data class PatternSpec(val gridSize: Int, val lit: Set<Int>)

/** Level 1: 3 of 9 · level 2: 4 of 9 · level 3: 5 of 16. */
fun patternFor(level: Int, rng: Random): PatternSpec {
    val (size, count) = when (level.coerceIn(Levels.MIN, Levels.MAX)) {
        1 -> 3 to 3
        2 -> 3 to 4
        else -> 4 to 5
    }
    val lit = (0 until size * size).shuffled(rng).take(count).toSet()
    return PatternSpec(size, lit)
}

// ---------- Odd one out (V2 hard) ----------

enum class ShapeColour { RED, BLUE }
enum class ShapeForm { CIRCLE, SQUARE }
enum class ShapeSize { BIG, SMALL }

data class Shape(val colour: ShapeColour, val form: ShapeForm, val size: ShapeSize)

data class OddOneOutSpec(val gridSize: Int, val items: List<Shape>, val targetIndex: Int)

/** Adaptive difficulty moves the grid only: 4×4, 5×5, 6×6 at levels 1–3. */
fun oddOneOutGridSize(level: Int): Int = level.coerceIn(Levels.MIN, Levels.MAX) + 3

/**
 * The target is one colour + shape + size combination. Every decoy matches it on two features
 * and differs on exactly one, so nothing pops out: you have to check each shape.
 */
fun oddOneOutFor(level: Int, rng: Random): OddOneOutSpec {
    val size = oddOneOutGridSize(level)
    val target = Shape(
        ShapeColour.entries.random(rng),
        ShapeForm.entries.random(rng),
        ShapeSize.entries.random(rng),
    )
    val decoys = (0 until size * size - 1).map { i ->
        when (i % 3) {
            0 -> target.copy(colour = target.colour.other())
            1 -> target.copy(form = target.form.other())
            else -> target.copy(size = target.size.other())
        }
    }
    val items = (decoys + target).shuffled(rng)
    return OddOneOutSpec(size, items, items.indexOf(target))
}

private fun ShapeColour.other() = if (this == ShapeColour.RED) ShapeColour.BLUE else ShapeColour.RED
private fun ShapeForm.other() = if (this == ShapeForm.CIRCLE) ShapeForm.SQUARE else ShapeForm.CIRCLE
private fun ShapeSize.other() = if (this == ShapeSize.BIG) ShapeSize.SMALL else ShapeSize.BIG

// ---------- adaptive difficulty ----------

data class PastRound(val solveMs: Long, val misses: Int)

object Difficulty {
    /** Rounds needed at a level before it can move. */
    const val WINDOW = 5

    /** (fast, slow) median solve times per round, in ms. */
    fun thresholds(type: RoundType): Pair<Long, Long> = when (type) {
        RoundType.STROOP -> 5_000L to 15_000L
        RoundType.PATTERN_FLASH -> 6_000L to 16_000L
        RoundType.ODD_ONE_OUT -> 5_000L to 15_000L
        RoundType.QR_SCAN -> Long.MAX_VALUE to Long.MAX_VALUE
    }

    /**
     * [recentAtLevel]: newest first, only rounds played at [current].
     * Fast and nearly clean over the last five → one notch harder. Slow → one notch easier.
     * Changing level empties the window, so it can't bounce morning to morning.
     */
    fun nextLevel(type: RoundType, current: Int, recentAtLevel: List<PastRound>): Int {
        if (type == RoundType.QR_SCAN || recentAtLevel.size < WINDOW) return current
        val window = recentAtLevel.take(WINDOW)
        val median = window.map { it.solveMs }.sorted()[WINDOW / 2]
        val (fast, slow) = thresholds(type)
        return when {
            median <= fast && window.sumOf { it.misses } <= 1 -> (current + 1).coerceAtMost(Levels.MAX)
            median >= slow -> (current - 1).coerceAtLeast(Levels.MIN)
            else -> current
        }
    }
}
