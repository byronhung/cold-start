package com.coldstart.app.puzzle

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.coldstart.app.ui.theme.Sun
import androidx.compose.ui.unit.dp
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText

/**
 * The last round, when an alarm has a registered code: get out of bed, walk to it, scan it.
 * A different code counts as a miss (at most once every two seconds, since the camera sees it
 * many times a second).
 */
@Composable
fun QrPuzzle(expected: String, onMiss: () -> Unit, onSolved: () -> Unit) {
    val context = LocalContext.current
    val hasCamera = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    var torch by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var misses by remember { mutableIntStateOf(0) }
    var lastMissAt by remember { mutableLongStateOf(0L) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PuzzlePrompt("Walk to your wake-up code and scan it")
        if (!hasCamera) {
            Text(
                "Cold Start isn't allowed to use the camera, so this round can't run. Hold to give up, " +
                    "then allow the camera when you register your code in Settings.",
                style = ColdText.caption,
                color = Sun.OnGlass.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            return@Column
        }
        BarcodeScanner(
            onCode = { code ->
                if (finished) return@BarcodeScanner
                if (code == expected) {
                    finished = true
                    onSolved()
                } else if (SystemClock.elapsedRealtime() - lastMissAt > 2_000) {
                    lastMissAt = SystemClock.elapsedRealtime()
                    misses++
                    onMiss()
                }
            },
            torchOn = torch,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(ColdShapes.button),
        )
        TextButton(onClick = { torch = !torch }) {
            Text(if (torch) "Torch off" else "Torch on", style = ColdText.body, color = Sun.OnGlass)
        }
        MissNote(misses, text = "That's a different code.")
    }
}
