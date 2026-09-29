package com.coldstart.app.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coldstart.app.alarm.Weekdays
import com.coldstart.app.data.Alarm
import com.coldstart.app.data.AlarmRepository
import kotlinx.coroutines.launch

/** What the form holds. The time itself lives in the time picker's own state until Save. */
data class EditDraft(
    val hour: Int,
    val minute: Int,
    val repeatDays: Int,
    val label: String,
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

    private var original: Alarm? = null
    private var busy = false

    init {
        if (alarmId == null) {
            draft = newDraft()
        } else {
            viewModelScope.launch {
                val alarm = repository.get(alarmId)
                original = alarm
                draft = alarm?.let { EditDraft(it.hour, it.minute, it.repeatDays, it.label, isNew = false) }
                    ?: newDraft()
            }
        }
    }

    private fun newDraft() = EditDraft(hour = 7, minute = 0, repeatDays = Weekdays.WEEKDAYS, label = "", isNew = true)

    /** [dayIndex] 0 = Monday … 6 = Sunday. */
    fun toggleDay(dayIndex: Int) {
        draft = draft?.let { it.copy(repeatDays = it.repeatDays xor (1 shl dayIndex)) }
    }

    fun setLabel(value: String) {
        draft = draft?.copy(label = value.take(MAX_LABEL))
    }

    /** Saving always turns the alarm on: you just set it, so you want it. */
    fun save(hour: Int, minute: Int, onDone: () -> Unit) {
        val d = draft ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            val base = original ?: Alarm(hour = hour, minute = minute)
            repository.save(
                base.copy(
                    hour = hour,
                    minute = minute,
                    repeatDays = d.repeatDays,
                    label = d.label.trim(),
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
