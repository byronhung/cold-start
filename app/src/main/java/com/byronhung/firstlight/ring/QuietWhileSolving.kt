package com.byronhung.firstlight.ring

/**
 * Quiet while you solve: tapping the puzzle silences the alarm, and [WINDOW_MS] without a tap
 * brings it back at full volume. For the alarm that goes off on a subway or in a meeting: you still
 * solve every round, just without the noise. Drift off mid-puzzle and the taps stop, so it blasts.
 *
 * Only taps on the puzzle count, never the give-up ring: holding that must not buy silence.
 */
object QuietWhileSolving {
    const val WINDOW_MS = 10_000L

    /** [lastTapMs] and [nowMs] on the same clock (elapsedRealtime); null = no tap yet this ring. */
    fun isQuiet(lastTapMs: Long?, nowMs: Long): Boolean =
        lastTapMs != null && nowMs - lastTapMs in 0 until WINDOW_MS
}
