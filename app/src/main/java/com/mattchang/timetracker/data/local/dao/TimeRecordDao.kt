package com.mattchang.timetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mattchang.timetracker.data.local.entity.RecordTagCrossRef
import com.mattchang.timetracker.data.local.entity.RecordWithTags
import com.mattchang.timetracker.data.local.entity.TimeRecordEntity
import kotlinx.coroutines.flow.Flow

data class CategorySummary(
    val categoryId: Long?,
    val totalMinutes: Int
)

@Dao
interface TimeRecordDao {

    @Transaction
    @Query("SELECT * FROM time_records ORDER BY start_time DESC")
    fun getAllRecordsWithTags(): Flow<List<RecordWithTags>>

    @Transaction
    @Query("SELECT * FROM time_records WHERE start_time >= :from AND start_time < :to ORDER BY start_time DESC")
    fun getRecordsBetween(from: Long, to: Long): Flow<List<RecordWithTags>>

    @Transaction
    @Query("SELECT * FROM time_records WHERE type = 'SLEEP' ORDER BY start_time DESC")
    fun getSleepRecords(): Flow<List<RecordWithTags>>

    @Query("SELECT * FROM time_records WHERE id = :id")
    suspend fun getRecordById(id: Long): TimeRecordEntity?

    @Query(
        "SELECT category_id AS categoryId, SUM(duration_minutes) AS totalMinutes " +
        "FROM time_records WHERE start_time >= :from AND start_time < :to " +
        "GROUP BY category_id"
    )
    fun getCategorySummary(from: Long, to: Long): Flow<List<CategorySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TimeRecordEntity): Long

    @Update
    suspend fun updateRecord(record: TimeRecordEntity)

    @Delete
    suspend fun deleteRecord(record: TimeRecordEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecordTagCrossRefs(crossRefs: List<RecordTagCrossRef>)

    @Query("DELETE FROM record_tag_cross_ref WHERE record_id = :recordId")
    suspend fun deleteTagsForRecord(recordId: Long)

    @Transaction
    suspend fun setTagsForRecord(recordId: Long, tagIds: List<Long>) {
        deleteTagsForRecord(recordId)
        val crossRefs = tagIds.map { RecordTagCrossRef(recordId, it) }
        insertRecordTagCrossRefs(crossRefs)
    }
}
