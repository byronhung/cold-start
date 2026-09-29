package com.coldstart.app.data

import kotlinx.coroutines.flow.Flow

/**
 * The one door to saved alarms. Screens never touch the DAO directly.
 *
 * Chunk 03 makes this the place that also schedules and cancels the real Android alarm, so every
 * save, toggle and delete keeps the database and the system in step. One door, not two.
 */
class AlarmRepository(private val dao: AlarmDao) {
    val alarms: Flow<List<Alarm>> = dao.observeAll()

    suspend fun get(id: Long): Alarm? = dao.get(id)

    /** Inserts a new alarm (id 0) or updates an existing one. Returns its id. */
    suspend fun save(alarm: Alarm): Long =
        if (alarm.id == 0L) {
            dao.insert(alarm)
        } else {
            dao.update(alarm)
            alarm.id
        }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun delete(id: Long) = dao.delete(id)
}
