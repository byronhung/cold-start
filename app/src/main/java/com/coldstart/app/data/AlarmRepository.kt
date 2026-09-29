package com.coldstart.app.data

import com.coldstart.app.alarm.AlarmScheduler
import com.coldstart.app.alarm.Weekdays
import com.coldstart.app.puzzle.Difficulty
import com.coldstart.app.puzzle.Levels
import com.coldstart.app.puzzle.PastRound
import com.coldstart.app.puzzle.planMorning
import com.coldstart.app.ring.RingSession
import com.coldstart.app.ring.RoundResultDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * The one door to saved alarms and wake history. Screens never touch the DAOs, and every change
 * to an alarm schedules or cancels the real Android alarm in the same step.
 *
 * A mutex serialises the writes, because two paths can land at once: the app starting up
 * (rescheduleAll) and an alarm firing (beginWake) happen together whenever an alarm wakes the app.
 */
class AlarmRepository(
    private val database: ColdStartDatabase,
    private val scheduler: AlarmScheduler,
) {
    private val alarmDao = database.alarmDao()
    private val wakeDao = database.wakeDao()
    private val mutex = Mutex()

    val alarms: Flow<List<Alarm>> = alarmDao.observeAll()
    val recentWakes: Flow<List<WakeLog>> = wakeDao.observeRecent(60)

    suspend fun get(id: Long): Alarm? = alarmDao.get(id)

    /** Inserts a new alarm (id 0) or updates an existing one, then schedules it. Returns its id. */
    suspend fun save(alarm: Alarm): Long = mutex.withLock {
        val id = if (alarm.id == 0L) alarmDao.insert(alarm) else alarm.id.also { alarmDao.update(alarm) }
        applySchedule(alarm.copy(id = id))
        id
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = mutex.withLock {
        alarmDao.setEnabled(id, enabled)
        alarmDao.get(id)?.let { applySchedule(it) }
    }

    suspend fun delete(id: Long) = mutex.withLock {
        scheduler.cancel(id)
        alarmDao.delete(id)
    }

    /** After a reboot, a clock change, a force-stop, or any app start: make Android match the table. */
    suspend fun rescheduleAll() = mutex.withLock {
        alarmDao.getAll().forEach { applySchedule(it) }
    }

    private fun applySchedule(alarm: Alarm) {
        if (alarm.enabled) scheduler.schedule(alarm) else scheduler.cancel(alarm.id)
    }

    /**
     * An alarm just fired. Sets up its next ring, plans the morning, and opens a wake record.
     * Returns null if it shouldn't ring after all (deleted or switched off since it was scheduled).
     */
    suspend fun beginWake(alarmId: Long, now: LocalDateTime = LocalDateTime.now()): RingSession? = mutex.withLock {
        val alarm = alarmDao.get(alarmId)
        if (alarm == null || !alarm.enabled) {
            scheduler.cancel(alarmId)
            return@withLock null
        }
        scheduleNextAfterFiring(alarm, now)

        val puzzles = alarm.roundTypes.filter { it != RoundType.QR_SCAN }.ifEmpty { RoundType.MORNING_DEFAULT }
        val lastOpener = wakeDao.lastOpener()?.let { runCatching { RoundType.valueOf(it) }.getOrNull() }
        val rounds = planMorning(puzzles, lastOpener, Random.Default) +
            if (alarm.qrCode != null) listOf(RoundType.QR_SCAN) else emptyList()
        val levels = puzzles.associateWith { levelFor(it) }

        val wakeId = wakeDao.insertWake(
            WakeLog(alarmId = alarmId, firedAt = System.currentTimeMillis(), opener = rounds.first()),
        )
        RingSession(
            alarmId = alarm.id,
            wakeId = wakeId,
            hour = alarm.hour,
            minute = alarm.minute,
            label = alarm.label,
            rounds = rounds,
            levels = levels,
            qrCode = alarm.qrCode,
        )
    }

    /** A second alarm fired while one is already ringing: don't ring twice, but keep its schedule right. */
    suspend fun skipWake(alarmId: Long, now: LocalDateTime = LocalDateTime.now()) = mutex.withLock {
        alarmDao.get(alarmId)?.takeIf { it.enabled }?.let { scheduleNextAfterFiring(it, now) }
    }

    /**
     * Done at fire time, before the ring starts, so a crash mid-ring can't lose tomorrow's alarm.
     * A one-off alarm switches itself off; a repeating one is set for its next day.
     */
    private suspend fun scheduleNextAfterFiring(alarm: Alarm, now: LocalDateTime) {
        if (alarm.repeatDays == Weekdays.NONE) {
            alarmDao.setEnabled(alarm.id, false)
            scheduler.cancel(alarm.id)
        } else {
            scheduler.schedule(alarm, now)
        }
    }

    suspend fun finishWake(wakeId: Long, outcome: WakeOutcome, results: List<RoundResultDraft>) {
        wakeDao.finish(wakeId, System.currentTimeMillis(), outcome.name)
        if (results.isNotEmpty()) {
            wakeDao.insertResults(
                results.map { RoundResult(wakeId = wakeId, type = it.type, level = it.level, solveMs = it.solveMs, misses = it.misses) },
            )
        }
    }

    /** This morning's level for a puzzle type, moved by the last five rounds at the current level. */
    private suspend fun levelFor(type: RoundType): Int {
        val recent = wakeDao.recentResults(type.name, limit = 20)
        val current = recent.firstOrNull()?.level ?: Levels.DEFAULT
        val atLevel = recent.filter { it.level == current }.map { PastRound(it.solveMs, it.misses) }
        return Difficulty.nextLevel(type, current, atLevel)
    }
}
