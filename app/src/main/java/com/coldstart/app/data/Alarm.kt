package com.coldstart.app.data

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
    /** The code to scan, once the QR round exists (chunk 11). Null until then. */
    val qrCode: String? = null,
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
