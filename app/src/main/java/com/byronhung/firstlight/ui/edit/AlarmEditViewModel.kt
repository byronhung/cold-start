package com.byronhung.firstlight.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byronhung.firstlight.alarm.WakeCheck
import com.byronhung.firstlight.alarm.Weekdays
import com.byronhung.firstlight.data.Alarm
import com.byronhung.firstlight.data.AlarmRepository
import com.byronhung.firstlight.data.FOLLOW_DEFAULT
import com.byronhung.firstlight.puzzle.Preset
import com.byronhung.firstlight.puzzle.WakeMethod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Everything the form holds, the time included: the wheel picker reports as it turns. */
data class EditDraft(
    val hour: Int,
    val minute: Int,
    val repeatDays: Int,
    val label: String,
    val wakeChecks: Int,
    /** [WakeMethod.code]. */
    val wakeMethod: Int,
    /** [Preset.code], or [FOLLOW_DEFAULT] to use the default from Settings. */
    val difficulty: Int,
    /** Null = the phone's default alarm sound. */
    val soundUri: String?,
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

    /** The shared wake-up code, so picking a scan method knows whether to open the scanner first. */
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
                    EditDraft(it.hour, it.minute, it.repeatDays, it.label, it.wakeChecks, it.wakeMethod, it.difficulty, it.soundUri, isNew = false)
                } ?: newDraft()
            }
        }
    }

    // New alarms get one wake check: enough to catch going back to sleep, rarely noticed when awake.
    /** A new alarm opens on the time it is now, so the wheels start close to wherever you're setting it. */
    private fun newDraft() = EditDraft(
        hour = LocalTime.now().hour, minute = LocalTime.now().minute, repeatDays = Weekdays.WEEKDAYS, label = "",
        wakeChecks = 1, wakeMethod = WakeMethod.PUZZLES.code, difficulty = FOLLOW_DEFAULT, soundUri = null, isNew = true,
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

    fun setSound(uri: String?) {
        draft = draft?.copy(soundUri = uri)
    }

    /** [code]: a [Preset.code], or [FOLLOW_DEFAULT]. */
    fun setDifficulty(code: Int) {
        draft = draft?.copy(difficulty = code)
    }

    /** The default difficulty from Settings, so the row can say "Normal · your default". */
    val defaultDifficulty: StateFlow<Preset> = repository.settings.map { Preset.of(it?.defaultDifficulty ?: Preset.NORMAL.code) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Preset.NORMAL)

    /** A scan method picked before any code exists: applied once the scanner returns one. */
    private var pendingMethod: WakeMethod? = null

    /** Returns true if the scanner must open first, because no wake-up code is registered yet. */
    fun pickMethod(method: WakeMethod): Boolean {
        if (method.usesScan && wakeCode.value == null) {
            pendingMethod = method
            return true
        }
        draft = draft?.copy(wakeMethod = method.code)
        return false
    }

    /** The scanner came back with a code: save it as the shared code and apply the waiting method. */
    fun codeScanned(code: String) {
        viewModelScope.launch { repository.setWakeCode(code) }
        pendingMethod?.let { m -> draft = draft?.copy(wakeMethod = m.code) }
        pendingMethod = null
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
                    wakeMethod = d.wakeMethod,
                    difficulty = d.difficulty,
                    soundUri = d.soundUri,
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
