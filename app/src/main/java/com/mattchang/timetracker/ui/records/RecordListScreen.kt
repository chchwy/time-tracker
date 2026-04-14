package com.mattchang.timetracker.ui.records

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattchang.timetracker.R
import com.mattchang.timetracker.domain.model.TimeRecord
import com.mattchang.timetracker.ui.components.TimeRecordCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListScreen(
    onRecordClick: (Long) -> Unit = {},
    onSleepRecordClick: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: RecordListViewModel = hiltViewModel()
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteText = stringResource(R.string.record_deleted)
    val undoText = stringResource(R.string.undo)

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val groupedRecords = remember(records) {
        records.groupBy { it.endTime.toLocalDate() }
            .toSortedMap(compareByDescending { it })
    }

    var weekOffset by remember { mutableIntStateOf(0) }

    val today = remember { LocalDate.now() }
    val weekStart = remember(weekOffset) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOffset.toLong())
    }
    val weekEnd = remember(weekStart) { weekStart.plusDays(6) }

    val weekRecords = remember(groupedRecords, weekStart, weekEnd) {
        groupedRecords.filter { (date, _) -> !date.isBefore(weekStart) && !date.isAfter(weekEnd) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            WeekNavigationHeader(
                weekStart = weekStart,
                weekEnd = weekEnd,
                onPreviousWeek = { weekOffset-- },
                onNextWeek = { weekOffset++ },
                onNavigateToSettings = onNavigateToSettings
            )

            if (weekRecords.isEmpty()) {
                EmptyState()
            } else {
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd (E)")

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekRecords.forEach { (date, dayRecords) ->
                        item(key = "header_$date") {
                            DateHeader(date = date, formatter = dateFormatter)
                        }

                        items(dayRecords, key = { it.id }) { record ->
                            TimeRecordCard(
                                record = record,
                                category = record.categoryId?.let { categoryMap[it] },
                                onClick = {
                                    if (record.isSleep) onSleepRecordClick(record.id)
                                    else onRecordClick(record.id)
                                }
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun WeekNavigationHeader(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("MM/dd")
    val label = "${weekStart.format(fmt)} - ${weekEnd.format(fmt)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous week"
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium
        )
        Row {
            IconButton(onClick = onNextWeek) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next week"
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate, formatter: DateTimeFormatter) {
    Text(
        text = date.format(formatter),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}


@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.empty_records),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
