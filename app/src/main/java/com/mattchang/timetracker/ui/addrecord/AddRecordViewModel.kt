package com.mattchang.timetracker.ui.addrecord

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.RecordType
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.model.TimeRecord
import com.mattchang.timetracker.domain.repository.CategoryRepository
import com.mattchang.timetracker.domain.repository.TagRepository
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

data class AddRecordUiState(
    val title: String = "",
    val selectedCategoryId: Long? = null,
    val selectedTagIds: Set<Long> = emptySet(),
    val startTime: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0),
    val endTime: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0),
    val note: String = "",
    val isEditing: Boolean = false,
    val editingRecordId: Long = 0,
    val errorMessage: String? = null
) {
    val durationMinutes: Int
        get() {
            val dur = Duration.between(startTime, endTime)
            return if (dur.isNegative) 0 else dur.toMinutes().toInt()
        }
}

sealed class AddRecordEvent {
    data object SaveSuccess : AddRecordEvent()
}

@HiltViewModel
class AddRecordViewModel @Inject constructor(
    private val timeRecordRepository: TimeRecordRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddRecordUiState())
    val uiState: StateFlow<AddRecordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddRecordEvent>()
    val events = _events.asSharedFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTitles: StateFlow<List<String>> = timeRecordRepository.getRecentTitles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val recordId = savedStateHandle.get<Long>("recordId") ?: 0L
        if (recordId > 0) {
            loadRecord(recordId)
        } else {
            viewModelScope.launch {
                val records = timeRecordRepository.getAllRecords().first()
                val today = java.time.LocalDate.now()
                // Find all records that ended today
                val todayRecords = records.filter { it.endTime.toLocalDate() == today }
                if (todayRecords.isNotEmpty()) {
                    val latestEndTime = todayRecords.maxOf { it.endTime }
                    val now = LocalDateTime.now().withSecond(0).withNano(0)
                    // If latestEndTime is in the future (e.g. tracking anomaly), cap it at 'now'
                    val newStartTime = if (latestEndTime.isAfter(now)) now else latestEndTime
                    _uiState.update { it.copy(startTime = newStartTime, endTime = newStartTime) }
                }
            }
        }
    }

    private fun loadRecord(id: Long) {
        viewModelScope.launch {
            timeRecordRepository.getRecordById(id)?.let { record ->
                _uiState.update {
                    it.copy(
                        title = record.title ?: "",
                        selectedCategoryId = record.categoryId,
                        selectedTagIds = record.tags.map { t -> t.id }.toSet(),
                        startTime = record.startTime,
                        endTime = record.endTime,
                        note = record.note ?: "",
                        isEditing = true,
                        editingRecordId = record.id
                    )
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null) }
    }

    fun selectCategory(categoryId: Long) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, errorMessage = null) }
    }

    fun toggleTag(tag: Tag) {
        _uiState.update { state ->
            val newTags = if (tag.id in state.selectedTagIds) {
                state.selectedTagIds - tag.id
            } else {
                state.selectedTagIds + tag.id
            }
            state.copy(selectedTagIds = newTags)
        }
    }

    fun updateStartTime(dateTime: LocalDateTime) {
        _uiState.update { state ->
            val duration = java.time.Duration.between(state.startTime, state.endTime)
            val newEnd = dateTime.plus(duration)
            state.copy(startTime = dateTime, endTime = newEnd, errorMessage = null)
        }
    }

    fun updateEndTime(dateTime: LocalDateTime) {
        _uiState.update { it.copy(endTime = dateTime, errorMessage = null) }
    }

    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            val tagId = tagRepository.insertTag(Tag(name = name))
            _uiState.update { it.copy(selectedTagIds = it.selectedTagIds + tagId) }
        }
    }

    fun updateDate(newDateTime: LocalDateTime) {
        val date = newDateTime.toLocalDate()
        viewModelScope.launch {
            val latestEnd = timeRecordRepository.getLatestEndTimeOnDate(date)
            if (latestEnd != null) {
                val now = LocalDateTime.now().withSecond(0).withNano(0)
                val snapped = if (latestEnd.isAfter(now)) now else latestEnd
                _uiState.update { it.copy(startTime = snapped, endTime = snapped, errorMessage = null) }
            } else {
                val sevenAm = LocalDateTime.of(date, java.time.LocalTime.of(7, 0))
                _uiState.update { it.copy(startTime = sevenAm, endTime = sevenAm, errorMessage = null) }
            }
        }
    }

    fun addDuration(minutes: Long) {
        _uiState.update { state ->
            val newEndTime = state.endTime.plusMinutes(minutes)
            state.copy(endTime = newEndTime, errorMessage = null)
        }
    }

    fun save() {
        val state = _uiState.value

        if (state.selectedCategoryId == null) {
            _uiState.update { it.copy(errorMessage = "select_category") }
            return
        }
        if (!state.endTime.isAfter(state.startTime)) {
            _uiState.update { it.copy(errorMessage = "end_before_start") }
            return
        }

        viewModelScope.launch {
            val record = TimeRecord(
                id = if (state.isEditing) state.editingRecordId else 0,
                type = RecordType.NORMAL,
                categoryId = state.selectedCategoryId,
                title = state.title.ifBlank { null },
                startTime = state.startTime,
                endTime = state.endTime,
                durationMinutes = state.durationMinutes,
                note = state.note.ifBlank { null }
            )

            if (state.isEditing) {
                timeRecordRepository.updateRecord(record, state.selectedTagIds.toList())
            } else {
                timeRecordRepository.insertRecord(record, state.selectedTagIds.toList())
            }

            _events.emit(AddRecordEvent.SaveSuccess)
        }
    }

    fun delete() {
        val state = _uiState.value
        if (state.isEditing) {
            viewModelScope.launch {
                timeRecordRepository.getRecordById(state.editingRecordId)?.let { record ->
                    timeRecordRepository.deleteRecord(record)
                    _events.emit(AddRecordEvent.SaveSuccess)
                }
            }
        }
    }
}
