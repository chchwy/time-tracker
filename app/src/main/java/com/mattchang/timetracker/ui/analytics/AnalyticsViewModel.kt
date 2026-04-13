package com.mattchang.timetracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.TimeRecord
import com.mattchang.timetracker.domain.repository.CategoryRepository
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── Types ───────────────────────────────────────────────────────────────────

enum class PeriodType { DAY, WEEK, MONTH }

data class CategorySummaryUi(
    val name: String,
    val colorHex: String,
    val totalMinutes: Int,
    val fraction: Float
)

data class SleepAnalyticsUi(
    val avgDurationHours: Float,
    val avgMorningEnergy: Float,
    val readingRate: Float,
    val noComputerRate: Float,
    val chattingRate: Float
)

data class AnalyticsUiState(
    val periodType: PeriodType = PeriodType.WEEK,
    val periodLabel: String = "",
    val categorySummary: List<CategorySummaryUi> = emptyList(),
    val totalTrackedMinutes: Int = 0,
    val dailyAvgMinutes: Float = 0f,
    val sleepAnalytics: SleepAnalyticsUi? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel @Inject constructor(
    private val timeRecordRepository: TimeRecordRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _periodType = MutableStateFlow(PeriodType.WEEK)
    val periodType: StateFlow<PeriodType> = _periodType.asStateFlow()

    private val _periodStart = MutableStateFlow(
        LocalDate.now().with(DayOfWeek.MONDAY)
    )

    /** Derived (from, to) date pair — exclusive `to` */
    private val periodBounds: StateFlow<Pair<LocalDate, LocalDate>> =
        combine(_periodType, _periodStart) { type, start ->
            computeDateRange(type, start)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            computeDateRange(PeriodType.WEEK, LocalDate.now().with(DayOfWeek.MONDAY))
        )

    private val periodRecords: StateFlow<List<TimeRecord>> = periodBounds
        .flatMapLatest { (from, to) ->
            timeRecordRepository.getRecordsBetween(
                from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                to.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sleepRecords: StateFlow<List<TimeRecord>> = timeRecordRepository.getSleepRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<AnalyticsUiState> = combine(
        periodBounds,
        periodRecords,
        categories,
        sleepRecords
    ) { bounds, records, cats, sleepRecs ->
        val type = _periodType.value
        val (from, to) = bounds
        val catMap = cats.associateBy { it.id }

        // ── Category summary ──────────────────────────────────────────────
        val grouped = records
            .filter { it.categoryId != null && it.durationMinutes > 0 }
            .groupBy { it.categoryId }
        val totalMins = grouped.values.sumOf { recs -> recs.sumOf { it.durationMinutes } }
        val categorySummary = grouped.map { (catId, recs) ->
            val cat = catMap[catId]
            val minutes = recs.sumOf { it.durationMinutes }
            CategorySummaryUi(
                name = cat?.name ?: "Other",
                colorHex = cat?.colorHex ?: "#9E9E9E",
                totalMinutes = minutes,
                fraction = if (totalMins > 0) minutes.toFloat() / totalMins else 0f
            )
        }.sortedByDescending { it.totalMinutes }

        // ── Totals ────────────────────────────────────────────────────────
        val totalTracked = records.sumOf { it.durationMinutes }
        val dayCount = when (type) {
            PeriodType.DAY -> 1
            PeriodType.WEEK -> 7
            PeriodType.MONTH -> from.month.length(from.isLeapYear)
        }
        val dailyAvg = if (dayCount > 0) totalTracked.toFloat() / dayCount else 0f

        // ── Sleep analytics (period-scoped) ───────────────────────────────
        val fromMs = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toMs = to.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val periodSleep = sleepRecs.filter { rec ->
            val ms = rec.startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ms in fromMs until toMs
        }

        AnalyticsUiState(
            periodType = type,
            periodLabel = buildPeriodLabel(type, from, to),
            categorySummary = categorySummary,
            totalTrackedMinutes = totalTracked,
            dailyAvgMinutes = dailyAvg,
            sleepAnalytics = computeSleepAnalytics(periodSleep)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    // ── Actions ───────────────────────────────────────────────────────────

    fun setPeriodType(type: PeriodType) {
        _periodType.value = type
        _periodStart.value = defaultStart(type)
    }

    fun navigatePrevious() {
        _periodStart.update { current ->
            when (_periodType.value) {
                PeriodType.DAY -> current.minusDays(1)
                PeriodType.WEEK -> current.minusWeeks(1)
                PeriodType.MONTH -> current.minusMonths(1).withDayOfMonth(1)
            }
        }
    }

    fun navigateNext() {
        _periodStart.update { current ->
            when (_periodType.value) {
                PeriodType.DAY -> current.plusDays(1)
                PeriodType.WEEK -> current.plusWeeks(1)
                PeriodType.MONTH -> current.plusMonths(1).withDayOfMonth(1)
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun defaultStart(type: PeriodType): LocalDate = when (type) {
        PeriodType.DAY -> LocalDate.now()
        PeriodType.WEEK -> LocalDate.now().with(DayOfWeek.MONDAY)
        PeriodType.MONTH -> LocalDate.now().withDayOfMonth(1)
    }

    private fun computeDateRange(type: PeriodType, start: LocalDate): Pair<LocalDate, LocalDate> =
        when (type) {
            PeriodType.DAY -> Pair(start, start.plusDays(1))
            PeriodType.WEEK -> {
                val mon = start.with(DayOfWeek.MONDAY)
                Pair(mon, mon.plusWeeks(1))
            }
            PeriodType.MONTH -> {
                val first = start.withDayOfMonth(1)
                Pair(first, first.plusMonths(1))
            }
        }

    private fun buildPeriodLabel(type: PeriodType, from: LocalDate, to: LocalDate): String {
        val mdf = DateTimeFormatter.ofPattern("MM/dd")
        return when (type) {
            PeriodType.DAY -> from.format(DateTimeFormatter.ofPattern("yyyy/MM/dd (E)"))
            PeriodType.WEEK -> "${from.format(mdf)} – ${to.minusDays(1).format(mdf)}"
            PeriodType.MONTH -> from.format(DateTimeFormatter.ofPattern("yyyy/MM"))
        }
    }

    private fun computeSleepAnalytics(records: List<TimeRecord>): SleepAnalyticsUi? {
        if (records.isEmpty()) return null
        val n = records.size.toFloat()
        val avgDuration = records.map { it.durationMinutes }.average().toFloat() / 60f
        val energyRecs = records.filter { it.morningEnergyIndex != null }
        val avgEnergy = if (energyRecs.isNotEmpty())
            energyRecs.map { it.morningEnergyIndex!! }.average().toFloat() else 0f
        return SleepAnalyticsUi(
            avgDurationHours = avgDuration,
            avgMorningEnergy = avgEnergy,
            readingRate = records.count { it.readBookBeforeBed == true } / n,
            noComputerRate = records.count { it.usedComputerBeforeBed != true } / n,
            chattingRate = records.count { it.chattedWithWife == true } / n
        )
    }
}
