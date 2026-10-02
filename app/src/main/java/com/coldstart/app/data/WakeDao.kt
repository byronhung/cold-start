package com.coldstart.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
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

    @Query("UPDATE wake_log SET checksPassed = checksPassed + 1 WHERE id = :id")
    suspend fun checkPassed(id: Long)

    @Query("UPDATE wake_log SET checksMissed = checksMissed + 1 WHERE id = :id")
    suspend fun checkMissed(id: Long)

    @Query("SELECT * FROM settings WHERE id = 0")
    fun observeSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun settings(): AppSettings?

    @Upsert
    suspend fun saveSettings(settings: AppSettings)

    @Query("SELECT * FROM wake_log ORDER BY firedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WakeLog>>
}
