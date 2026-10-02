package com.coldstart.app.ui.list

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.coldstart.app.ui.components.AmberButton
import com.coldstart.app.ui.components.GlassCard
import com.coldstart.app.ui.components.SectionLabel
import com.coldstart.app.ui.theme.LocalSky
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space

/** What the phone must allow before an alarm can reliably ring. Re-read every time the app resumes. */
data class SetupChecks(
    val notifications: Boolean,
    val fullScreen: Boolean,
    val exactAlarms: Boolean,
    val battery: Boolean,
) {
    val allGood get() = notifications && fullScreen && exactAlarms && battery

    companion object {
        fun read(context: Context): SetupChecks {
            val nm = context.getSystemService(NotificationManager::class.java)
            val am = context.getSystemService(AlarmManager::class.java)
            val pm = context.getSystemService(PowerManager::class.java)
            return SetupChecks(
                notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
                fullScreen = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent(),
                exactAlarms = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms(),
                battery = pm.isIgnoringBatteryOptimizations(context.packageName),
            )
        }
    }
}

/**
 * The "phone makers' battery killers" answer from the brief: not clever code, a checklist that
 * walks you to the right settings screen and disappears once everything is allowed.
 */
@Composable
fun SetupCard() {
    val context = LocalContext.current
    var checks by remember { mutableStateOf(SetupChecks.read(context)) }
    LifecycleResumeEffect(Unit) {
        checks = SetupChecks.read(context)
        onPauseOrDispose { }
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        checks = SetupChecks.read(context)
    }
    if (checks.allGood) return

    val pkg = Uri.parse("package:${context.packageName}")
    val sky = LocalSky.current
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Space.lg + 2.dp), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
            SectionLabel("Before alarms can ring", color = sky.sunInk)
            if (!checks.notifications) {
                SetupRow("Allow notifications", "The ringing screen opens through one.") {
                    if (Build.VERSION.SDK_INT >= 33) {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }
                }
            }
            if (!checks.fullScreen) {
                SetupRow("Allow full-screen alarms", "Lets the alarm take over the lock screen.") {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
                }
            }
            if (!checks.exactAlarms) {
                SetupRow("Allow alarms", "So it rings on the minute, not roughly.") {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
                }
            }
            if (!checks.battery) {
                SetupRow("Remove battery limits", "Stops the phone putting Cold Start to sleep overnight.") {
                    @Suppress("BatteryLife")
                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))
                }
            }
        }
    }
}

@Composable
private fun SetupRow(title: String, why: String, onAllow: () -> Unit) {
    val sky = LocalSky.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = ColdText.bodyStrong, color = sky.ink)
            Text(why, style = ColdText.caption, color = sky.dim)
        }
        AmberButton("Allow", onAllow, height = 44.dp)
    }
}
