package com.mattchang.timetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mattchang.timetracker.ui.addrecord.AddRecordScreen
import com.mattchang.timetracker.ui.analytics.AnalyticsScreen
import com.mattchang.timetracker.ui.records.RecordListScreen
import com.mattchang.timetracker.ui.sleep.SleepScreen
import com.mattchang.timetracker.ui.timer.TimerScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Records.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Records.route) {
                RecordListScreen(
                    onRecordClick = { recordId ->
                        navController.navigate("edit_record/$recordId")
                    }
                )
            }
            composable(Screen.Timer.route) {
                TimerScreen()
            }
            composable(Screen.AddRecord.route) {
                AddRecordScreen(
                    onSaved = {
                        navController.navigate(Screen.Records.route) {
                            popUpTo(Screen.Records.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "edit_record/{recordId}",
                arguments = listOf(navArgument("recordId") { type = NavType.LongType })
            ) {
                AddRecordScreen(
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Screen.Sleep.route) {
                SleepScreen()
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
        }
    }
}
