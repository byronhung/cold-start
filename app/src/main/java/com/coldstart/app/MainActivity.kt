package com.coldstart.app

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.coldstart.app.ui.edit.AlarmEditScreen
import com.coldstart.app.ui.edit.AlarmEditViewModel
import com.coldstart.app.ui.history.HistoryScreen
import com.coldstart.app.ui.history.HistoryViewModel
import com.coldstart.app.ui.list.AlarmListScreen
import com.coldstart.app.ui.list.AlarmListViewModel
import com.coldstart.app.ui.scan.ScanScreen
import com.coldstart.app.ui.settings.SettingsScreen
import com.coldstart.app.ui.settings.SettingsViewModel
import com.coldstart.app.ui.theme.ColdStartTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        // A permission granted from the setup card may have made scheduling possible.
        val app = application as ColdStartApp
        app.appScope.launch { app.repository.rescheduleAll() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as ColdStartApp).repository
        val is24Hour = DateFormat.is24HourFormat(this)

        setContent {
            ColdStartTheme {
                val nav = rememberNavController()
                NavHost(
                    navController = nav,
                    startDestination = "list",
                    // Screens slide a little and fade, instead of the stock hard cut.
                    enterTransition = { slideInHorizontally(tween(320)) { it / 8 } + fadeIn(tween(320)) },
                    exitTransition = { fadeOut(tween(180)) },
                    popEnterTransition = { fadeIn(tween(320)) },
                    popExitTransition = { slideOutHorizontally(tween(260)) { it / 8 } + fadeOut(tween(220)) },
                ) {
                    composable("list") {
                        val vm: AlarmListViewModel = viewModel(
                            factory = viewModelFactory { initializer { AlarmListViewModel(repository, is24Hour) } },
                        )
                        AlarmListScreen(
                            viewModel = vm,
                            onAdd = { nav.navigate("edit") },
                            onEdit = { id -> nav.navigate("edit?id=$id") },
                            onHistory = { nav.navigate("history") },
                            onSettings = { nav.navigate("settings") },
                        )
                    }
                    composable("history") {
                        val vm: HistoryViewModel = viewModel(
                            factory = viewModelFactory { initializer { HistoryViewModel(repository, is24Hour) } },
                        )
                        HistoryScreen(viewModel = vm, onBack = { nav.popBackStack() })
                    }
                    composable("settings") { entry ->
                        val vm: SettingsViewModel = viewModel(
                            factory = viewModelFactory { initializer { SettingsViewModel(repository) } },
                        )
                        val scanned by entry.savedStateHandle.getStateFlow<String?>(SCANNED_CODE, null).collectAsState()
                        LaunchedEffect(scanned) {
                            scanned?.let {
                                vm.setCode(it)
                                entry.savedStateHandle[SCANNED_CODE] = null
                            }
                        }
                        SettingsScreen(viewModel = vm, onScan = { nav.navigate("scan") }, onBack = { nav.popBackStack() })
                    }
                    composable("scan") {
                        ScanScreen(
                            // Hand the code back to Settings underneath, then return to it.
                            onCode = { code ->
                                nav.previousBackStackEntry?.savedStateHandle?.set(SCANNED_CODE, code)
                                if (nav.currentDestination?.route == "scan") nav.popBackStack()
                            },
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(
                        route = "edit?id={id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id")?.takeIf { it >= 0 }
                        val vm: AlarmEditViewModel = viewModel(
                            factory = viewModelFactory { initializer { AlarmEditViewModel(repository, id) } },
                        )
                        AlarmEditScreen(
                            viewModel = vm,
                            onOpenSettings = { nav.navigate("settings") },
                            // Only pop if we're still on the edit screen, so a double tap can't pop the list too.
                            onDone = {
                                if (nav.currentDestination?.route?.startsWith("edit") == true) nav.popBackStack()
                            },
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val SCANNED_CODE = "scannedCode"
    }
}
