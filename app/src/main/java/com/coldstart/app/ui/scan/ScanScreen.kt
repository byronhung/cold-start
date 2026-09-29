package com.coldstart.app.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.coldstart.app.puzzle.BarcodeScanner
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space

/** Registering the code an alarm will ask you to scan. Asks for the camera here, at setup, not at 6am. */
@Composable
fun ScanScreen(onCode: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }
    var torch by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColdColors.Ground)
            .safeDrawingPadding()
            .padding(horizontal = Space.screen, vertical = Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ColdColors.Ink)
            }
            Text("Register a code", style = ColdText.title, color = ColdColors.Ink)
        }
        Text(
            "Point the camera at any barcode or QR code that lives away from your bed: " +
                "a kettle, a shampoo bottle, a printed sticker on the bathroom mirror.",
            style = ColdText.body,
            color = ColdColors.InkDim,
        )
        if (granted) {
            BarcodeScanner(
                onCode = { code ->
                    if (!done) {
                        done = true
                        onCode(code)
                    }
                },
                torchOn = torch,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(ColdShapes.medium),
            )
            TextButton(onClick = { torch = !torch }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (torch) "Torch off" else "Torch on", style = ColdText.body, color = ColdColors.Ink)
            }
        } else {
            Text("Cold Start needs the camera to read the code.", style = ColdText.body, color = ColdColors.InkMute)
            Button(
                onClick = { ask.launch(Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(containerColor = ColdColors.Accent, contentColor = ColdColors.Ground),
                shape = ColdShapes.small,
            ) { Text("Allow camera", style = ColdText.prompt) }
        }
    }
}
