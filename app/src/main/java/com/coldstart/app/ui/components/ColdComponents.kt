package com.coldstart.app.ui.components

import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.coldstart.app.alarm.Weekdays
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdText
import java.time.DayOfWeek

val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * The line under an alarm's time: "M T W T F S S" with its repeat days lit, or "ONCE" —
 * plus " · GYM" when it has a label.
 */
@Composable
fun RepeatLine(repeatDays: Int, label: String) {
    Text(repeatLineText(repeatDays, label), style = ColdText.label)
}

private fun repeatLineText(repeatDays: Int, label: String): AnnotatedString = buildAnnotatedString {
    if (repeatDays == Weekdays.NONE) {
        withStyle(SpanStyle(color = ColdColors.InkMute)) { append("ONCE") }
    } else {
        DayOfWeek.entries.forEachIndexed { i, day ->
            val on = Weekdays.has(repeatDays, day)
            withStyle(SpanStyle(color = if (on) ColdColors.Accent else ColdColors.InkMute)) {
                append(DAY_LETTERS[i])
            }
            if (i < DAY_LETTERS.lastIndex) append(" ")
        }
    }
    if (label.isNotBlank()) {
        withStyle(SpanStyle(color = ColdColors.InkMute)) { append(" · ${label.uppercase()}") }
    }
}

@Composable
fun coldSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = ColdColors.Ground,
    checkedTrackColor = ColdColors.Accent,
    checkedBorderColor = ColdColors.Accent,
    uncheckedThumbColor = ColdColors.InkMute,
    uncheckedTrackColor = ColdColors.Line,
    uncheckedBorderColor = ColdColors.Line,
)
