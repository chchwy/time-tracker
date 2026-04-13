package com.mattchang.timetracker.data.repository

import com.mattchang.timetracker.data.local.dao.TimeRecordDao
import com.mattchang.timetracker.data.local.entity.RecordWithTags
import com.mattchang.timetracker.data.local.entity.TimeRecordEntity
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.RecordType
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.model.TimeRecord
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeRecordRepositoryImpl @Inject constructor(
    private val timeRecordDao: TimeRecordDao
) : TimeRecordRepository {

    override fun getAllRecords(): Flow<List<TimeRecord>> {
        return timeRecordDao.getAllRecordsWithTags().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecordsBetween(fromMillis: Long, toMillis: Long): Flow<List<TimeRecord>> {
        return timeRecordDao.getRecordsBetween(fromMillis, toMillis).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSleepRecords(): Flow<List<TimeRecord>> {
        return timeRecordDao.getSleepRecords().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecentTitles(): Flow<List<String>> {
        return timeRecordDao.getRecentTitles()
    }

    override suspend fun getRecordById(id: Long): TimeRecord? {
        return timeRecordDao.getRecordById(id)?.let { entity ->
            RecordWithTags(entity, emptyList()).toDomain()
        }
    }

    override suspend fun insertRecord(record: TimeRecord, tagIds: List<Long>): Long {
        val entity = record.toEntity()
        val id = timeRecordDao.insertRecord(entity)
        if (tagIds.isNotEmpty()) {
            timeRecordDao.setTagsForRecord(id, tagIds)
        }
        return id
    }

    override suspend fun updateRecord(record: TimeRecord, tagIds: List<Long>) {
        val entity = record.toEntity()
        timeRecordDao.updateRecord(entity)
        timeRecordDao.setTagsForRecord(record.id, tagIds)
    }

    override suspend fun deleteRecord(record: TimeRecord) {
        timeRecordDao.deleteRecord(record.toEntity())
    }

    private fun RecordWithTags.toDomain(): TimeRecord {
        return TimeRecord(
            id = record.id,
            type = RecordType.valueOf(record.type),
            categoryId = record.categoryId,
            title = record.title,
            startTime = millisToLocalDateTime(record.startTime),
            endTime = millisToLocalDateTime(record.endTime),
            durationMinutes = record.durationMinutes,
            note = record.note,
            tags = tags.map { Tag(id = it.id, name = it.name) },
            createdAt = millisToLocalDateTime(record.createdAt),
            updatedAt = millisToLocalDateTime(record.updatedAt),
            childInterrupted = record.childInterrupted,
            usedComputerBeforeBed = record.usedComputerBeforeBed,
            readBookBeforeBed = record.readBookBeforeBed,
            bookTitleBeforeBed = record.bookTitleBeforeBed,
            chattedWithWife = record.chattedWithWife,
            stayUpLateReason = record.stayUpLateReason,
            morningEnergyIndex = record.morningEnergyIndex
        )
    }

    private fun TimeRecord.toEntity(): TimeRecordEntity {
        return TimeRecordEntity(
            id = id,
            type = type.name,
            categoryId = categoryId,
            title = title,
            startTime = localDateTimeToMillis(startTime),
            endTime = localDateTimeToMillis(endTime),
            durationMinutes = durationMinutes,
            note = note,
            createdAt = localDateTimeToMillis(createdAt),
            updatedAt = localDateTimeToMillis(updatedAt),
            childInterrupted = childInterrupted,
            usedComputerBeforeBed = usedComputerBeforeBed,
            readBookBeforeBed = readBookBeforeBed,
            bookTitleBeforeBed = bookTitleBeforeBed,
            chattedWithWife = chattedWithWife,
            stayUpLateReason = stayUpLateReason,
            morningEnergyIndex = morningEnergyIndex
        )
    }

    private fun millisToLocalDateTime(millis: Long): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    }

    private fun localDateTimeToMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
