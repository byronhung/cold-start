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

    /** v0.2: mornings start at the top level (22 s half-asleep was too easy). Slow weeks step down. */
    const val DEFAULT = 3
}

// ---------- difficulty presets ----------

/**
 * Per alarm, chosen on the edit screen. Normal is the default and the only one that feeds the
 * automatic difficulty, so a Gentle weekend never makes the weekday alarm easier.
 */
enum class Preset(val code: Int, val rounds: Int) {
    /** For people who wake easily: 3 rounds, easiest puzzles. */
    GENTLE(0, 3),

    /** 5 rounds, starts at the top level and eases down if mornings are slow. */
    NORMAL(1, 5),

    /** 4 rounds, top level, never eases down: fewer reps, because each one is a real puzzle. */
    HARD(2, 4);

    /** This preset's level for a puzzle type, given what adaptive difficulty would choose. */
    fun level(adaptive: Int): Int = when (this) {
        GENTLE -> Levels.MIN
        NORMAL -> adaptive
        HARD -> Levels.MAX
    }

    val feedsAdaptive: Boolean get() = this == NORMAL

    companion object {
        fun of(code: Int): Preset = entries.firstOrNull { it.code == code } ?: NORMAL
    }
}

// ---------- puzzle mix ----------

/** Fewer than three and mornings just alternate: the boredom problem comes back. */
const val MIN_MIX = 3

/**
 * Every puzzle the mix picker shows. [type] is null for a Plus puzzle that isn't built yet: it shows
 * in the picker (locked, or "coming soon" with Plus) but can't be chosen.
 */
data class CatalogPuzzle(val name: String, val type: RoundType?, val plus: Boolean)

val PUZZLE_CATALOG = listOf(
    CatalogPuzzle("Stroop", RoundType.STROOP, plus = false),
    CatalogPuzzle("Pattern", RoundType.PATTERN_FLASH, plus = false),
    CatalogPuzzle("Odd one out", RoundType.ODD_ONE_OUT, plus = false),
    CatalogPuzzle("Pairs", RoundType.PAIRS, plus = true),
    CatalogPuzzle("Path", RoundType.PATH, plus = true),
    CatalogPuzzle("Slide out", RoundType.SLIDE, plus = true),
)

fun parseMix(saved: String?): List<RoundType> =
    saved.orEmpty().split(",").mapNotNull { name -> RoundType.entries.firstOrNull { it.name == name.trim() } }

fun encodeMix(mix: List<RoundType>): String = mix.distinct().joinToString(",") { it.name }

/**
 * The puzzles a morning really draws from: the saved mix, minus anything not built or not owned.
 * If that leaves fewer than [MIN_MIX], the free three: an alarm always has a full mix.
 */
fun effectiveMix(saved: List<RoundType>, isPlus: Boolean): List<RoundType> {
    val allowed = PUZZLE_CATALOG.filter { it.type != null && (!it.plus || isPlus) }.mapNotNull { it.type }
    val mix = saved.distinct().filter { it in allowed }
    return if (mix.size >= MIN_MIX) mix else RoundType.MORNING_DEFAULT
}

/** An alarm's difficulty: its own override, or the default from Settings. */
fun effectivePreset(override: Int, default: Int): Preset =
    if (override < 0) Preset.of(default) else Preset.of(override)

// ---------- how to wake up ----------

/** Per alarm: what stops it. */
enum class WakeMethod(val code: Int) {
    PUZZLES(0),

    /** Just walk to your code and scan it. */
    SCAN(1),

    /** The puzzles, then the scan as the last round. */
    PUZZLES_AND_SCAN(2);

    val usesScan: Boolean get() = this != PUZZLES

    companion object {
        fun of(code: Int): WakeMethod = entries.firstOrNull { it.code == code } ?: PUZZLES
    }
}

/**
 * The rounds for a morning. With no wake-up code registered, a scan method falls back to puzzles:
 * removing the code in Settings must never leave an alarm that only the 30 s hold can stop.
 */
fun roundsFor(method: WakeMethod, puzzles: List<RoundType>, hasCode: Boolean): List<RoundType> = when {
    !hasCode || method == WakeMethod.PUZZLES -> puzzles
    method == WakeMethod.SCAN -> listOf(RoundType.QR_SCAN)
    else -> puzzles + RoundType.QR_SCAN
}

/** "Not home?" on the scan round: this many puzzles replace the scan, all at the top level. */
const val NOT_HOME_ROUNDS = 2

/**
 * The scan round swapped for [NOT_HOME_ROUNDS] puzzles from [mix], for when the code is somewhere
 * you aren't. Dearer than walking to the code on purpose, so in bed it's never the easy way out.
 * The first replacement can't repeat the round before it.
 */
fun notHomeRounds(rounds: List<RoundType>, mix: List<RoundType>, rng: Random): List<RoundType> {
    val at = rounds.indexOf(RoundType.QR_SCAN)
    if (at < 0 || mix.isEmpty()) return rounds
    val swap = planMorning(mix, lastOpener = rounds.getOrNull(at - 1), rng = rng, rounds = NOT_HOME_ROUNDS)
    return rounds.take(at) + swap + rounds.drop(at + 1)
}

// ---------- the morning ----------

/** Five rounds: long enough that solving it is actually waking up (~60–90 s). */
const val MORNING_ROUNDS = 5

/**
 * [rounds] rounds drawn from [types]: every type at least once, never the same type twice in a
 * row, and yesterday's first type can't be first today.
 */
fun planMorning(
    types: List<RoundType>,
    lastOpener: RoundType?,
    rng: Random,
    rounds: Int = MORNING_ROUNDS,
): List<RoundType> {
    val pool = types.distinct()
    if (pool.isEmpty() || rounds <= 0) return emptyList()
    if (pool.size == 1) return List(rounds) { pool[0] }

    val needAll = rounds >= pool.size
    repeat(500) {
        val plan = ArrayList<RoundType>(rounds)
        for (i in 0 until rounds) {
            val banned = if (i == 0) lastOpener else plan[i - 1]
            plan += pool.filter { it != banned }.random(rng)
        }
        if (!needAll || plan.toSet().size == pool.size) return plan
    }
    // Practically unreachable: a fixed cycle that still meets every rule.
    val start = pool.indexOfFirst { it != lastOpener }.coerceAtLeast(0)
    return List(rounds) { pool[(start + it) % pool.size] }
}

// ---------- Stroop ----------

enum class InkColour { RED, BLUE, GREEN, YELLOW }

/** The word names one colour; the ink is always a different one. */
data class StroopWord(val word: InkColour, val ink: InkColour)

/** Words per round: 2, 3, 4 at levels 1–3. */
fun stroopWordCount(level: Int): Int = level.coerceIn(Levels.MIN, Levels.MAX) + 1

/** What a Stroop word asks you to tap: the colour of the ink, or the colour the word names. */
enum class StroopAsk { INK, WORD }

/**
 * Which ask each word in a round gets: random, so you have to read the instruction every time,
 * and both kinds appear in any round of two or more words.
 */
fun stroopAsks(count: Int, rng: Random): List<StroopAsk> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(StroopAsk.entries.random(rng))
    while (true) {
        val asks = List(count) { StroopAsk.entries.random(rng) }
        if (asks.toSet().size == 2) return asks
    }
}

/** The colour a tap must match for this word and ask. */
fun StroopWord.answer(ask: StroopAsk): InkColour = if (ask == StroopAsk.INK) ink else word

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
        RoundType.PAIRS -> 10_000L to 30_000L
        RoundType.PATH -> 8_000L to 25_000L
        RoundType.SLIDE -> 8_000L to 30_000L
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
