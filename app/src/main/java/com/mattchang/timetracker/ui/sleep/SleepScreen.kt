package com.mattchang.timetracker.ui.sleep

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattchang.timetracker.R
import com.mattchang.timetracker.domain.model.TimeRecord
import com.mattchang.timetracker.ui.components.DateTimeField
import com.mattchang.timetracker.ui.components.DurationText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    onSaved: (() -> Unit)? = null,
    viewModel: SleepViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val sleepRecords by viewModel.sleepRecords.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    val savedMsg = stringResource(R.string.sleep_saved)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SleepEvent.SaveSuccess -> {
                    if (onSaved != null) {
                        // In edit mode: navigate back after saving
                        onSaved()
                    } else {
                        selectedTab = 1   // jump to history after save
                        snackbarHostState.showSnackbar(savedMsg)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Tab row ───────────────────────────────────────────────────
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.sleep_log_tab)) },
                    icon = { Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.sleep_history_tab)) },
                    icon = { Icon(Icons.Default.NightlightRound, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // ── Tab content ───────────────────────────────────────────────
            when (selectedTab) {
                0 -> SleepFormTab(
                    form = form,
                    onSleepTimeChanged = viewModel::updateSleepTime,
                    onWakeTimeChanged = viewModel::updateWakeTime,
                    onToggleChildInterrupted = viewModel::toggleChildInterrupted,
                    onToggleUsedComputer = viewModel::toggleUsedComputer,
                    onToggleReadBook = viewModel::toggleReadBook,
                    onBookTitleBeforeBedChanged = viewModel::updateBookTitleBeforeBed,
                    onToggleChattedWithWife = viewModel::toggleChattedWithWife,
                    onStayUpLateReasonChanged = viewModel::updateStayUpLateReason,
                    onMorningEnergyChanged = viewModel::updateMorningEnergy,
                    onSave = viewModel::save,
                    onDelete = viewModel::delete
                )
                1 -> SleepHistoryTab(
                    insights = insights,
                    records = sleepRecords
                )
            }
        }
    }
}

// ─── Form Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun SleepFormTab(
    form: SleepFormState,
    onSleepTimeChanged: (LocalDateTime) -> Unit,
    onWakeTimeChanged: (LocalDateTime) -> Unit,
    onToggleChildInterrupted: () -> Unit,
    onToggleUsedComputer: () -> Unit,
    onToggleReadBook: () -> Unit,
    onBookTitleBeforeBedChanged: (String) -> Unit,
    onToggleChattedWithWife: () -> Unit,
    onStayUpLateReasonChanged: (String) -> Unit,
    onMorningEnergyChanged: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // ── Time pickers ──────────────────────────────────────────────────
        item {
            DateTimeField(
                label = stringResource(R.string.sleep_time),
                dateTime = form.sleepTime,
                onDateTimeChanged = onSleepTimeChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            DateTimeField(
                label = stringResource(R.string.wake_time),
                dateTime = form.wakeTime,
                onDateTimeChanged = onWakeTimeChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Duration display ──────────────────────────────────────────────
        item {
            if (form.durationMinutes > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.sleep_duration_label) + " ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DurationText(
                        minutes = form.durationMinutes,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            if (form.errorMessage == "end_before_start") {
                Text(
                    text = stringResource(R.string.end_before_start_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Morning energy slider ─────────────────────────────────────────
        item {
            Column {
                Text(
                    text = stringResource(R.string.morning_energy_label, form.morningEnergyIndex),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("1", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = form.morningEnergyIndex.toFloat(),
                        onValueChange = { onMorningEnergyChanged(it.roundToInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = energyColor(form.morningEnergyIndex),
                            activeTrackColor = energyColor(form.morningEnergyIndex)
                        )
                    )
                    Text("10", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Habit toggles ─────────────────────────────────────────────────
        item {
            Text(
                text = stringResource(R.string.habit_toggles),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            HabitToggleRow(
                label = stringResource(R.string.read_book),
                checked = form.readBookBeforeBed,
                onCheckedChange = { onToggleReadBook() }
            )
        }
        if (form.readBookBeforeBed) {
            item {
                OutlinedTextField(
                    value = form.bookTitleBeforeBed,
                    onValueChange = onBookTitleBeforeBedChanged,
                    label = { Text(stringResource(R.string.book_title_before_bed)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        item {
            HabitToggleRow(
                label = stringResource(R.string.chatted_with_wife),
                checked = form.chattedWithWife,
                onCheckedChange = { onToggleChattedWithWife() }
            )
        }
        item {
            HabitToggleRow(
                label = stringResource(R.string.used_computer),
                checked = form.usedComputerBeforeBed,
                onCheckedChange = { onToggleUsedComputer() }
            )
        }
        item {
            HabitToggleRow(
                label = stringResource(R.string.child_interrupted),
                checked = form.childInterrupted,
                onCheckedChange = { onToggleChildInterrupted() }
            )
        }

        // ── Stay up late reason ───────────────────────────────────────────
        item {
            OutlinedTextField(
                value = form.stayUpLateReason,
                onValueChange = onStayUpLateReasonChanged,
                label = { Text(stringResource(R.string.stay_up_late_reason)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        // ── Save button ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (form.isEditing) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── History Tab ──────────────────────────────────────────────────────────────

@Composable
private fun SleepHistoryTab(
    insights: SleepInsights,
    records: List<TimeRecord>
) {
    if (records.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Bedtime,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_sleep_records),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Quick Insights cards ──────────────────────────────────────────
        if (insights.hasData) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Text(
                    text = stringResource(R.string.insights_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightCard(
                        icon = Icons.Default.Bedtime,
                        label = stringResource(R.string.weekly_avg_sleep),
                        value = stringResource(R.string.hours_format, insights.weeklyAvgHours),
                        modifier = Modifier.weight(1f)
                    )
                    InsightCard(
                        icon = Icons.Default.ElectricBolt,
                        label = stringResource(R.string.avg_morning_energy),
                        value = if (insights.avgMorningEnergy > 0f)
                            "%.1f".format(insights.avgMorningEnergy) else "–",
                        modifier = Modifier.weight(1f)
                    )
                    InsightCard(
                        icon = Icons.Default.Book,
                        label = stringResource(R.string.reading_streak),
                        value = stringResource(R.string.days_format, insights.readingStreakDays),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
        }

        // ── Sleep record history ──────────────────────────────────────────
        item {
            Text(
                text = stringResource(R.string.sleep_history_tab),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(records, key = { it.id }) { record ->
            SleepHistoryCard(record = record)
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── Small composables ────────────────────────────────────────────────────────

@Composable
private fun HabitToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(250),
        label = "habitBg"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun InsightCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon, contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun SleepHistoryCard(record: TimeRecord) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date + duration row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = record.endTime.format(dateFormatter),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                DurationText(
                    minutes = record.durationMinutes,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Time range
            Text(
                text = "${record.startTime.format(timeFormatter)} → ${record.endTime.format(timeFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Habit icons row
            val habits = buildList {
                if (record.readBookBeforeBed == true) add("📖")
                if (record.chattedWithWife == true) add("💬")
                if (record.usedComputerBeforeBed == true) add("💻")
                if (record.childInterrupted == true) add("👶")
            }
            if (habits.isNotEmpty() || record.morningEnergyIndex != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    habits.forEach { emoji ->
                        Text(emoji, fontSize = 16.sp)
                    }
                    record.morningEnergyIndex?.let { energy ->
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = energyColor(energy)
                        )
                        Text(
                            text = "$energy/10",
                            style = MaterialTheme.typography.labelMedium,
                            color = energyColor(energy)
                        )
                    }
                }
            }

            // Note
            if (!record.stayUpLateReason.isNullOrBlank()) {
                Text(
                    text = "📝 ${record.stayUpLateReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!record.bookTitleBeforeBed.isNullOrBlank()) {
                Text(
                    text = "📚 ${record.bookTitleBeforeBed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Energy index → color
@Composable
private fun energyColor(energy: Int): Color {
    return when {
        energy >= 8 -> Color(0xFF4CAF50)   // green
        energy >= 5 -> Color(0xFFFF9800)   // orange
        else        -> Color(0xFFF44336)   // red
    }
}
