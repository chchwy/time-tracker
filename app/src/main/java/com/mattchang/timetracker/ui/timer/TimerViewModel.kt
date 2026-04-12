package com.mattchang.timetracker.ui.timer

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

data class TimerUiState(
    val isRunning: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val selectedCategoryId: Long? = null,
    val selectedTagIds: Set<Long> = emptySet()
)

sealed class TimerEvent {
    /** 停止後通知 UI，帶已建立的紀錄 id 供跳轉編輯 */
    data class RecordCreated(val recordId: Long, val durationMinutes: Int) : TimerEvent()
}

private const val KEY_IS_RUNNING = "timer_is_running"
private const val KEY_START_EPOCH = "timer_start_epoch"
private const val KEY_CATEGORY_ID = "timer_category_id"

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timeRecordRepository: TimeRecordRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TimerEvent>()
    val events = _events.asSharedFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var tickJob: Job? = null

    /** Epoch millis when the timer was started (survives rotation via SavedStateHandle) */
    private var startEpochMillis: Long = 0L

    init {
        // Restore state after process recreation (rotation, etc.)
        val wasRunning = savedStateHandle.get<Boolean>(KEY_IS_RUNNING) ?: false
        val startEpoch = savedStateHandle.get<Long>(KEY_START_EPOCH) ?: 0L
        val categoryId = savedStateHandle.get<Long?>(KEY_CATEGORY_ID)

        if (wasRunning && startEpoch > 0L) {
            startEpochMillis = startEpoch
            val elapsed = (System.currentTimeMillis() - startEpoch) / 1000L
            _uiState.update {
                it.copy(
                    isRunning = true,
                    elapsedSeconds = elapsed,
                    selectedCategoryId = categoryId
                )
            }
            startTick()
        } else if (categoryId != null) {
            _uiState.update { it.copy(selectedCategoryId = categoryId) }
        }
    }

    fun start() {
        if (_uiState.value.isRunning) return
        startEpochMillis = System.currentTimeMillis()
        savedStateHandle[KEY_IS_RUNNING] = true
        savedStateHandle[KEY_START_EPOCH] = startEpochMillis
        _uiState.update { it.copy(isRunning = true, elapsedSeconds = 0L) }
        startTick()
    }

    fun stop() {
        if (!_uiState.value.isRunning) return
        tickJob?.cancel()

        val stopTime = System.currentTimeMillis()
        val durationMs = stopTime - startEpochMillis
        val durationMinutes = (durationMs / 1000 / 60).toInt().coerceAtLeast(1)

        savedStateHandle[KEY_IS_RUNNING] = false
        savedStateHandle.remove<Long>(KEY_START_EPOCH)

        val state = _uiState.value
        val startTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(startEpochMillis), ZoneId.systemDefault()
        )
        val endTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(stopTime), ZoneId.systemDefault()
        )

        viewModelScope.launch {
            val record = TimeRecord(
                type = RecordType.NORMAL,
                categoryId = state.selectedCategoryId,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes
            )
            val newId = timeRecordRepository.insertRecord(record, state.selectedTagIds.toList())
            _uiState.update { it.copy(isRunning = false, elapsedSeconds = 0L) }
            _events.emit(TimerEvent.RecordCreated(newId, durationMinutes))
        }
    }

    fun selectCategory(categoryId: Long) {
        savedStateHandle[KEY_CATEGORY_ID] = categoryId
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
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

    fun createTag(name: String) {
        viewModelScope.launch {
            val tagId = tagRepository.insertTag(Tag(name = name))
            _uiState.update { it.copy(selectedTagIds = it.selectedTagIds + tagId) }
        }
    }

    private fun startTick() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val elapsed = (System.currentTimeMillis() - startEpochMillis) / 1000L
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
