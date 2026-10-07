package com.coldstart.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** [AWAY]: solved, but with "Not home?" puzzles in place of the scan. Stored by name, so no migration. */
enum class WakeOutcome { SOLVED, AWAY, GAVE_UP, TIMED_OUT }

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
    @ColumnInfo(defaultValue = "0") val checksPassed: Int = 0,
    /** Wake checks missed: each one re-rang the alarm. */
    @ColumnInfo(defaultValue = "0") val checksMissed: Int = 0,
)

/** App-wide settings: one row, id 0. */
@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 0,
    /** The barcode every "finish with a scan" alarm asks for. Null until registered. */
    val wakeCode: String? = null,
    /** The puzzle mix every alarm draws from, as "STROOP,PATTERN_FLASH,…". Null = the free three. */
    val puzzleMix: String? = null,
    /** [com.coldstart.app.puzzle.Preset.code] used by every alarm that doesn't override it. */
    @ColumnInfo(defaultValue = "1") val defaultDifficulty: Int = 1,
    /** Cold Start Plus unlocked. Until Google Play Billing (chunk P11), set by a debug switch. */
    @ColumnInfo(defaultValue = "0") val isPlus: Boolean = false,
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
