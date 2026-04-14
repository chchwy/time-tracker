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
    val fraction: Float,
    val isUnknown: Boolean = false
)

data class SleepAnalyticsUi(
    val avgDurationHours: Float,
    val avgMorningEnergy: Float,
    val readingRate: Float,
    val noComputerRate: Float,
    val chattingRate: Float
)


data class TagSummaryUi(
    val name: String,
    val totalMinutes: Int
)

data class BookReadEntryUi(
    val title: String,
    val readCount: Int,
    val lastReadDate: java.time.LocalDate
)

data class AnalyticsUiState(
    val periodType: PeriodType = PeriodType.WEEK,
    val periodLabel: String = "",
    val categorySummary: List<CategorySummaryUi> = emptyList(),
    val tagSummary: List<TagSummaryUi> = emptyList(),
    val totalTrackedMinutes: Int = 0,
    val elapsedMinutes: Int = 0,
    val dailyAvgMinutes: Float = 0f,
    val sleepAnalytics: SleepAnalyticsUi? = null,
    val booksRead: List<BookReadEntryUi> = emptyList()
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

        // ── Totals ────────────────────────────────────────────────────────
        val totalTracked = records.sumOf { it.durationMinutes }
        val dayCount = when (type) {
            PeriodType.DAY -> 1
            PeriodType.WEEK -> 7
            PeriodType.MONTH -> from.month.length(from.isLeapYear)
        }
        // Only count time that has already elapsed — past days get 1440 min,
        // today gets minutes since midnight, future days contribute 0.
        val today = LocalDate.now()
        val nowMinuteOfDay = java.time.LocalTime.now().hour * 60 + java.time.LocalTime.now().minute
        val elapsedMinutes = run {
            var total = 0
            var day = from
            while (day.isBefore(to)) {
                total += when {
                    day.isBefore(today) -> 24 * 60
                    day.isEqual(today) -> nowMinuteOfDay
                    else -> 0
                }
                day = day.plusDays(1)
            }
            total
        }
        val fromMs = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toMs = to.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // ── Category summary ──────────────────────────────────────────────
        // Start with minutes from records that fall within the period
        val minutesByCatId = mutableMapOf<Long?, Int>()
        records
            .filter { it.categoryId != null && it.durationMinutes > 0 }
            .forEach { rec -> minutesByCatId[rec.categoryId] = (minutesByCatId[rec.categoryId] ?: 0) + rec.durationMinutes }

        // Add overlap from sleep that started before the period but ends within it
        // (e.g. sleep started 23:40 yesterday, woke up 07:00 today)
        var overnightMinutes = 0
        sleepRecs.filter { rec ->
            val startMs = rec.startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = rec.endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            startMs < fromMs && endMs > fromMs
        }.forEach { rec ->
            val endMs = rec.endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val overlapMins = ((minOf(endMs, toMs) - fromMs) / 60000L).toInt().coerceAtLeast(0)
            if (overlapMins > 0) {
                minutesByCatId[rec.categoryId] = (minutesByCatId[rec.categoryId] ?: 0) + overlapMins
                overnightMinutes += overlapMins
            }
        }

        val categorySummary = minutesByCatId.map { (catId, minutes) ->
            val cat = catMap[catId]
            CategorySummaryUi(
                name = cat?.name ?: "Other",
                colorHex = cat?.colorHex ?: "#9E9E9E",
                totalMinutes = minutes,
                fraction = if (elapsedMinutes > 0) minutes.toFloat() / elapsedMinutes else 0f
            )
        }.sortedByDescending { it.totalMinutes }.toMutableList()

        val unknownMinutes = (elapsedMinutes - totalTracked - overnightMinutes).coerceAtLeast(0)
        if (unknownMinutes > 0) {
            categorySummary.add(
                CategorySummaryUi(
                    name = "",
                    colorHex = "#BDBDBD",
                    totalMinutes = unknownMinutes,
                    fraction = unknownMinutes.toFloat() / elapsedMinutes,
                    isUnknown = true
                )
            )
        }

        // ── Tag summary ───────────────────────────────────────────────────
        val tagMinutes = mutableMapOf<String, Int>()
        records.filter { it.durationMinutes > 0 && !it.isSleep }.forEach { rec ->
            rec.tags.forEach { tag ->
                tagMinutes[tag.name] = (tagMinutes[tag.name] ?: 0) + rec.durationMinutes
            }
        }
        val tagSummary = tagMinutes.map { (name, minutes) ->
            TagSummaryUi(name = name, totalMinutes = minutes)
        }.sortedByDescending { it.totalMinutes }

        // ── Sleep analytics (period-scoped) ───────────────────────────────
        val periodSleep = sleepRecs.filter { rec ->
            val ms = rec.startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ms in fromMs until toMs
        }

        val booksRead = periodSleep
            .filter { !it.bookTitleBeforeBed.isNullOrBlank() }
            .groupBy { it.bookTitleBeforeBed!! }
            .map { (title, recs) ->
                BookReadEntryUi(
                    title = title,
                    readCount = recs.size,
                    lastReadDate = recs.maxOf { it.endTime.toLocalDate() }
                )
            }
            .sortedByDescending { it.lastReadDate }

        val effectiveTracked = totalTracked + overnightMinutes
        val dailyAvg = if (dayCount > 0) effectiveTracked.toFloat() / dayCount else 0f

        AnalyticsUiState(
            periodType = type,
            periodLabel = buildPeriodLabel(type, from, to),
            categorySummary = categorySummary,
            tagSummary = tagSummary,
            totalTrackedMinutes = effectiveTracked,
            elapsedMinutes = elapsedMinutes,
            dailyAvgMinutes = dailyAvg,
            sleepAnalytics = computeSleepAnalytics(periodSleep),
            booksRead = booksRead
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

    private fun previousDateRange(type: PeriodType, currentFrom: LocalDate): Pair<LocalDate, LocalDate> =
        when (type) {
            PeriodType.DAY -> Pair(currentFrom.minusDays(1), currentFrom)
            PeriodType.WEEK -> Pair(currentFrom.minusWeeks(1), currentFrom)
            PeriodType.MONTH -> Pair(currentFrom.minusMonths(1).withDayOfMonth(1), currentFrom.withDayOfMonth(1))
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
