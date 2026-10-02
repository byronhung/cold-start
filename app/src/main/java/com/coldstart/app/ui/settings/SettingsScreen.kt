package com.coldstart.app.ui.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import com.coldstart.app.ui.theme.Skies
import com.coldstart.app.ui.theme.Space
import com.coldstart.app.ui.theme.Sun
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUi(val wakeCode: String?, val alarmsUsingIt: Int)

class SettingsViewModel(private val repository: AlarmRepository) : ViewModel() {
    val ui: StateFlow<SettingsUi?> = combine(repository.settings, repository.alarms) { settings, alarms ->
        SettingsUi(settings?.wakeCode, alarms.count { it.wakeMethod != 0 })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setCode(code: String?) {
        viewModelScope.launch { repository.setWakeCode(code) }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onScan: () -> Unit, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    SkyBackground(Skies.Night) {
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
        }
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
