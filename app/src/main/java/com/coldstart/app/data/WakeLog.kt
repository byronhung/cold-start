package com.coldstart.app.data

import androidx.room.ColumnInfo
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
