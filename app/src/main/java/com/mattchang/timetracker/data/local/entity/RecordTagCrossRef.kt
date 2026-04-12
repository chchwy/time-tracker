package com.mattchang.timetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "record_tag_cross_ref",
    primaryKeys = ["record_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = TimeRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["record_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tag_id")]
)
data class RecordTagCrossRef(
    @ColumnInfo(name = "record_id")
    val recordId: Long,

    @ColumnInfo(name = "tag_id")
    val tagId: Long
)
