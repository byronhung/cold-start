package com.coldstart.app.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coldstart.app.alarm.WakeCheck
import com.coldstart.app.alarm.Weekdays
import com.coldstart.app.data.Alarm
import com.coldstart.app.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the form holds, the time included: the wheel picker reports as it turns. */
data class EditDraft(
    val hour: Int,
    val minute: Int,
    val repeatDays: Int,
    val label: String,
    val wakeChecks: Int,
    val finishWithScan: Boolean,
    val isNew: Boolean,
)

/** [alarmId] null = adding a new alarm. */
class AlarmEditViewModel(
    private val repository: AlarmRepository,
    private val alarmId: Long?,
) : ViewModel() {

    /** Null while an existing alarm is still loading. */
    var draft by mutableStateOf<EditDraft?>(null)
        private set

    /** The shared wake-up code, so the scan toggle can say whether one is registered. */
    val wakeCode: StateFlow<String?> = repository.settings.map { it?.wakeCode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var original: Alarm? = null
    private var busy = false

    init {
        if (alarmId == null) {
            draft = newDraft()
        } else {
            viewModelScope.launch {
                val alarm = repository.get(alarmId)
                original = alarm
                draft = alarm?.let {
                    EditDraft(it.hour, it.minute, it.repeatDays, it.label, it.wakeChecks, it.finishWithScan, isNew = false)
                } ?: newDraft()
            }
        }
    }

    // New alarms get one wake check: enough to catch going back to sleep, rarely noticed when awake.
    private fun newDraft() = EditDraft(
        hour = 7, minute = 0, repeatDays = Weekdays.WEEKDAYS, label = "",
        wakeChecks = 1, finishWithScan = false, isNew = true,
    )

    fun setTime(hour: Int, minute: Int) {
        draft = draft?.copy(hour = hour, minute = minute)
    }

    /** [dayIndex] 0 = Monday … 6 = Sunday. */
    fun toggleDay(dayIndex: Int) {
        draft = draft?.let { it.copy(repeatDays = it.repeatDays xor (1 shl dayIndex)) }
    }

    fun setLabel(value: String) {
        draft = draft?.copy(label = value.take(MAX_LABEL))
    }

    fun setWakeChecks(count: Int) {
        draft = draft?.copy(wakeChecks = count.coerceIn(0, WakeCheck.MAX))
    }

    fun setFinishWithScan(on: Boolean) {
        draft = draft?.copy(finishWithScan = on)
    }

    /** Saving always turns the alarm on: you just set it, so you want it. */
    fun save(onDone: () -> Unit) {
        val d = draft ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            val base = original ?: Alarm(hour = d.hour, minute = d.minute)
            repository.save(
                base.copy(
                    hour = d.hour,
                    minute = d.minute,
                    repeatDays = d.repeatDays,
                    label = d.label.trim(),
                    wakeChecks = d.wakeChecks,
                    finishWithScan = d.finishWithScan,
                    enabled = true,
                ),
            )
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = original?.id ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            repository.delete(id)
            onDone()
        }
    }

    companion object {
        const val MAX_LABEL = 24
    }
}
