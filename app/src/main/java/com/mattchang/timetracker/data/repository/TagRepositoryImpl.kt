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
        return tagDao.getAllTags().map { list ->
            list.map { Tag(id = it.id, name = it.name) }
        }
    }

    override suspend fun insertTag(tag: Tag): Long {
        return tagDao.insertTag(TagEntity(id = tag.id, name = tag.name))
    }

    override suspend fun deleteTag(id: Long) {
        tagDao.deleteTagById(id)
    }
}
