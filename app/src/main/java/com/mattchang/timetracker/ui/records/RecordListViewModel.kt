package com.mattchang.timetracker.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.TimeRecord
import com.mattchang.timetracker.domain.repository.CategoryRepository
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordListViewModel @Inject constructor(
    private val timeRecordRepository: TimeRecordRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val records: StateFlow<List<TimeRecord>> = timeRecordRepository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteRecord(record: TimeRecord) {
        viewModelScope.launch {
            timeRecordRepository.deleteRecord(record)
        }
    }
}
