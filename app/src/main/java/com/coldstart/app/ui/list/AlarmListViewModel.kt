package com.coldstart.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coldstart.app.alarm.formatTime
import com.coldstart.app.alarm.nextAlarmSummary
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
    val time: String,
    val repeatDays: Int,
    val label: String,
    val enabled: Boolean,
)

data class AlarmListUi(
    val rows: List<AlarmRowUi>,
    /** "Next: tomorrow at 06:30 · in 7 h 49 m", or null when nothing is on. */
    val nextSummary: String?,
)

class AlarmListViewModel(private val repository: AlarmRepository) : ViewModel() {

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
            rows = alarms.map { AlarmRowUi(it.id, formatTime(it.hour, it.minute), it.repeatDays, it.label, it.enabled) },
            nextSummary = nextAlarmSummary(alarms, LocalDateTime.now()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }
}
