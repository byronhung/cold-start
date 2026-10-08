package com.coldstart.app.ring

import android.os.Bundle
import android.text.format.DateFormat
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.activity.enableEdgeToEdge
import com.coldstart.app.ColdStartApp
import com.coldstart.app.ui.theme.ColdStartTheme

/**
 * The ringing screen. Shows over the lock screen and turns the screen on. Back does nothing and
 * the volume buttons are swallowed: the only ways out are solving the puzzles or the 30-second hold.
 *
 * It holds no ring state of its own. It reads [RingController], and closes itself if nothing is
 * ringing, so a stale notification tap can never open a ghost.
 */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this) { /* swallowed on purpose */ }

        val controller = (application as ColdStartApp).ringController
        val is24Hour = DateFormat.is24HourFormat(this)
        setContent {
            val skyTheme by (application as ColdStartApp).skyTheme.collectAsState()
            ColdStartTheme(skyTheme) {
                RingRoute(controller = controller, is24Hour = is24Hour, onClose = { finishAndRemoveTask() })
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
        if (keyCode in SWALLOWED_KEYS) true else super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
        if (keyCode in SWALLOWED_KEYS) true else super.onKeyUp(keyCode, event)

    private companion object {
        val SWALLOWED_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
        )
    }
}
