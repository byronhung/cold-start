package com.byronhung.firstlight.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.byronhung.firstlight.puzzle.BarcodeScanner
import com.byronhung.firstlight.ui.components.AmberButton
import com.byronhung.firstlight.ui.components.IconSquareButton
import com.byronhung.firstlight.ui.components.QuietButton
import com.byronhung.firstlight.ui.components.SkyBackground
import com.byronhung.firstlight.ui.components.SunIcons
import com.byronhung.firstlight.ui.theme.AppShapes
import com.byronhung.firstlight.ui.theme.AppText
import com.byronhung.firstlight.ui.theme.LocalSky
import com.byronhung.firstlight.ui.theme.LocalSkyTheme
import com.byronhung.firstlight.ui.theme.Space
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.BarcodeScanning

/** Registering the wake-up code. Asks for the camera here, at setup, not at 6am. */
@Composable
fun ScanScreen(onCode: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) {
        if (!granted) ask.launch(Manifest.permission.CAMERA)
        // The barcode reader's model comes from Play services. Fetch it now, while there's a
        // connection, so the scan round works at 6am in airplane mode.
        runCatching {
            ModuleInstall.getClient(context).installModules(
                ModuleInstallRequest.newBuilder().addApi(BarcodeScanning.getClient()).build(),
            )
        }
    }
    var torch by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    SkyBackground(LocalSkyTheme.current.night, calm = true) {
        val sky = LocalSky.current
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Space.screen, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                Modifier.padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconSquareButton(SunIcons.Back, "Back", onBack)
                Text("Register your code", style = AppText.title, color = sky.ink)
            }
            Text(
                "Point the camera at any barcode or QR code that lives away from your bed: " +
                    "toothpaste, a cereal box, the kettle. Nothing to print.",
                style = AppText.body,
                color = sky.dim,
                modifier = Modifier.padding(horizontal = 4.dp),
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
                        .clip(AppShapes.card),
                )
                QuietButton(if (torch) "Torch off" else "Torch on", { torch = !torch }, Modifier.align(Alignment.CenterHorizontally))
            } else {
                Text("First Light needs the camera to read the code.", style = AppText.body, color = sky.mute)
                AmberButton("Allow camera", { ask.launch(Manifest.permission.CAMERA) })
            }
        }
    }
}
