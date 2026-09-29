package com.coldstart.app.ring

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.coldstart.app.ColdStartApp
import com.coldstart.app.data.WakeOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns the ring from the moment the alarm fires until [RingController] goes back to Idle.
 * The ringing screen can close, crash or be swiped away: the sound lives here, not there.
 */
class RingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app get() = application as ColdStartApp
    private var ringer: Ringer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android requires this within seconds of startForegroundService, on every start.
        goForeground(Notifications.ringing(this, currentSession()))

        val controller = app.ringController
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId < 0) {
            if (!controller.isActive) shutDown()
            return START_NOT_STICKY
        }
        if (controller.isActive) {
            scope.launch { app.repository.skipWake(alarmId) }
            return START_REDELIVER_INTENT
        }

        controller.markStarting()
        acquireWakeLock()
        // Watch for the ring ending, however it ends.
        scope.launch {
            controller.state.first { it == RingState.Idle }
            shutDown()
        }
        scope.launch {
            val session = app.repository.beginWake(alarmId)
            if (session == null) {
                controller.abort()
                return@launch
            }
            controller.begin(session)
            goForeground(Notifications.ringing(this@RingService, session))
            ringer = Ringer(this@RingService).also { it.start(scope) }
            delay(RING_TIMEOUT_MS)
            controller.finish(WakeOutcome.TIMED_OUT, emptyList())
        }
        // If Android kills us mid-ring, it restarts the service with this same intent: it rings again.
        return START_REDELIVER_INTENT
    }

    private fun currentSession(): RingSession? =
        (app.ringController.state.value as? RingState.Ringing)?.session

    private fun goForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                // The type Android reserves for alarm apps holding the exact-alarm permission.
                startForeground(Notifications.ID_RINGING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
                return
            } catch (e: Exception) {
                Log.w(TAG, "systemExempted refused, falling back to mediaPlayback", e)
            }
        }
        startForeground(Notifications.ID_RINGING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    private fun acquireWakeLock() {
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "coldstart:ring")
            .apply { acquire(RING_TIMEOUT_MS + 60_000) }
    }

    private fun shutDown() {
        ringer?.stop()
        ringer = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        ringer?.stop()
        wakeLock?.takeIf { it.isHeld }?.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RingService"
        private const val EXTRA_ALARM_ID = "alarmId"

        /** An alarm nobody answers stops after an hour, logged as timed out. */
        const val RING_TIMEOUT_MS = 60 * 60 * 1000L

        fun startIntent(context: Context, alarmId: Long): Intent =
            Intent(context, RingService::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
    }
}
