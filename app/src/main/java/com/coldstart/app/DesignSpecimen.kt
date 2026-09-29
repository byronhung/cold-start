package com.coldstart.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdStartTheme
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space

/**
 * Chunk 01's check: one piece of each screen from the brief, drawn with the real theme.
 * Nothing here is interactive. The alarm list replaces it in chunk 02.
 */
@Composable
fun DesignSpecimen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColdColors.Ground)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen, vertical = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text("CHUNK 01 · DESIGN SYSTEM", style = ColdText.label, color = ColdColors.Accent)
            Text("Cold Start", style = ColdText.title, color = ColdColors.Ink)
            Text(
                "Next: tomorrow at 06:30 · in 7 h 49 m",
                style = ColdText.caption,
                color = ColdColors.InkMute,
            )
        }

        // --- alarm list rows ---
        Column {
            AlarmRowSpecimen("06:30", onDays = setOf(0, 1, 2, 3, 4), enabled = true)
            AlarmRowSpecimen("08:15", onDays = setOf(5, 6), enabled = true)
            AlarmRowSpecimen("05:45", onDays = emptySet(), enabled = false, note = "ONCE · GYM")
            HorizontalDivider(color = ColdColors.Line)
        }

        // --- ringing screen: round frame + Stroop ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColdColors.Surface, ColdShapes.medium)
                .padding(Space.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Text("06:31", style = ColdText.clock, color = ColdColors.Ink)
            Pips(done = 1, current = 1, total = 3)
            Text("ROUND 2 OF 3", style = ColdText.label, color = ColdColors.InkMute)
            Spacer(Modifier.height(Space.xs))
            Text("Tap the ink colour", style = ColdText.prompt, color = ColdColors.InkDim)
            Text("RED", style = ColdText.stroopWord, color = ColdColors.PuzzleBlue)
            Swatches()
            Spacer(Modifier.height(Space.xs))
            HoldBar(fraction = 10f / 30f, text = "Hold to give up · 10 of 30 s")
        }

        // --- the palette, named ---
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text("PALETTE", style = ColdText.label, color = ColdColors.InkMute)
            Chip("Accent · done, on", ColdColors.Accent)
            Chip("Danger · give up only", ColdColors.Danger)
            Chip("Ink · text", ColdColors.Ink)
            Chip("Ink dim · secondary", ColdColors.InkDim)
            Chip("Ink mute · labels", ColdColors.InkMute)
        }

        Text(
            "Instrument Sans for reading. Bricolage for titles. JetBrains Mono for 0123456789.",
            style = ColdText.body,
            color = ColdColors.InkDim,
        )
    }
}

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
private fun AlarmRowSpecimen(time: String, onDays: Set<Int>, enabled: Boolean, note: String? = null) {
    HorizontalDivider(color = ColdColors.Line)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(time, style = ColdText.alarmTime, color = if (enabled) ColdColors.Ink else ColdColors.InkMute)
            if (note != null) {
                Text(note, style = ColdText.label, color = ColdColors.InkMute)
            } else {
                DayLetters(onDays)
            }
        }
        Toggle(enabled)
    }
}

/** "M T W T F S S", with the days the alarm repeats on in accent. */
@Composable
private fun DayLetters(onDays: Set<Int>) {
    Text(
        text = buildAnnotatedString {
            DAY_LETTERS.forEachIndexed { i, letter ->
                withStyle(SpanStyle(color = if (i in onDays) ColdColors.Accent else ColdColors.InkMute)) {
                    append(letter)
                }
                if (i < DAY_LETTERS.lastIndex) append(" ")
            }
        },
        style = ColdText.label,
    )
}

@Composable
private fun Toggle(on: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .background(if (on) ColdColors.Accent else ColdColors.Line, ColdShapes.pill)
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .background(if (on) ColdColors.Ground else ColdColors.InkMute, CircleShape),
        )
    }
}

@Composable
private fun Pips(done: Int, current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val colour = when {
                i < done -> ColdColors.Accent
                i == current -> ColdColors.Ink
                else -> ColdColors.Line
            }
            Box(
                Modifier
                    .size(width = 32.dp, height = 5.dp)
                    .background(colour, ColdShapes.pill),
            )
        }
    }
}

@Composable
private fun Swatches() {
    val colours = listOf(ColdColors.PuzzleRed, ColdColors.PuzzleBlue, ColdColors.PuzzleGreen, ColdColors.PuzzleYellow)
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        colours.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                pair.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(width = 130.dp, height = 56.dp)
                            .background(ColdColors.Ground, ColdShapes.small)
                            .border(1.dp, ColdColors.Line, ColdShapes.small),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(c, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldBar(fraction: Float, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(ColdShapes.pill)
            .border(1.dp, ColdColors.Line, ColdShapes.pill),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Square-edged fill, clipped by the pill: it reads as the bar filling up, not a shape moving.
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(ColdColors.Danger.copy(alpha = 0.22f)),
        )
        Text(
            text,
            style = ColdText.caption,
            color = ColdColors.InkMute,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun Chip(name: String, colour: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(18.dp)
                .background(colour, ColdShapes.small),
        )
        Spacer(Modifier.width(Space.md))
        Text(name, style = ColdText.caption, color = ColdColors.InkDim)
    }
}

@Preview(widthDp = 390, heightDp = 1400)
@Composable
private fun DesignSpecimenPreview() = ColdStartTheme { DesignSpecimen() }
