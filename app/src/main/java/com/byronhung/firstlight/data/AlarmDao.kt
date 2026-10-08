package com.byronhung.firstlight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    /** Every alarm, earliest in the day first. Re-emits whenever the table changes. */
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms")
    suspend fun getAll(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun get(id: Long): Alarm?

    @Insert
    suspend fun insert(alarm: Alarm): Long

    @Update
    suspend fun update(alarm: Alarm)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun delete(id: Long)
}
