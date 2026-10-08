package com.coldstart.app.ring

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coldstart.app.ColdStartApp
import com.coldstart.app.alarm.WakeCheck
import com.coldstart.app.alarm.putCheck
import com.coldstart.app.alarm.toCheck
import com.coldstart.app.ui.components.SkyBackground
import com.coldstart.app.ui.components.SunIcons
import com.coldstart.app.ui.components.springClick
import com.coldstart.app.ui.theme.ColdStartTheme
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.LocalSky
import com.coldstart.app.ui.theme.Motion
import com.coldstart.app.ui.theme.LocalSkyTheme
import com.coldstart.app.ui.theme.Sun
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Still awake?" One big button and a countdown ring. Tapping in time passes the check; running
 * out means WakeCheckReceiver's deadline fires and the alarm rings again. Opened only when the
 * phone was locked or dark: a phone in use passes the check without showing this.
 */
class WakeCheckActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val check = intent.toCheck()
        val deadline = intent.getLongExtra(EXTRA_DEADLINE, 0L)
        if (check == null || System.currentTimeMillis() >= deadline) {
            finish()
            return
        }
        val app = application as ColdStartApp
        setContent {
            val skyTheme by app.skyTheme.collectAsState()
            ColdStartTheme(skyTheme) {
                WakeCheckScreen(
                    check = check,
                    deadline = deadline,
                    onConfirm = {
                        Notifications.cancelWakeCheck(this)
                        app.appScope.launch { app.repository.checkPassed(check) }
                    },
                    onClose = { finishAndRemoveTask() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_DEADLINE = "deadline"

        fun intent(context: Context, check: WakeCheck, deadline: Long): Intent =
            Intent(context, WakeCheckActivity::class.java).putCheck(check).putExtra(EXTRA_DEADLINE, deadline)
    }
}

private enum class Phase { WAITING, PASSED, MISSED }

@Composable
private fun WakeCheckScreen(check: WakeCheck, deadline: Long, onConfirm: () -> Unit, onClose: () -> Unit) {
    var phase by remember { mutableStateOf(Phase.WAITING) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(phase) {
        when (phase) {
            Phase.WAITING -> while (true) {
                now = System.currentTimeMillis()
                if (now >= deadline) {
                    phase = Phase.MISSED
                    break
                }
                delay(200)
            }
            Phase.PASSED -> {
                delay(2_500)
                onClose()
            }
            // The ringing screen opens over this one; close underneath it.
            Phase.MISSED -> {
                delay(2_500)
                onClose()
            }
        }
    }
    val leftMs = (deadline - now).coerceAtLeast(0)
    val fraction = leftMs.toFloat() / WakeCheck.WINDOW_MS

    SkyBackground(LocalSkyTheme.current.morning) {
        val sky = LocalSky.current
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 28.dp, end = 28.dp, top = 64.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "WAKE CHECK ${check.index} OF ${check.total}",
                style = ColdText.label.copy(letterSpacing = ColdText.label.letterSpacing * 1.1f),
                color = sky.mute,
            )
            when (phase) {
                Phase.WAITING -> {
                    Text("Still awake?", style = ColdText.display.copy(fontSize = 44.sp), color = sky.ink, modifier = Modifier.padding(top = 14.dp))
                    Text(
                        "Tap before the ring runs out, or the alarm starts again from round 1.",
                        style = ColdText.body,
                        color = sky.dim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .widthIn(max = 280.dp),
                    )
                    Box(Modifier.padding(top = 54.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(260.dp)) {
                            val stroke = 8.dp.toPx()
                            val inset = stroke / 2
                            val arc = size.copy(width = size.width - stroke, height = size.height - stroke)
                            drawArc(sky.ink.copy(alpha = 0.12f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                            drawArc(sky.ink, -90f, 360f * fraction, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                        UpButton(secondsLeft = ((leftMs + 999) / 1000).toInt()) {
                            if (System.currentTimeMillis() < deadline) {
                                phase = Phase.PASSED
                                onConfirm()
                            }
                        }
                    }
                }
                Phase.PASSED -> Passed(check)
                Phase.MISSED -> {
                    Text("Caught you.", style = ColdText.display.copy(fontSize = 44.sp), color = sky.ink, modifier = Modifier.padding(top = 140.dp))
                    Text(
                        "No tap in time, so the alarm is ringing again. Five rounds, from the top.",
                        style = ColdText.body,
                        color = sky.dim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .widthIn(max = 280.dp),
                    )
                }
            }
        }
    }
}

/** The big pulsing "I'm up" disc. */
@Composable
private fun UpButton(secondsLeft: Int, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val halo by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "halo")
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = 1f + 0.18f * halo
                    scaleY = 1f + 0.18f * halo
                    alpha = 1f - halo
                }
                .background(Color.White.copy(alpha = 0.6f), CircleShape),
        )
        Column(
            Modifier
                .springClick(0.9f, onClick = onClick)
                .size(200.dp)
                .shadow(24.dp, CircleShape, ambientColor = Color(0xFFA04628), spotColor = Color(0xFFA04628))
                .background(Brush.radialGradient(listOf(Color.White, Color(0xFFFFF1DE), Color(0xFFFFE2BF))), CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("I'm up", style = ColdText.header.copy(fontSize = 34.sp), color = Sun.OnAmber)
            Text("$secondsLeft s left", style = ColdText.bodyStrong.copy(fontSize = 14.sp), color = Sun.OnAmber.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun Passed(check: WakeCheck) {
    val sky = LocalSky.current
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, Motion.bouncy()) }
    Box(
        Modifier
            .padding(top = 120.dp)
            .size(128.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                rotationZ = (1f - pop.value) * -30f
            }
            .background(Color.White.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(SunIcons.Check, contentDescription = null, tint = Sun.OnAmber, modifier = Modifier.size(60.dp)) }
    Text("Nice. You're up.", style = ColdText.display.copy(fontSize = 40.sp), color = sky.ink, modifier = Modifier.padding(top = 28.dp))
    Text(
        if (check.isLast) "That's the last check. Have a good morning." else "Next check in 5 minutes.",
        style = ColdText.body,
        color = sky.dim,
        modifier = Modifier.padding(top = 8.dp),
    )
}
