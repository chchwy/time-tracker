package com.mattchang.timetracker.data.repository

import com.mattchang.timetracker.data.local.dao.TagDao
import com.mattchang.timetracker.data.local.entity.TagEntity
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveTags(): Flow<List<Tag>> {
        return tagDao.getActiveTags().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertTag(tag: Tag): Long {
        return tagDao.insertTag(tag.toEntity())
    }

    override suspend fun updateTag(tag: Tag) {
        tagDao.updateTag(tag.toEntity())
    }

    override suspend fun deleteTag(id: Long) {
        tagDao.deleteTagById(id)
    }

    private fun TagEntity.toDomain() = Tag(id = id, name = name, isArchived = isArchived)
    private fun Tag.toEntity() = TagEntity(id = id, name = name, isArchived = isArchived)
}
