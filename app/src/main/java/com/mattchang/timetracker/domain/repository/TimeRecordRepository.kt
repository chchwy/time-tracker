package com.mattchang.timetracker.domain.repository

import com.mattchang.timetracker.domain.model.TimeRecord
import kotlinx.coroutines.flow.Flow

interface TimeRecordRepository {
    fun getAllRecords(): Flow<List<TimeRecord>>
    fun getRecordsBetween(fromMillis: Long, toMillis: Long): Flow<List<TimeRecord>>
    fun getSleepRecords(): Flow<List<TimeRecord>>
    fun getRecentTitles(): Flow<List<String>>
    suspend fun getRecordById(id: Long): TimeRecord?
    suspend fun insertRecord(record: TimeRecord, tagIds: List<Long>): Long
    suspend fun updateRecord(record: TimeRecord, tagIds: List<Long>)
    suspend fun deleteRecord(record: TimeRecord)
}
