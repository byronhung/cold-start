package com.coldstart.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coldstart.app.alarm.NextAlarm
import com.coldstart.app.alarm.clockDigits
import com.coldstart.app.alarm.nextAlarm
import com.coldstart.app.alarm.period
import com.coldstart.app.data.AlarmRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class AlarmRowUi(
    val id: Long,
    /** "6:30" or "06:30". */
    val time: String,
    /** "AM"/"PM" on a 12-hour phone, null on a 24-hour one. */
    val period: String?,
    val repeatDays: Int,
    val label: String,
    val enabled: Boolean,
    val wakeChecks: Int,
    val scan: Boolean,
    /** [com.coldstart.app.puzzle.Preset.code]. */
    val difficulty: Int = 1,
)

data class AlarmListUi(
    val rows: List<AlarmRowUi>,
    /** The hero: next alarm, or null when nothing is on. */
    val next: NextAlarm?,
    /** Picks the sky. Updated every minute along with the countdown. */
    val hour: Int,
)

/** [is24Hour] follows the phone's own clock setting. */
class AlarmListViewModel(
    private val repository: AlarmRepository,
    private val is24Hour: Boolean,
) : ViewModel() {

    /** Fires once now, then on every minute boundary, so "in 7 h 49 m" keeps counting down. */
    private val minuteTicks = flow {
        while (true) {
            emit(Unit)
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }

    /** Null until the database has answered, so the screen doesn't flash "all quiet". */
    val ui: StateFlow<AlarmListUi?> = combine(repository.alarms, minuteTicks) { alarms, _ ->
        val now = LocalDateTime.now()
        AlarmListUi(
            rows = alarms.map {
                AlarmRowUi(
                    id = it.id,
                    time = clockDigits(it.hour, it.minute, is24Hour),
                    period = if (is24Hour) null else period(it.hour),
                    repeatDays = it.repeatDays,
                    label = it.label,
                    enabled = it.enabled,
                    wakeChecks = it.wakeChecks,
                    scan = it.finishWithScan,
                    difficulty = it.difficulty,
                )
            },
            next = nextAlarm(alarms, now, is24Hour),
            hour = now.hour,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }
}
