package com.mattchang.timetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "time_records",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("start_time"),
        Index("type"),
        Index("category_id")
    ]
)
data class TimeRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: String = "NORMAL",

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    val title: String? = null,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long,

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,

    val note: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "child_interrupted")
    val childInterrupted: Boolean? = null,

    @ColumnInfo(name = "used_computer_before_bed")
    val usedComputerBeforeBed: Boolean? = null,

    @ColumnInfo(name = "read_book_before_bed")
    val readBookBeforeBed: Boolean? = null,

    @ColumnInfo(name = "chatted_with_wife")
    val chattedWithWife: Boolean? = null,

    @ColumnInfo(name = "stay_up_late_reason")
    val stayUpLateReason: String? = null,

    @ColumnInfo(name = "morning_energy_index")
    val morningEnergyIndex: Int? = null
)
