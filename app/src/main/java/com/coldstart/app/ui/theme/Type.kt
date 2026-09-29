package com.coldstart.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.coldstart.app.R

/**
 * Three faces, each with one role — the same split as the brief:
 *
 *   Bricolage Grotesque → display: screen titles, the Stroop word
 *   Instrument Sans     → everything read: prompts, captions, settings
 *   JetBrains Mono      → every number and label: clock, alarm times, "ROUND 2 OF 3"
 *
 * All three are variable fonts: one file holds every weight. Mono is for anything that counts,
 * so digits don't jitter as the clock ticks.
 */

// Bricolage has an optical-size axis. Display text is always big, so pin it to the big-text
// cut (tighter spacing, finer detail) rather than the default 14pt one.
@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: FontWeight) = Font(
    R.font.bricolage_grotesque,
    weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("opsz", 48f),
    ),
)

val Bricolage = FontFamily(bricolage(FontWeight.Medium), bricolage(FontWeight.Bold))

val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans, FontWeight.Normal),
    Font(R.font.instrument_sans, FontWeight.Medium),
    Font(R.font.instrument_sans, FontWeight.SemiBold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

/** Named for what they're used for, so a screen reads as `ColdText.alarmTime`, not `headlineSmall`. */
object ColdText {
    /** Screen titles: "Alarms". */
    val title = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp,
    )

    /** The Stroop word. The biggest thing on a puzzle screen. */
    val stroopWord = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
        fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1).sp,
    )

    /** The live clock on the ringing screen. */
    val clock = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium,
        fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-2).sp,
    )

    /** An alarm's time in the list. */
    val alarmTime = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium,
        fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp,
    )

    /** Uppercase labels: "ROUND 2 OF 3", repeat days. Pass text already uppercased. */
    val label = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.3.sp,
    )

    /** Puzzle instructions: "Tap the ink colour". Four words at most. */
    val prompt = TextStyle(
        fontFamily = InstrumentSans, fontWeight = FontWeight.Medium,
        fontSize = 17.sp, lineHeight = 22.sp,
    )

    val body = TextStyle(
        fontFamily = InstrumentSans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    )

    val caption = TextStyle(
        fontFamily = InstrumentSans, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp,
    )
}

/** Material's slots, pointed at our faces, so any plain `Text()` is Instrument Sans by default. */
val ColdTypography = Typography(
    bodyLarge = ColdText.body,
    bodyMedium = ColdText.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = ColdText.caption,
    titleLarge = ColdText.title,
    labelSmall = ColdText.label,
)
