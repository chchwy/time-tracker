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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattchang.timetracker.R
import com.mattchang.timetracker.domain.model.TimeRecord
import com.mattchang.timetracker.ui.analytics.PeriodType
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

    var periodType by remember { mutableStateOf(PeriodType.WEEK) }
    var periodOffset by remember { mutableIntStateOf(0) }

    val today = LocalDate.now()
    val periodStart = remember(periodType, periodOffset) {
        when (periodType) {
            PeriodType.DAY -> today.plusDays(periodOffset.toLong())
            PeriodType.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(periodOffset.toLong())
            PeriodType.MONTH -> today.withDayOfMonth(1).plusMonths(periodOffset.toLong())
        }
    }
    val periodEnd = remember(periodType, periodStart) {
        when (periodType) {
            PeriodType.DAY -> periodStart
            PeriodType.WEEK -> periodStart.plusDays(6)
            PeriodType.MONTH -> periodStart.plusMonths(1).minusDays(1)
        }
    }

    val periodRecords = remember(groupedRecords, periodStart, periodEnd) {
        groupedRecords.filter { (date, _) -> !date.isBefore(periodStart) && !date.isAfter(periodEnd) }
    }

    val periodLabel = remember(periodType, periodStart, periodEnd) {
        val fmt = DateTimeFormatter.ofPattern("MM/dd")
        when (periodType) {
            PeriodType.DAY -> periodStart.format(DateTimeFormatter.ofPattern("yyyy/MM/dd (E)"))
            PeriodType.WEEK -> "${periodStart.format(fmt)} – ${periodEnd.format(fmt)}"
            PeriodType.MONTH -> periodStart.format(DateTimeFormatter.ofPattern("yyyy/MM"))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Period type selector ──────────────────────────────────────
            val options = listOf(
                PeriodType.DAY to stringResource(R.string.day),
                PeriodType.WEEK to stringResource(R.string.week),
                PeriodType.MONTH to stringResource(R.string.month)
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                options.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = periodType == type,
                        onClick = { periodType = type; periodOffset = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        label = { Text(label) }
                    )
                }
            }

            // ── Date navigation ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { periodOffset-- }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                }
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = { periodOffset++ }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            }

            if (periodRecords.isEmpty()) {
                EmptyState()
            } else {
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd (E)")

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    periodRecords.forEach { (date, dayRecords) ->
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
