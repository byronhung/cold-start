package com.coldstart.app

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.coldstart.app.ui.list.AlarmListScreen
import com.coldstart.app.ui.list.AlarmListViewModel
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

        setContent {
            ColdStartTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "list") {
                    composable("list") {
                        val vm: AlarmListViewModel = viewModel(
                            factory = viewModelFactory { initializer { AlarmListViewModel(repository, DateFormat.is24HourFormat(this@MainActivity)) } },
                        )
                        AlarmListScreen(
                            viewModel = vm,
                            onAdd = { nav.navigate("edit") },
                            onEdit = { id -> nav.navigate("edit?id=$id") },
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
}
