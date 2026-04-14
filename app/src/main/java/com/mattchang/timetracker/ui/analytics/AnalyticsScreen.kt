package com.mattchang.timetracker.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.mattchang.timetracker.ui.components.DonutChart
import com.mattchang.timetracker.ui.components.PieSlice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // ── Period type selector ──────────────────────────────────────────
        item {
            val options = listOf(
                PeriodType.DAY to stringResource(R.string.day),
                PeriodType.WEEK to stringResource(R.string.week),
                PeriodType.MONTH to stringResource(R.string.month)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = uiState.periodType == type,
                        onClick = { viewModel.setPeriodType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        label = { Text(label) }
                    )
                }
            }
        }

        // ── Date navigation ───────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::navigatePrevious) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Text(
                    text = uiState.periodLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::navigateNext) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }

        // ── Empty state ───────────────────────────────────────────────────
        if (uiState.totalTrackedMinutes == 0 && uiState.sleepAnalytics == null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = stringResource(R.string.no_data_for_period),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // ── Donut chart + legend ──────────────────────────────────────
            if (uiState.categorySummary.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.category_distribution)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val unknownLabel = stringResource(R.string.unknown_time)
                            val slices = uiState.categorySummary.map { cat ->
                                PieSlice(
                                    color = parseColor(cat.colorHex),
                                    fraction = cat.fraction,
                                    label = if (cat.isUnknown) unknownLabel else cat.name
                                )
                            }
                            DonutChart(
                                slices = slices,
                                modifier = Modifier.size(140.dp),
                                strokeWidth = 28.dp
                            )

                            Spacer(Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.categorySummary.forEach { item ->
                                    LegendRow(
                                        color = parseColor(item.colorHex),
                                        label = if (item.isUnknown) unknownLabel else item.name,
                                        minutes = item.totalMinutes,
                                        fraction = item.fraction
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Decision panel ────────────────────────────────────────────
            if (uiState.elapsedMinutes > 0) {
                item {
                    val rate = uiState.totalTrackedMinutes.toFloat() / uiState.elapsedMinutes
                    val pct = (rate * 100).toInt()
                    SectionCard(title = stringResource(R.string.decision_panel)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.recording_rate),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { rate.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(
                                    R.string.recording_rate_detail,
                                    formatMinutes(uiState.totalTrackedMinutes),
                                    formatMinutes(uiState.elapsedMinutes)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Summary cards ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        label = stringResource(R.string.total_tracked),
                        value = formatMinutes(uiState.totalTrackedMinutes),
                        icon = Icons.Default.BarChart,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = stringResource(R.string.daily_avg),
                        value = formatMinutes(uiState.dailyAvgMinutes.toInt()),
                        icon = Icons.Default.Coffee,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Sleep analytics ───────────────────────────────────────────
            uiState.sleepAnalytics?.let { sleep ->
                item {
                    SectionCard(title = stringResource(R.string.sleep_analysis)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Avg duration + energy
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MiniStatCard(
                                    icon = Icons.Default.Bedtime,
                                    label = stringResource(R.string.avg_sleep_duration),
                                    value = "%.1f h".format(sleep.avgDurationHours),
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniStatCard(
                                    icon = Icons.Default.ElectricBolt,
                                    label = stringResource(R.string.avg_morning_energy),
                                    value = if (sleep.avgMorningEnergy > 0f)
                                        "%.1f".format(sleep.avgMorningEnergy) else "–",
                                    iconTint = Color(0xFFFF9800),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            HorizontalDivider()

                            // Habit rates
                            HabitRateRow(
                                icon = Icons.Default.Book,
                                label = stringResource(R.string.read_book),
                                rate = sleep.readingRate
                            )
                            HabitRateRow(
                                icon = Icons.Default.Bedtime,
                                label = stringResource(R.string.chatted_with_wife),
                                rate = sleep.chattingRate
                            )
                        }
                    }
                }
            }

            // ── Reading log ───────────────────────────────────────────────
            if (uiState.booksRead.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.reading_log)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.booksRead.forEach { book ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Book,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = stringResource(R.string.times_read, book.readCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}


private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF9E9E9E)
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon, contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendRow(
    color: Color,
    label: String,
    minutes: Int,
    fraction: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = "${formatMinutes(minutes)} · ${(fraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MiniStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconTint)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun HabitRateRow(
    icon: ImageVector,
    label: String,
    rate: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp),
             tint = MaterialTheme.colorScheme.secondary)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        LinearProgressIndicator(
            progress = { rate },
            modifier = Modifier.width(80.dp),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "${(rate * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(32.dp)
        )
    }
}
