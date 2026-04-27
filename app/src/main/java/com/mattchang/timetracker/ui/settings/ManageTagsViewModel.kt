package com.mattchang.timetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageTagsViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    val tags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun renameTag(tag: Tag, newName: String) {
        viewModelScope.launch {
            tagRepository.updateTag(tag.copy(name = newName.trim()))
        }
    }

    fun archiveTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.updateTag(tag.copy(isArchived = true))
        }
    }

    fun unarchiveTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.updateTag(tag.copy(isArchived = false))
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag.id)
        }
    }
}
