package com.mattchang.timetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            val sortOrder = (categories.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            categoryRepository.insertCategory(
                Category(
                    name = name,
                    icon = "circle",
                    colorHex = colorHex,
                    isDefault = false,
                    sortOrder = sortOrder
                )
            )
        }
    }

    fun updateCategory(category: Category, newName: String, newColorHex: String) {
        viewModelScope.launch {
            categoryRepository.updateCategory(
                category.copy(
                    name = newName,
                    colorHex = newColorHex
                )
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}
