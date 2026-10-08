package com.byronhung.firstlight.alarm

/**
 * "Still awake?" checks after an alarm is solved: the fix for solving it half-asleep and going
 * back to sleep. Pure logic; the Android side is WakeCheckReceiver and WakeCheckActivity.
 *
 * Each check fires [DELAY_MS] after the last solve or check. If the phone is in use, it passes
 * silently (someone scrolling is awake). Otherwise a full-screen "Still awake?" has [WINDOW_MS]
 * to be answered; missing it rings the alarm again from round 1.
 */
data class WakeCheck(
    val alarmId: Long,
    val wakeId: Long,
    /** 1-based: this is check [index] of [total]. */
    val index: Int,
    val total: Int,
) {
    val isLast: Boolean get() = index >= total

    fun next(): WakeCheck? = if (isLast) null else copy(index = index + 1)

    companion object {
        const val DELAY_MS = 5 * 60 * 1000L
        const val WINDOW_MS = 60 * 1000L

        /** Set by the debug receiver so the flow can be tested in seconds. Null in normal use. */
        @Volatile var delayOverrideMs: Long? = null

        val delayMs: Long get() = delayOverrideMs ?: DELAY_MS

        /** The first check after a solve, or null if this alarm has checks switched off. */
        fun first(alarmId: Long, wakeId: Long, total: Int): WakeCheck? =
            if (total <= 0) null else WakeCheck(alarmId, wakeId, index = 1, total = total.coerceAtMost(MAX))

        const val MAX = 3
    }
}

/** What to do when a check fires. */
enum class CheckAction {
    /** Phone is unlocked and in use: count it as passed, no prompt. */
    PASS_SILENTLY,

    /** Phone is locked or dark: show "Still awake?" and start the countdown. */
    ASK,

    /** An alarm is already ringing (it was missed, or another alarm went off): skip this check. */
    SKIP,
}

/**
 * [isCurrent]: this check is the one pending check. A check that was replaced (another alarm was
 * solved since) or cancelled (an alarm started ringing) is stale, and stale checks do nothing.
 */
fun decideCheck(alarmRinging: Boolean, screenOn: Boolean, locked: Boolean, isCurrent: Boolean = true): CheckAction = when {
    !isCurrent -> CheckAction.SKIP
    alarmRinging -> CheckAction.SKIP
    screenOn && !locked -> CheckAction.PASS_SILENTLY
    else -> CheckAction.ASK
}
