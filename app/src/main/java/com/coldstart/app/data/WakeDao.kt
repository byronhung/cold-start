package com.coldstart.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeDao {
    @Insert
    suspend fun insertWake(wake: WakeLog): Long

    @Query("UPDATE wake_log SET endedAt = :endedAt, outcome = :outcome WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long, outcome: String)

    @Insert
    suspend fun insertResults(results: List<RoundResult>)

    @Query("SELECT opener FROM wake_log WHERE opener IS NOT NULL ORDER BY firedAt DESC LIMIT 1")
    suspend fun lastOpener(): String?

    /** Newest first. */
    @Query("SELECT * FROM round_results WHERE type = :type ORDER BY id DESC LIMIT :limit")
    suspend fun recentResults(type: String, limit: Int): List<RoundResult>

    @Query("SELECT * FROM wake_log ORDER BY firedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WakeLog>>
}
