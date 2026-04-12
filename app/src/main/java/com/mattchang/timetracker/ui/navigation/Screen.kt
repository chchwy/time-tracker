package com.mattchang.timetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
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

    companion object {
        val bottomNavItems = listOf(Records, Timer, AddRecord, Sleep, Analytics)
    }
}
