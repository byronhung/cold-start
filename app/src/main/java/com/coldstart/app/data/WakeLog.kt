package com.coldstart.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WakeOutcome { SOLVED, GAVE_UP, TIMED_OUT }

/** One time an alarm rang. [outcome] stays null if the ring never ended cleanly (process killed). */
@Entity(tableName = "wake_log")
data class WakeLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    /** Epoch millis. */
    val firedAt: Long,
    val endedAt: Long? = null,
    val outcome: WakeOutcome? = null,
    /** The first puzzle type that morning, so tomorrow can open with a different one. */
    val opener: RoundType? = null,
)

/** One solved puzzle round. Adaptive difficulty reads these. */
@Entity(tableName = "round_results")
data class RoundResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wakeId: Long,
    val type: RoundType,
    val level: Int,
    val solveMs: Long,
    val misses: Int,
)
