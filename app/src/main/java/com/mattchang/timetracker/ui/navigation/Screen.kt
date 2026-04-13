package com.mattchang.timetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.mattchang.timetracker.R

sealed class Screen(
    val route: String,
    val labelResId: Int,
    val icon: ImageVector
) {
    data object Records : Screen("records", R.string.tab_records, Icons.AutoMirrored.Filled.List)
    data object Timer : Screen("timer", R.string.tab_timer, Icons.Default.Timer)
    data object AddRecord : Screen("add_record", R.string.tab_add, Icons.Default.Add)
    data object Sleep : Screen("sleep", R.string.tab_sleep, Icons.Default.Bedtime)
    data object Analytics : Screen("analytics", R.string.tab_analytics, Icons.Default.BarChart)
    data object Settings : Screen("settings", R.string.settings, Icons.Default.Settings)
    data object ManageCategories : Screen("manage_categories", R.string.manage_categories, Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Records, Timer, AddRecord, Sleep, Analytics)
    }
}
