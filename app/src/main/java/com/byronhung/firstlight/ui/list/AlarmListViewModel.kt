package com.byronhung.firstlight.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byronhung.firstlight.alarm.NextAlarm
import com.byronhung.firstlight.alarm.clockDigits
import com.byronhung.firstlight.alarm.nextAlarm
import com.byronhung.firstlight.alarm.period
import com.byronhung.firstlight.data.Alarm
import com.byronhung.firstlight.data.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    /** [com.byronhung.firstlight.puzzle.WakeMethod.code]. */
    val wakeMethod: Int,
    /** [com.byronhung.firstlight.puzzle.Preset.code]. */
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
                    wakeMethod = it.wakeMethod,
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

    private val _deleted = MutableStateFlow<List<Alarm>>(emptyList())

    /** The last batch deleted from the list, while its Undo is still on offer. */
    val deleted: StateFlow<List<Alarm>> = _deleted.asStateFlow()

    fun delete(ids: Set<Long>) {
        viewModelScope.launch { _deleted.value = repository.deleteAll(ids) }
    }

    fun undoDelete() {
        val back = _deleted.value
        _deleted.value = emptyList()
        viewModelScope.launch { repository.restore(back) }
    }

    fun dismissUndo() {
        _deleted.value = emptyList()
    }
}
