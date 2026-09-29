package com.coldstart.app.ui.edit

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coldstart.app.alarm.Weekdays
import com.coldstart.app.ui.components.DAY_LETTERS
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space
import java.time.DayOfWeek

@Composable
fun AlarmEditScreen(viewModel: AlarmEditViewModel, onScan: () -> Unit, onDone: () -> Unit) {
    val draft = viewModel.draft
    if (draft == null) {
        // Existing alarm still loading: plain ground, no flash of a default form.
        Box(
            Modifier
                .fillMaxSize()
                .background(ColdColors.Ground),
        )
        return
    }
    AlarmEditContent(
        draft = draft,
        onBack = onDone,
        onToggleDay = viewModel::toggleDay,
        onLabel = viewModel::setLabel,
        onScan = onScan,
        onRemoveCode = { viewModel.setQrCode(null) },
        onSave = { h, m -> viewModel.save(h, m, onDone) },
        onDelete = { viewModel.delete(onDone) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditContent(
    draft: EditDraft,
    onBack: () -> Unit,
    onToggleDay: (Int) -> Unit,
    onLabel: (String) -> Unit,
    onScan: () -> Unit,
    onRemoveCode: () -> Unit,
    onSave: (Int, Int) -> Unit,
    onDelete: () -> Unit,
) {
    // The picker owns the time while editing; it's only read back on Save.
    // Follow the phone's clock setting: AM/PM switch on a 12-hour phone, 0–23 dial on a 24-hour one.
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val time = rememberTimePickerState(initialHour = draft.hour, initialMinute = draft.minute, is24Hour = is24Hour)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColdColors.Ground)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen, vertical = Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ColdColors.Ink)
            }
            Text(
                if (draft.isNew) "New alarm" else "Edit alarm",
                style = ColdText.title,
                color = ColdColors.Ink,
            )
        }

        TimePicker(
            state = time,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = TimePickerDefaults.colors(
                clockDialColor = ColdColors.Surface,
                clockDialSelectedContentColor = ColdColors.Ground,
                clockDialUnselectedContentColor = ColdColors.InkDim,
                selectorColor = ColdColors.Accent,
                containerColor = ColdColors.Ground,
                timeSelectorSelectedContainerColor = ColdColors.Accent.copy(alpha = 0.18f),
                timeSelectorUnselectedContainerColor = ColdColors.Surface,
                timeSelectorSelectedContentColor = ColdColors.Accent,
                timeSelectorUnselectedContentColor = ColdColors.Ink,
                periodSelectorBorderColor = ColdColors.Line,
                periodSelectorSelectedContainerColor = ColdColors.Accent.copy(alpha = 0.18f),
                periodSelectorUnselectedContainerColor = ColdColors.Surface,
                periodSelectorSelectedContentColor = ColdColors.Accent,
                periodSelectorUnselectedContentColor = ColdColors.InkDim,
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Text("REPEAT", style = ColdText.label, color = ColdColors.InkMute)
            DayPicker(draft.repeatDays, onToggleDay)
            Text(
                if (draft.repeatDays == Weekdays.NONE) "No days picked: rings once." else " ",
                style = ColdText.caption,
                color = ColdColors.InkMute,
            )
        }

        OutlinedTextField(
            value = draft.label,
            onValueChange = onLabel,
            singleLine = true,
            placeholder = { Text("Label (optional)", color = ColdColors.InkMute) },
            textStyle = ColdText.body,
            shape = ColdShapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColdColors.Accent,
                unfocusedBorderColor = ColdColors.Line,
                cursorColor = ColdColors.Accent,
                focusedTextColor = ColdColors.Ink,
                unfocusedTextColor = ColdColors.Ink,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        ScanSection(draft.qrCode, onScan, onRemoveCode)

        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Button(
                onClick = { onSave(time.hour, time.minute) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColdColors.Accent,
                    contentColor = ColdColors.Ground,
                ),
                shape = ColdShapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Save alarm", style = ColdText.prompt)
            }
            if (!draft.isNew) {
                // Deliberately not red: red means "give up" in this app and nothing else.
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete alarm", style = ColdText.body, color = ColdColors.InkMute)
                }
            }
        }
    }
}

@Composable
private fun DayPicker(repeatDays: Int, onToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DayOfWeek.entries.forEachIndexed { i, day ->
            val on = Weekdays.has(repeatDays, day)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(if (on) ColdColors.Accent else ColdColors.Ground, CircleShape)
                    .border(1.dp, if (on) ColdColors.Accent else ColdColors.Line, CircleShape)
                    .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle(i) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    DAY_LETTERS[i],
                    style = ColdText.label,
                    color = if (on) ColdColors.Ground else ColdColors.InkDim,
                )
            }
        }
    }
}

/** Optional last round: walk to a code you registered and scan it. */
@Composable
private fun ScanSection(qrCode: String?, onScan: () -> Unit, onRemove: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text("FINISH WITH A SCAN", style = ColdText.label, color = ColdColors.InkMute)
        if (qrCode == null) {
            Text(
                "Optional. Register a barcode or QR code away from your bed. The last round is walking to it and scanning it.",
                style = ColdText.caption,
                color = ColdColors.InkMute,
            )
            OutlinedButton(
                onClick = onScan,
                shape = ColdShapes.small,
                border = BorderStroke(1.dp, ColdColors.Line),
            ) { Text("Register a code", style = ColdText.body, color = ColdColors.Ink) }
        } else {
            Text(
                "Code registered. This alarm ends with scanning it.",
                style = ColdText.caption,
                color = ColdColors.Accent,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                OutlinedButton(
                    onClick = onScan,
                    shape = ColdShapes.small,
                    border = BorderStroke(1.dp, ColdColors.Line),
                ) { Text("Use a different code", style = ColdText.caption, color = ColdColors.Ink) }
                TextButton(onClick = onRemove) {
                    Text("Remove", style = ColdText.caption, color = ColdColors.InkMute)
                }
            }
        }
    }
}
