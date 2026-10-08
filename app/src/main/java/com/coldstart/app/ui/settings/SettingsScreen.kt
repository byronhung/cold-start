package com.coldstart.app.ui.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.coldstart.app.BuildConfig
import com.coldstart.app.data.AppSettings
import com.coldstart.app.data.RoundType
import com.coldstart.app.puzzle.CatalogPuzzle
import com.coldstart.app.puzzle.MIN_MIX
import com.coldstart.app.puzzle.PUZZLE_CATALOG
import com.coldstart.app.puzzle.Preset
import com.coldstart.app.puzzle.effectiveMix
import com.coldstart.app.puzzle.parseMix
import com.coldstart.app.ui.components.PlusSheet
import com.coldstart.app.ui.components.Segmented
import com.coldstart.app.ui.components.SpringToggle
import com.coldstart.app.ui.components.springClick
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.coldstart.app.data.AlarmRepository
import com.coldstart.app.ui.components.AmberButton
import com.coldstart.app.ui.components.GlassCard
import com.coldstart.app.ui.components.IconSquareButton
import com.coldstart.app.ui.components.QuietButton
import com.coldstart.app.ui.components.SectionLabel
import com.coldstart.app.ui.components.SkyBackground
import com.coldstart.app.ui.components.SunIcons
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.LocalSky
import com.coldstart.app.ui.theme.LocalSkyTheme
import com.coldstart.app.ui.theme.Motion
import com.coldstart.app.ui.theme.SkyTheme
import com.coldstart.app.ui.theme.Space
import com.coldstart.app.ui.theme.Sun
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUi(
    val wakeCode: String?,
    val alarmsUsingIt: Int,
    /** The mix as saved (what the picker shows as on). */
    val mix: List<RoundType>,
    val defaultDifficulty: Preset,
    val isPlus: Boolean,
    val theme: SkyTheme,
)

class SettingsViewModel(private val repository: AlarmRepository) : ViewModel() {
    val ui: StateFlow<SettingsUi?> = combine(repository.settings, repository.alarms) { stored, alarms ->
        val settings = stored ?: AppSettings()
        SettingsUi(
            wakeCode = settings.wakeCode,
            alarmsUsingIt = alarms.count { it.wakeMethod != 0 },
            mix = effectiveMix(parseMix(settings.puzzleMix), settings.isPlus),
            defaultDifficulty = Preset.of(settings.defaultDifficulty),
            isPlus = settings.isPlus,
            theme = SkyTheme.effective(settings.theme, settings.isPlus),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setCode(code: String?) {
        viewModelScope.launch { repository.setWakeCode(code) }
    }

    /** Turns a puzzle on or off. Returns false (and changes nothing) if that would leave fewer than [MIN_MIX]. */
    fun toggle(type: RoundType): Boolean {
        val now = ui.value?.mix ?: return false
        val next = if (type in now) now - type else now + type
        if (next.size < MIN_MIX) return false
        viewModelScope.launch { repository.setPuzzleMix(next) }
        return true
    }

    fun setDefaultDifficulty(preset: Preset) {
        viewModelScope.launch { repository.setDefaultDifficulty(preset) }
    }

    fun setTheme(theme: SkyTheme) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    fun setPlus(on: Boolean) {
        viewModelScope.launch { repository.setPlus(on) }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onScan: () -> Unit, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var plusReason by remember { mutableStateOf<String?>(null) }
    var mixNote by remember { mutableStateOf<String?>(null) }
    SkyBackground(LocalSkyTheme.current.night, calm = true) {
        val sky = LocalSky.current
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconSquareButton(SunIcons.Back, "Back", onBack)
                Text("Settings", style = ColdText.title, color = sky.ink)
            }

            val current = ui
            if (current != null) {
                if (!current.isPlus) PlusTeaser { plusReason = "Three more puzzles and four new skies." }

                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            SectionLabel("Puzzle mix", Modifier.weight(1f))
                            Text("${current.mix.size} of ${PUZZLE_CATALOG.size} on", style = ColdText.caption.copy(fontSize = 12.5.sp), color = sky.mute)
                        }
                        PUZZLE_CATALOG.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { entry ->
                                    MixTile(
                                        entry = entry,
                                        on = entry.type != null && entry.type in current.mix,
                                        isPlus = current.isPlus,
                                        modifier = Modifier.weight(1f),
                                        onTap = {
                                            when {
                                                entry.plus && !current.isPlus -> plusReason = "${entry.name} is part of Cold Start Plus."
                                                entry.type == null -> mixNote = "${entry.name} is coming soon."
                                                !viewModel.toggle(entry.type) -> mixNote = "At least $MIN_MIX stay on, so mornings never just alternate."
                                                else -> mixNote = null
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        Text(
                            mixNote ?: if (current.isPlus) "Every alarm draws from these. At least $MIN_MIX stay on."
                            else "Every alarm draws from these three. Plus adds three more to mix in.",
                            style = ColdText.caption.copy(fontSize = 12.5.sp),
                            color = sky.dim,
                        )
                    }
                }

                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel("Default difficulty")
                        Segmented(
                            listOf("Gentle", "Normal", "Hard"),
                            Preset.entries.indexOf(current.defaultDifficulty),
                            onSelect = { viewModel.setDefaultDifficulty(Preset.entries[it]) },
                        )
                        Text(
                            "Every alarm uses this unless you change that one alarm.",
                            style = ColdText.caption.copy(fontSize = 12.5.sp),
                            color = sky.dim,
                        )
                    }
                }

                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel("Theme")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SkyTheme.entries.forEach { theme ->
                                val locked = theme.plus && !current.isPlus
                                ThemeSwatch(
                                    theme = theme,
                                    selected = theme == current.theme,
                                    locked = locked,
                                    onTap = {
                                        if (locked) plusReason = "${theme.label} is a Plus sky." else viewModel.setTheme(theme)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionLabel("Your wake-up code")
                    val code = ui?.wakeCode
                    if (code != null) {
                        BarcodeArt(code)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(code, style = ColdText.header.copy(fontSize = 22.sp, letterSpacing = 0.04.em), color = sky.ink)
                            val n = ui?.alarmsUsingIt ?: 0
                            Text(
                                if (n == 0) "Not used yet. Pick Scan or Both under “How to wake up” on an alarm."
                                else "Used by $n alarm" + if (n == 1) "" else "s",
                                style = ColdText.caption,
                                color = sky.dim,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AmberButton("Scan a new code", onScan, Modifier.weight(1f))
                            QuietButton("Remove", { viewModel.setCode(null) }, Modifier.weight(1f))
                        }
                    } else {
                        Text(
                            "No code yet. Register one and any alarm can end with walking to it.",
                            style = ColdText.body,
                            color = sky.dim,
                        )
                        AmberButton("Register a code", onScan, Modifier.fillMaxWidth())
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionLabel("How it works")
                    Step(1, "Scan any barcode or QR code that lives away from your bed. Toothpaste, a cereal box, the kettle. Nothing to print.")
                    Step(2, "Cold Start saves the number under the bars. It never looks the product up.")
                    Step(3, "Alarms set to Scan or Both stop only when you walk there and scan it.")
                }
            }

            Text(
                "Every tube of the same toothpaste has the same barcode, so a new one still works. Don't keep a spare by the bed.",
                style = ColdText.caption.copy(fontSize = 13.sp),
                color = sky.mute,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            // Debug builds only: flip Plus on and off until Google Play Billing replaces this (P11).
            if (BuildConfig.DEBUG && ui != null) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Debug: Plus", style = ColdText.bodyStrong, color = sky.ink)
                            Text("Not in release builds.", style = ColdText.caption, color = sky.mute)
                        }
                        SpringToggle(ui?.isPlus == true, { viewModel.setPlus(it) }, "Debug Plus")
                    }
                }
            }
        }

        PlusSheet(reason = plusReason, onDismiss = { plusReason = null })
    }
}

@Composable
private fun Step(n: Int, text: String) {
    val sky = LocalSky.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Sun.ToggleLight.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Text(n.toString(), style = ColdText.chip.copy(fontSize = 13.sp), color = Sun.ToggleLight) }
        Text(text, style = ColdText.body.copy(fontSize = 15.sp), color = sky.ink.copy(alpha = 0.85f))
    }
}

/** A barcode drawn from your actual code's digits, with the prototype's moving scan line. */
@Composable
private fun BarcodeArt(code: String) {
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1_200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "line")
    val ink = Color(0xFF2A1D3A)
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(ColdShapes.button)
            .background(Color(0xFFFFF6EC)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 24.dp),
        ) {
            // Each digit becomes a bar and a gap of its own widths: the same code always looks the same.
            val units = code.flatMap { c -> val d = c.code % 10; listOf(1 + d % 3, 1 + (d / 3) % 3) }
            val unit = size.width / units.sum().coerceAtLeast(1)
            var x = 0f
            units.forEachIndexed { i, u ->
                if (i % 2 == 0) drawRect(ink, Offset(x, 0f), Size(u * unit, size.height))
                x += u * unit
            }
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            val ly = size.height * y
            drawLine(Sun.Rose.copy(alpha = 0.35f), Offset(0f, ly), Offset(size.width, ly), strokeWidth = 10.dp.toPx())
            drawLine(Sun.Rose, Offset(0f, ly), Offset(size.width, ly), strokeWidth = 2.dp.toPx())
        }
    }
}

/** One puzzle in the mix picker: amber when on, a lock when it's Plus and not owned. */
@Composable
private fun MixTile(entry: CatalogPuzzle, on: Boolean, isPlus: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val sky = LocalSky.current
    val locked = entry.plus && !isPlus
    val soon = entry.type == null && !locked
    Box(
        modifier
            .springClick(0.92f, onClick = onTap)
            .height(74.dp)
            .clip(ColdShapes.small)
            .background(if (on) Sun.ToggleLight.copy(alpha = 0.16f) else sky.card)
            .border(1.dp, if (on) Sun.ToggleLight.copy(alpha = 0.6f) else sky.cardEdge, ColdShapes.small)
            .semantics { contentDescription = entry.name + when { locked -> ", Plus"; soon -> ", coming soon"; on -> ", on"; else -> ", off" } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.name, style = ColdText.chip.copy(fontSize = 12.sp), color = sky.ink.copy(alpha = if (locked || soon) 0.55f else 1f))
            if (soon) Text("Coming soon", style = ColdText.chip.copy(fontSize = 10.sp), color = sky.mute)
        }
        if (locked) {
            Icon(SunIcons.Lock, contentDescription = null, tint = Sun.ToggleLight, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(12.dp))
        }
    }
}

/** One sky in the Theme row: its night-to-dawn colours, amber-ringed when chosen, a lock when it's Plus. */
@Composable
private fun ThemeSwatch(theme: SkyTheme, selected: Boolean, locked: Boolean, onTap: () -> Unit) {
    val sky = LocalSky.current
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, Motion.bouncy(), label = "swatch")
    Column(
        Modifier
            .springClick(0.9f, onClick = onTap)
            .semantics(mergeDescendants = true) {
                contentDescription = theme.label + when { locked -> ", Plus"; selected -> ", chosen"; else -> "" }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (locked) 0.55f else 1f
                }
                .size(52.dp)
                .clip(ColdShapes.small)
                .background(Brush.linearGradient(theme.swatch))
                .border(2.dp, if (selected) Sun.ToggleLight else Color.Transparent, ColdShapes.small),
        ) {
            if (locked) {
                Icon(SunIcons.Lock, contentDescription = null, tint = Sun.ToggleLight, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(12.dp))
            }
        }
        // "Neon city" is too long for a 52 dp swatch; the first word is the name people use.
        Text(theme.label.substringBefore(' '), style = ColdText.chip.copy(fontSize = 11.sp), color = sky.ink.copy(alpha = if (locked) 0.6f else 1f))
    }
}

/** The Settings banner for free users: one line, opens the Plus sheet. */
@Composable
private fun PlusTeaser(onClick: () -> Unit) {
    val sky = LocalSky.current
    Row(
        Modifier
            .fillMaxWidth()
            .springClick(0.97f, onClick = onClick)
            .clip(ColdShapes.button)
            .background(Brush.linearGradient(listOf(Sun.AmberLight.copy(alpha = 0.22f), Sun.Amber.copy(alpha = 0.22f))))
            .border(1.dp, Sun.ToggleLight.copy(alpha = 0.45f), ColdShapes.button)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Cold Start Plus", style = ColdText.bodyStrong, color = sky.ink)
            Text("3 puzzles and 4 skies. Pay once.", style = ColdText.caption, color = sky.dim)
        }
        Text("See", style = ColdText.bodyStrong, color = Sun.ToggleLight)
    }
}
