package com.mattchang.timetracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class RecordWithTags(
    @Embedded val record: TimeRecordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            RecordTagCrossRef::class,
            parentColumn = "record_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity>
)
