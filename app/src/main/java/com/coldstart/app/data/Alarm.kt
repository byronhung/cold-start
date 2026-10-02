package com.coldstart.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** One saved alarm: one row in the `alarms` table. */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    /** Which weekdays it repeats on, as bits: Monday = bit 0 … Sunday = bit 6. 0 = rings once. */
    val repeatDays: Int = 0,
    val label: String = "",
    val enabled: Boolean = true,
    /** The puzzle types this alarm draws its morning from. */
    val roundTypes: List<RoundType> = RoundType.MORNING_DEFAULT,
    /** Unused since v0.2: the code is shared now, see [AppSettings.wakeCode]. Kept to avoid a migration. */
    val qrCode: String? = null,
    /**
     * "Still awake?" checks after solving, 0–3. Like snooze, but the other way round. New alarms
     * get 1; alarms from before v0.2 get 0 (the column default) so nothing changes under them.
     */
    @ColumnInfo(defaultValue = "0") val wakeChecks: Int = 1,
    /** Unused since v5: replaced by [wakeMethod] (the migration carried its value over). */
    @ColumnInfo(defaultValue = "0") val finishWithScan: Boolean = false,
    /** Difficulty preset, as [com.coldstart.app.puzzle.Preset.code]: 0 Gentle, 1 Normal, 2 Hard. */
    @ColumnInfo(defaultValue = "1") val difficulty: Int = 1,
    /** How it stops, as [com.coldstart.app.puzzle.WakeMethod.code]: 0 Puzzles, 1 Scan, 2 Puzzles + scan. */
    @ColumnInfo(defaultValue = "0") val wakeMethod: Int = 0,
    /** The sound picked for this alarm (a content URI). Null = the phone's default alarm sound. */
    val soundUri: String? = null,
)

enum class RoundType {
    STROOP,
    PATTERN_FLASH,
    ODD_ONE_OUT,
    QR_SCAN;

    companion object {
        val MORNING_DEFAULT = listOf(STROOP, PATTERN_FLASH, ODD_ONE_OUT)
    }
}

/** Room stores simple columns only, so the round list is saved as "STROOP,PATTERN_FLASH,…". */
class Converters {
    @TypeConverter
    fun roundTypesToString(types: List<RoundType>): String = types.joinToString(",") { it.name }

    @TypeConverter
    fun stringToRoundTypes(value: String): List<RoundType> =
        if (value.isBlank()) emptyList() else value.split(",").map { RoundType.valueOf(it) }
}
