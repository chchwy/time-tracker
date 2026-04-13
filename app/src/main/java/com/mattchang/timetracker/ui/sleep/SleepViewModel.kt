package com.mattchang.timetracker.ui.sleep

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.RecordType
import com.mattchang.timetracker.domain.model.TimeRecord
import com.mattchang.timetracker.domain.repository.CategoryRepository
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class SleepFormState(
    val sleepTime: LocalDateTime = LocalDateTime.now().minusDays(1).withHour(23).withMinute(30).withSecond(0),
    val wakeTime: LocalDateTime = LocalDateTime.now().withHour(7).withMinute(0).withSecond(0),
    val childInterrupted: Boolean = false,
    val usedComputerBeforeBed: Boolean = false,
    val readBookBeforeBed: Boolean = false,
    val bookTitleBeforeBed: String = "",
    val chattedWithWife: Boolean = false,
    val stayUpLateReason: String = "",
    val morningEnergyIndex: Int = 7,
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val editingRecordId: Long = 0
) {
    val durationMinutes: Int
        get() {
            val dur = Duration.between(sleepTime, wakeTime)
            return if (dur.isNegative) 0 else dur.toMinutes().toInt()
        }
}

data class SleepInsights(
    val weeklyAvgHours: Float = 0f,
    val avgMorningEnergy: Float = 0f,
    val readingStreakDays: Int = 0,
    val hasData: Boolean = false
)

sealed class SleepEvent {
    data object SaveSuccess : SleepEvent()
}

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val timeRecordRepository: TimeRecordRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _form = MutableStateFlow(SleepFormState())
    val form: StateFlow<SleepFormState> = _form.asStateFlow()

    private val _events = MutableSharedFlow<SleepEvent>()
    val events = _events.asSharedFlow()

    val sleepRecords: StateFlow<List<TimeRecord>> = timeRecordRepository.getSleepRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insights: StateFlow<SleepInsights> = sleepRecords
        .map { records -> computeInsights(records) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SleepInsights())

    init {
        val recordId = savedStateHandle.get<Long>("recordId") ?: 0L
        if (recordId > 0) {
            loadRecord(recordId)
        }
    }

    private fun loadRecord(id: Long) {
        viewModelScope.launch {
            timeRecordRepository.getRecordById(id)?.let { record ->
                _form.update {
                    it.copy(
                        sleepTime = record.startTime,
                        wakeTime = record.endTime,
                        childInterrupted = record.childInterrupted ?: false,
                        usedComputerBeforeBed = record.usedComputerBeforeBed ?: false,
                        readBookBeforeBed = record.readBookBeforeBed ?: false,
                        bookTitleBeforeBed = record.bookTitleBeforeBed ?: "",
                        chattedWithWife = record.chattedWithWife ?: false,
                        stayUpLateReason = record.stayUpLateReason ?: "",
                        morningEnergyIndex = record.morningEnergyIndex ?: 7,
                        isEditing = true,
                        editingRecordId = record.id
                    )
                }
            }
        }
    }

    // ── Form Updaters ────────────────────────────────────────────────────

    fun updateSleepTime(dt: LocalDateTime) {
        _form.update { it.copy(sleepTime = dt, errorMessage = null) }
    }

    fun updateWakeTime(dt: LocalDateTime) {
        _form.update { it.copy(wakeTime = dt, errorMessage = null) }
    }

    fun toggleChildInterrupted() {
        _form.update { it.copy(childInterrupted = !it.childInterrupted) }
    }

    fun toggleUsedComputer() {
        _form.update { it.copy(usedComputerBeforeBed = !it.usedComputerBeforeBed) }
    }

    fun toggleReadBook() {
        _form.update { it.copy(readBookBeforeBed = !it.readBookBeforeBed) }
    }

    fun updateBookTitleBeforeBed(title: String) {
        _form.update { it.copy(bookTitleBeforeBed = title) }
    }

    fun toggleChattedWithWife() {
        _form.update { it.copy(chattedWithWife = !it.chattedWithWife) }
    }

    fun updateStayUpLateReason(reason: String) {
        _form.update { it.copy(stayUpLateReason = reason) }
    }

    fun updateMorningEnergy(value: Int) {
        _form.update { it.copy(morningEnergyIndex = value) }
    }

    // ── Save ─────────────────────────────────────────────────────────────

    fun save() {
        val state = _form.value

        if (!state.wakeTime.isAfter(state.sleepTime)) {
            _form.update { it.copy(errorMessage = "end_before_start") }
            return
        }

        viewModelScope.launch {
            // Fetch categories fresh from the repository to avoid race conditions
            // with WhileSubscribed StateFlow that may not have started collecting yet
            val allCategories = categoryRepository.getAllCategories().first()
            val sleepCategory = allCategories.find { it.icon == "bedtime" }

            val record = TimeRecord(
                id = if (state.isEditing) state.editingRecordId else 0,
                type = RecordType.SLEEP,
                categoryId = sleepCategory?.id,
                startTime = state.sleepTime,
                endTime = state.wakeTime,
                durationMinutes = state.durationMinutes,
                childInterrupted = state.childInterrupted,
                usedComputerBeforeBed = state.usedComputerBeforeBed,
                readBookBeforeBed = state.readBookBeforeBed,
                bookTitleBeforeBed = state.bookTitleBeforeBed.takeIf { state.readBookBeforeBed }?.ifBlank { null },
                chattedWithWife = state.chattedWithWife,
                stayUpLateReason = state.stayUpLateReason.ifBlank { null },
                morningEnergyIndex = state.morningEnergyIndex
            )

            if (state.isEditing) {
                timeRecordRepository.updateRecord(record, emptyList())
            } else {
                timeRecordRepository.insertRecord(record, emptyList())
            }

            // Reset to fresh form (keep default times for convenience)
            _form.value = SleepFormState()
            _events.emit(SleepEvent.SaveSuccess)
        }
    }

    fun delete() {
        val state = _form.value
        if (state.isEditing) {
            viewModelScope.launch {
                timeRecordRepository.getRecordById(state.editingRecordId)?.let { record ->
                    timeRecordRepository.deleteRecord(record)
                    _events.emit(SleepEvent.SaveSuccess)
                }
            }
        }
    }

    // ── Insights Computation ─────────────────────────────────────────────

    private fun computeInsights(records: List<TimeRecord>): SleepInsights {
        if (records.isEmpty()) return SleepInsights(hasData = false)

        // Weekly average (last 7 days)
        val sevenDaysAgo = LocalDateTime.now().minusDays(7)
        val recentRecords = records.filter { it.startTime.isAfter(sevenDaysAgo) }
        val weeklyAvgHours = if (recentRecords.isNotEmpty()) {
            recentRecords.map { it.durationMinutes }.average().toFloat() / 60f
        } else 0f

        // Average morning energy
        val energyRecords = records.filter { it.morningEnergyIndex != null }
        val avgEnergy = if (energyRecords.isNotEmpty()) {
            energyRecords.map { it.morningEnergyIndex!! }.average().toFloat()
        } else 0f

        // Reading streak: consecutive days (by wake date) with readBookBeforeBed=true
        val readingStreak = computeReadingStreak(records)

        return SleepInsights(
            weeklyAvgHours = weeklyAvgHours,
            avgMorningEnergy = avgEnergy,
            readingStreakDays = readingStreak,
            hasData = true
        )
    }

    private fun computeReadingStreak(records: List<TimeRecord>): Int {
        // Map each wake date -> did they read?
        val readingByDate: Map<LocalDate, Boolean> = records
            .groupBy { it.endTime.toLocalDate() }
            .mapValues { (_, recs) -> recs.any { it.readBookBeforeBed == true } }

        var streak = 0
        var date = LocalDate.now()
        while (true) {
            val didRead = readingByDate[date] ?: false
            if (!didRead) break
            streak++
            date = date.minusDays(1)
        }
        return streak
    }
}
