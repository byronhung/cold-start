package com.byronhung.firstlight.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.byronhung.firstlight.R

/**
 * Sunrise type, from the approved prototype:
 *   Outfit  → every clock and time, and titles. Light weights at huge sizes (200 at 104sp).
 *   Figtree → everything read: prompts, captions, buttons, labels.
 * Neither is used by Grit. Both are variable fonts bundled in the app, so they work offline at 6am.
 */
@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: FontWeight) =
    Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val Outfit = FontFamily(
    listOf(FontWeight.ExtraLight, FontWeight.Light, FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold)
        .map { variable(R.font.outfit, it) },
)

val Figtree = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
        .map { variable(R.font.figtree, it) },
)

private val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

/** Named for where they're used, so a screen reads `AppText.heroClock`, not `displayLarge`. */
object AppText {
    /** The next-alarm clock on the list. */
    val heroClock = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.ExtraLight, fontSize = 104.sp, lineHeight = 100.sp, letterSpacing = (-0.04).em, lineHeightStyle = tight)

    /** The live clock while ringing. */
    val ringClock = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.ExtraLight, fontSize = 84.sp, lineHeight = 84.sp, letterSpacing = (-0.04).em, lineHeightStyle = tight)

    /** "Good morning", "Still awake?", "All quiet". */
    val display = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.ExtraLight, fontSize = 52.sp, lineHeight = 54.sp, letterSpacing = (-0.03).em)

    /** An alarm's time on its card. */
    val alarmTime = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = (-0.03).em, lineHeightStyle = tight)

    /** AM / PM beside a time. */
    val period = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 15.sp)

    /** Screen titles: "Settings". */
    val title = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.01).em)

    /** The app name and small headers: "First Light", "Edit alarm". */
    val header = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-0.01).em)

    /** The Stroop word. */
    val stroopWord = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 62.sp, lineHeight = 66.sp, letterSpacing = (-0.02).em)

    /** Wheel picker, centre row. */
    val wheel = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 34.sp)

    /** Uppercase labels: "NEXT ALARM", "ROUND 2 OF 5". Pass text already uppercased. */
    val label = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.16.em)

    val prompt = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    val body = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp)
    val bodyStrong = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    val caption = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val chip = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
    val day = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.06.em)
    val button = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

/** Material's slots pointed at Figtree, so any plain `Text()` matches. */
val AppTypography = Typography(
    bodyLarge = AppText.body,
    bodyMedium = AppText.caption,
    bodySmall = AppText.caption,
    labelLarge = AppText.button,
)
