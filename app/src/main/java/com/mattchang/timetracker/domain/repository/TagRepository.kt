package com.mattchang.timetracker.domain.repository

import com.mattchang.timetracker.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    fun getActiveTags(): Flow<List<Tag>>
    suspend fun insertTag(tag: Tag): Long
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(id: Long)
}
