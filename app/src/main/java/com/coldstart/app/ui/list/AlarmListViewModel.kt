package com.coldstart.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coldstart.app.alarm.clockDigits
import com.coldstart.app.alarm.nextAlarmSummary
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
    /** "06:30" or "6:30". */
    val time: String,
    /** "AM"/"PM" on a 12-hour phone, null on a 24-hour one. */
    val period: String?,
    val repeatDays: Int,
    val label: String,
    val enabled: Boolean,
)

data class AlarmListUi(
    val rows: List<AlarmRowUi>,
    /** "Next: tomorrow at 06:30 · in 7 h 49 m", or null when nothing is on. */
    val nextSummary: String?,
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

    /** Null until the database has answered, so the screen doesn't flash "no alarms". */
    val ui: StateFlow<AlarmListUi?> = combine(repository.alarms, minuteTicks) { alarms, _ ->
        AlarmListUi(
            rows = alarms.map {
                AlarmRowUi(
                    id = it.id,
                    time = clockDigits(it.hour, it.minute, is24Hour),
                    period = if (is24Hour) null else period(it.hour),
                    repeatDays = it.repeatDays,
                    label = it.label,
                    enabled = it.enabled,
                )
            },
            nextSummary = nextAlarmSummary(alarms, LocalDateTime.now(), is24Hour),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }
}
