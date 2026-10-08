package com.byronhung.firstlight.ui.plus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byronhung.firstlight.data.RoundType
import com.byronhung.firstlight.puzzle.Levels
import com.byronhung.firstlight.puzzle.PairsPuzzle
import com.byronhung.firstlight.puzzle.PathPuzzle
import com.byronhung.firstlight.puzzle.SlidePuzzle
import com.byronhung.firstlight.ui.components.AmberButton
import com.byronhung.firstlight.ui.components.GlassCard
import com.byronhung.firstlight.ui.components.PlusSheet
import com.byronhung.firstlight.ui.components.QuietButton
import com.byronhung.firstlight.ui.components.Segmented
import com.byronhung.firstlight.ui.components.SectionLabel
import com.byronhung.firstlight.ui.components.SkyBackground
import com.byronhung.firstlight.ui.theme.AppShapes
import com.byronhung.firstlight.ui.theme.AppText
import com.byronhung.firstlight.ui.theme.LocalSky
import com.byronhung.firstlight.ui.theme.SkyTheme
import com.byronhung.firstlight.ui.theme.Sun

/**
 * Try before you buy, from the approved mockup (claude.ai/artifact/3BdabayPCjA5BD57hGj6LC).
 * A locked puzzle opens a playable round at the easiest level; a locked sky opens full screen,
 * live. Neither unlocks anything for a real morning: they're previews, as often as people like.
 */

/** What Plus says each puzzle grows into, shown once the preview round is solved. */
private fun grows(type: RoundType): String = when (type) {
    RoundType.PAIRS -> "Solved. In Plus, Normal asks for 3 pairs and Hard for 4."
    RoundType.PATH -> "Lit. In Plus, boards grow to 4×4 and 5×5."
    RoundType.SLIDE -> "Out. In Plus, boards grow to 5×5 and 6×6."
    else -> "Solved."
}

/** A playable round of a Plus puzzle in a bottom sheet. [type] null = hidden. */
@Composable
fun BoxScope.PuzzlePreviewSheet(type: RoundType?, name: String, onGetPlus: () -> Unit, onDismiss: () -> Unit) {
    // Keep the last puzzle while the sheet animates out.
    var shown by remember { mutableStateOf(RoundType.PAIRS) }
    var shownName by remember { mutableStateOf("") }
    if (type != null) {
        shown = type
        shownName = name
    }
    var round by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    BackHandler(enabled = type != null, onBack = onDismiss)

    AnimatedVisibility(type != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF06061A).copy(alpha = 0.6f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    AnimatedVisibility(
        type != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF221B45), Color(0xFF171334))))
                // Taps inside the sheet must not fall through to the scrim and close it.
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.3f)),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(shownName, style = AppText.title.copy(fontSize = 24.sp), color = Sun.OnGlass, modifier = Modifier.weight(1f))
                SectionLabel("Plus · preview", color = Sun.ToggleLight)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.puzzle)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                // A new key deals a fresh board for "Another round".
                key(shown, round) {
                    if (solved) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(grows(shown), style = AppText.bodyStrong, color = Sun.Glow, textAlign = TextAlign.Center)
                            QuietButton("Another round", {
                                solved = false
                                round++
                            })
                        }
                    } else {
                        val done = { solved = true }
                        when (shown) {
                            RoundType.PAIRS -> PairsPuzzle(Levels.MIN, done)
                            RoundType.PATH -> PathPuzzle(Levels.MIN, done)
                            RoundType.SLIDE -> SlidePuzzle(Levels.MIN, done)
                            else -> Unit
                        }
                    }
                }
            }
            AmberButton("Get Plus · pay once", onGetPlus, Modifier.fillMaxWidth(), height = 54.dp)
            QuietButton("Close", {
                solved = false
                onDismiss()
            }, Modifier.fillMaxWidth())
        }
    }
}

private val SAMPLE_ALARMS = listOf("6:30" to "Work", "7:15" to "Gym", "8:00" to "Weekend", "9:30" to "", "5:45" to "Flight")

/**
 * A Plus sky, full screen and live: the real scene, sample alarm cards to scroll over it (so the
 * parallax shows), and Night / Dawn / Day, since the app picks from the clock.
 */
@Composable
fun SkyPreviewScreen(theme: SkyTheme, onBack: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    var plusReason by remember { mutableStateOf<String?>(null) }
    val sky = when (phase) {
        0 -> theme.night
        1 -> theme.dawn
        else -> theme.forHour(12)
    }
    SkyBackground(sky) {
        val ink = LocalSky.current
        LazyColumn(
            Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 220.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(Modifier.padding(start = 6.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("${theme.label} · Plus · preview", color = ink.sunInk)
                    Text("6:30", style = AppText.heroClock, color = ink.ink)
                    Text("Touch the sky. Scroll the list.", style = AppText.body, color = ink.dim)
                }
            }
            items(SAMPLE_ALARMS) { (time, label) ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.Bottom) {
                        Text(time, style = AppText.alarmTime, color = ink.ink)
                        Text(
                            if (label.isBlank()) "AM" else "AM · $label",
                            style = AppText.caption,
                            color = ink.dim,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp).weight(1f),
                        )
                    }
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xC005040E))))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Segmented(listOf("Night", "Dawn", "Day"), phase, onSelect = { phase = it })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuietButton("Back", onBack)
                Spacer(Modifier.width(0.dp))
                AmberButton("Get Plus · pay once", { plusReason = "${theme.label} is a Plus sky." }, Modifier.weight(1f), height = 54.dp)
            }
        }
        PlusSheet(reason = plusReason, onDismiss = { plusReason = null })
    }
}
