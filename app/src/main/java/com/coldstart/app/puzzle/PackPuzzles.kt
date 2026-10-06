package com.coldstart.app.puzzle

import kotlin.random.Random

/**
 * Puzzle pack 1 (Plus): Memory pairs, Connect the path, Slide out. Pure logic, like Puzzles.kt:
 * every generator takes a [Random], and every board it makes is checked solvable before it's used.
 */

// ---------- Memory pairs ----------

/** Always six pairs on the table: the search stays real. */
const val PAIRS_ON_TABLE = 6

/** How many pairs you must find: 2, 3, 4 at levels 1–3. */
fun pairsToFind(level: Int): Int = level.coerceIn(Levels.MIN, Levels.MAX) + 1

/** Twelve cards: each of six shapes twice, shuffled. Values are shape indices 0–5. */
fun dealPairs(rng: Random): List<Int> = (0 until PAIRS_ON_TABLE).flatMap { listOf(it, it) }.shuffled(rng)

// ---------- Connect the path ----------

/** Pipe openings as bits. A turn rotates a tile clockwise: N→E→S→W. */
object Pipe {
    const val N = 1
    const val E = 2
    const val S = 4
    const val W = 8

    fun rotate(mask: Int, turns: Int): Int {
        var m = mask
        repeat(((turns % 4) + 4) % 4) { m = ((m shl 1) or (m shr 3)) and 15 }
        return m
    }
}

/**
 * [solved] holds each tile's openings in the solving orientation; [turns] how far each tile is
 * currently turned from it. Light enters on the left of row [startRow] and must leave on the right
 * of row [endRow].
 */
data class PathBoard(
    val size: Int,
    val solved: List<Int>,
    val turns: List<Int>,
    val startRow: Int,
    val endRow: Int,
) {
    fun openings(i: Int): Int = Pipe.rotate(solved[i], turns[i])

    fun turn(i: Int): PathBoard = copy(turns = turns.toMutableList().also { it[i] = it[i] + 1 })

    /** Tiles connected to the start light, following matching openings. */
    fun lit(): Set<Int> {
        val start = startRow * size
        if (openings(start) and Pipe.W == 0) return emptySet()
        val seen = mutableSetOf(start)
        val stack = ArrayDeque(listOf(start))
        val steps = listOf(Triple(Pipe.N, 0, -1), Triple(Pipe.E, 1, 0), Triple(Pipe.S, 0, 1), Triple(Pipe.W, -1, 0))
        val opposite = mapOf(Pipe.N to Pipe.S, Pipe.E to Pipe.W, Pipe.S to Pipe.N, Pipe.W to Pipe.E)
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            val x = i % size
            val y = i / size
            for ((bit, dx, dy) in steps) {
                if (openings(i) and bit == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx !in 0 until size || ny !in 0 until size) continue
                val j = ny * size + nx
                if (j in seen || openings(j) and opposite.getValue(bit) == 0) continue
                seen += j
                stack += j
            }
        }
        return seen
    }

    val isSolved: Boolean get() {
        val end = endRow * size + size - 1
        return end in lit() && openings(end) and Pipe.E != 0
    }
}

/** Grid size by level: 3×3, 4×4, 5×5. */
fun pathSize(level: Int): Int = level.coerceIn(Levels.MIN, Levels.MAX) + 2

/**
 * A random wandering path from the left edge to the right; every other tile gets a random
 * straight or corner. All tiles are then turned away from their solving position, so the board
 * never starts solved. Solvable by construction: turning each tile back solves it.
 */
fun pathFor(level: Int, rng: Random): PathBoard {
    val n = pathSize(level)
    val route = randomRoute(n, rng)
    val solved = MutableList(n * n) { if (rng.nextBoolean()) Pipe.N or Pipe.S else Pipe.N or Pipe.E }
    fun bitTowards(a: Pair<Int, Int>, b: Pair<Int, Int>) = when {
        b.first > a.first -> Pipe.E
        b.first < a.first -> Pipe.W
        b.second > a.second -> Pipe.S
        else -> Pipe.N
    }
    val opposite = mapOf(Pipe.N to Pipe.S, Pipe.E to Pipe.W, Pipe.S to Pipe.N, Pipe.W to Pipe.E)
    route.forEachIndexed { k, p ->
        val inBit = if (k == 0) Pipe.W else opposite.getValue(bitTowards(route[k - 1], p))
        val outBit = if (k == route.lastIndex) Pipe.E else bitTowards(p, route[k + 1])
        solved[p.second * n + p.first] = inBit or outBit
    }
    while (true) {
        val board = PathBoard(n, solved, List(n * n) { 1 + rng.nextInt(3) }, route.first().second, route.last().second)
        if (!board.isSolved) return board
    }
}

/** A self-avoiding walk from column 0 to the last column, drifting up and down on the way. */
private fun randomRoute(n: Int, rng: Random): List<Pair<Int, Int>> {
    repeat(1_000) {
        var x = 0
        var y = rng.nextInt(n)
        val route = mutableListOf(x to y)
        val seen = mutableSetOf(x to y)
        while (x < n - 1 && route.size <= n * 3) {
            val options = mutableListOf(x + 1 to y)
            if (y > 0 && (x to y - 1) !in seen) options += x to y - 1
            if (y < n - 1 && (x to y + 1) !in seen) options += x to y + 1
            val next = options.random(rng)
            x = next.first
            y = next.second
            route += next
            seen += next
        }
        if (x == n - 1 && route.size >= n + 1) return route
    }
    return (0 until n).map { it to 0 }
}

// ---------- Slide out ----------

/** A block on the 4×4 board. Long blocks slide only along their length; the key is horizontal. */
data class Block(val id: Int, val x: Int, val y: Int, val w: Int, val h: Int, val key: Boolean = false) {
    val horizontal: Boolean get() = key || w > h
}

const val SLIDE_SIZE = 4
const val SLIDE_EXIT_ROW = 1

data class SlideBoard(val blocks: List<Block>) {
    val key: Block get() = blocks.first { it.key }

    val isSolved: Boolean get() = key.x + key.w == SLIDE_SIZE && key.y == SLIDE_EXIT_ROW

    /**
     * The one move the app allows: slide a block as far as it goes in direction [dir] (-1 or +1)
     * along its own axis. Null if it can't move at all.
     */
    fun slide(id: Int, dir: Int): SlideBoard? {
        val b = blocks.first { it.id == id }
        val taken = HashSet<Int>()
        blocks.filter { it.id != id }.forEach { o ->
            for (dx in 0 until o.w) for (dy in 0 until o.h) taken += (o.y + dy) * SLIDE_SIZE + o.x + dx
        }
        var x = b.x
        var y = b.y
        while (true) {
            val nx = if (b.horizontal) x + dir else x
            val ny = if (b.horizontal) y else y + dir
            if (nx < 0 || ny < 0 || nx + b.w > SLIDE_SIZE || ny + b.h > SLIDE_SIZE) break
            var clear = true
            for (dx in 0 until b.w) for (dy in 0 until b.h) if ((ny + dy) * SLIDE_SIZE + nx + dx in taken) clear = false
            if (!clear) break
            x = nx
            y = ny
        }
        if (x == b.x && y == b.y) return null
        return SlideBoard(blocks.map { if (it.id == id) it.copy(x = x, y = y) else it })
    }

    fun moves(): List<SlideBoard> = blocks.flatMap { b -> listOf(-1, 1).mapNotNull { slide(b.id, it) } }

    /** A comparable fingerprint of where every block is. */
    fun signature(): String = blocks.sortedBy { it.id }.joinToString(";") { "${it.id}:${it.x},${it.y}" }
}

/**
 * Fewest moves to solve, using exactly the app's slide-as-far-as-it-goes rule, or null if it can't
 * be solved. The boards are tiny, so a full search is instant.
 */
fun solveSlide(start: SlideBoard, limit: Int = 30): Int? {
    if (start.isSolved) return 0
    val seen = hashSetOf(start.signature())
    var frontier = listOf(start)
    for (depth in 1..limit) {
        val next = mutableListOf<SlideBoard>()
        for (board in frontier) for (m in board.moves()) {
            if (m.isSolved) return depth
            if (seen.add(m.signature())) next += m
        }
        if (next.isEmpty()) return null
        frontier = next
    }
    return null
}

/** Fewest moves a board should take: 2–3, 3–5, 5–8 at levels 1–3. */
fun slideMoves(level: Int): IntRange = when (level.coerceIn(Levels.MIN, Levels.MAX)) {
    1 -> 2..3
    2 -> 3..5
    else -> 5..8
}

/**
 * Places blocks around a solved key, maps every position reachable from that layout, and works
 * out exactly how many moves each one needs (a reverse search from the solved positions). Then
 * picks one whose count is in this level's range. Exact, not luck: every returned board has a
 * known shortest solution, so none can be a dead end.
 */
fun slideFor(level: Int, rng: Random): SlideBoard {
    val range = slideMoves(level)
    repeat(3_000) {
        val solved = solvedLayout(rng, level) ?: return@repeat
        val fits = slideDistances(solved).filterValues { it in range }.keys
        if (fits.isNotEmpty()) return fits.random(rng)
    }
    return fallbackSlide()
}

/** Every board reachable from [start], mapped to its fewest moves to solved (unsolvable ones left out). */
internal fun slideDistances(start: SlideBoard): Map<SlideBoard, Int> {
    val boards = HashMap<String, SlideBoard>()
    val cameFrom = HashMap<String, MutableList<String>>()
    val queue = ArrayDeque(listOf(start))
    boards[start.signature()] = start
    while (queue.isNotEmpty() && boards.size < 20_000) {
        val b = queue.removeFirst()
        val from = b.signature()
        for (m in b.moves()) {
            val to = m.signature()
            cameFrom.getOrPut(to) { mutableListOf() } += from
            if (boards.putIfAbsent(to, m) == null) queue += m
        }
    }
    // Walk backwards from every solved board: each step back is one more move needed.
    val dist = HashMap<String, Int>()
    val back = ArrayDeque<String>()
    boards.forEach { (sig, b) -> if (b.isSolved) { dist[sig] = 0; back += sig } }
    while (back.isNotEmpty()) {
        val sig = back.removeFirst()
        val d = dist.getValue(sig)
        for (prev in cameFrom[sig].orEmpty()) {
            if (prev !in dist) {
                dist[prev] = d + 1
                back += prev
            }
        }
    }
    return dist.mapKeys { (sig, _) -> boards.getValue(sig) }
}

/** A solved board: key at the exit, more blocks at higher levels (3–4, 4–5, 5–6). */
private fun solvedLayout(rng: Random, level: Int): SlideBoard? {
    val blocks = mutableListOf(Block(0, SLIDE_SIZE - 2, SLIDE_EXIT_ROW, 2, 1, key = true))
    val taken = HashSet<Int>().apply { add(SLIDE_EXIT_ROW * SLIDE_SIZE + 2); add(SLIDE_EXIT_ROW * SLIDE_SIZE + 3) }
    val count = level.coerceIn(Levels.MIN, Levels.MAX) + 2 + rng.nextInt(2)
    var id = 1
    repeat(80) {
        if (blocks.size > count) return@repeat
        val vertical = rng.nextBoolean()
        val w = if (vertical) 1 else 2
        val h = if (vertical) 2 else 1
        val x = rng.nextInt(SLIDE_SIZE - w + 1)
        val y = rng.nextInt(SLIDE_SIZE - h + 1)
        // A horizontal block in the key's row could only ever sit in its way: keep that row clear.
        if (!vertical && y == SLIDE_EXIT_ROW) return@repeat
        val cells = (0 until w).flatMap { dx -> (0 until h).map { dy -> (y + dy) * SLIDE_SIZE + x + dx } }
        if (cells.any { it in taken }) return@repeat
        taken += cells
        blocks += Block(id++, x, y, w, h)
    }
    return if (blocks.size > count) SlideBoard(blocks) else null
}

/** The prototype's first board (4 moves). Only used if random generation somehow fails. */
private fun fallbackSlide() = SlideBoard(
    listOf(
        Block(0, 0, 1, 2, 1, key = true),
        Block(1, 2, 0, 1, 2),
        Block(2, 3, 0, 1, 2),
        Block(3, 2, 2, 2, 1),
        Block(4, 0, 3, 2, 1),
    ),
)
