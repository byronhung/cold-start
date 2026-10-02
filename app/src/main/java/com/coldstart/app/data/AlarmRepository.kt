package com.coldstart.app.data

import com.coldstart.app.alarm.AlarmScheduler
import com.coldstart.app.alarm.WakeCheck
import com.coldstart.app.alarm.Weekdays
import com.coldstart.app.puzzle.Difficulty
import com.coldstart.app.puzzle.Levels
import com.coldstart.app.puzzle.PastRound
import com.coldstart.app.puzzle.Preset
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
    val settings: Flow<AppSettings?> = wakeDao.observeSettings()

    suspend fun setWakeCode(code: String?) {
        wakeDao.saveSettings((wakeDao.settings() ?: AppSettings()).copy(wakeCode = code?.trim()?.ifEmpty { null }))
    }

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
     *
     * [isRering]: a missed wake check is ringing it again. The alarm may have switched itself off
     * already (one-offs do, at fire time) and its next ring is already set, so neither is touched.
     */
    suspend fun beginWake(
        alarmId: Long,
        now: LocalDateTime = LocalDateTime.now(),
        isRering: Boolean = false,
    ): RingSession? = mutex.withLock {
        val alarm = alarmDao.get(alarmId)
        if (alarm == null || (!alarm.enabled && !isRering)) {
            if (!isRering) scheduler.cancel(alarmId)
            return@withLock null
        }
        if (!isRering) scheduleNextAfterFiring(alarm, now)

        val wakeCode = wakeDao.settings()?.wakeCode
        val scan = alarm.finishWithScan && wakeCode != null
        val puzzles = alarm.roundTypes.filter { it != RoundType.QR_SCAN }.ifEmpty { RoundType.MORNING_DEFAULT }
        val lastOpener = wakeDao.lastOpener()?.let { runCatching { RoundType.valueOf(it) }.getOrNull() }
        val preset = Preset.of(alarm.difficulty)
        val rounds = planMorning(puzzles, lastOpener, Random.Default, rounds = preset.rounds) +
            if (scan) listOf(RoundType.QR_SCAN) else emptyList()
        val levels = puzzles.associateWith { preset.level(adaptive = levelFor(it)) }

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
            qrCode = if (scan) wakeCode else null,
            wakeChecks = alarm.wakeChecks.coerceIn(0, WakeCheck.MAX),
            preset = preset,
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

    /** Only a solve earns wake checks: a give-up or a time-out has already ended the morning. */
    suspend fun finishWake(session: RingSession, outcome: WakeOutcome, results: List<RoundResultDraft>) {
        val wakeId = session.wakeId
        wakeDao.finish(wakeId, System.currentTimeMillis(), outcome.name)
        // Only Normal mornings teach the automatic difficulty: a Gentle or Hard one says nothing
        // about how fast you are at your own level.
        if (results.isNotEmpty() && session.preset.feedsAdaptive) {
            wakeDao.insertResults(
                results.map { RoundResult(wakeId = wakeId, type = it.type, level = it.level, solveMs = it.solveMs, misses = it.misses) },
            )
        }
        if (outcome == WakeOutcome.SOLVED) {
            WakeCheck.first(session.alarmId, wakeId, session.wakeChecks)?.let { scheduler.scheduleWakeCheck(it) }
        }
    }

    /** Answered in time, or passed silently because the phone was in use. Sets up the next one. */
    suspend fun checkPassed(check: WakeCheck) {
        scheduler.cancelCheckDeadline(check)
        wakeDao.checkPassed(check.wakeId)
        check.next()?.let { scheduler.scheduleWakeCheck(it) }
    }

    /** Missed: the caller rings the alarm again, and that morning gets its own checks if solved. */
    suspend fun checkMissed(check: WakeCheck) {
        wakeDao.checkMissed(check.wakeId)
    }

    /** This morning's level for a puzzle type, moved by the last five rounds at the current level. */
    private suspend fun levelFor(type: RoundType): Int {
        val recent = wakeDao.recentResults(type.name, limit = 20)
        val current = recent.firstOrNull()?.level ?: Levels.DEFAULT
        val atLevel = recent.filter { it.level == current }.map { PastRound(it.solveMs, it.misses) }
        return Difficulty.nextLevel(type, current, atLevel)
    }
}
