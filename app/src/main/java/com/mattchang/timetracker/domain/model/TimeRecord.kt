package com.mattchang.timetracker.domain.model

import java.time.LocalDateTime

data class TimeRecord(
    val id: Long = 0,
    val type: RecordType = RecordType.NORMAL,
    val categoryId: Long? = null,
    val category: Category? = null,
    val title: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationMinutes: Int,
    val note: String? = null,
    val tags: List<Tag> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val childInterrupted: Boolean? = null,
    val usedComputerBeforeBed: Boolean? = null,
    val readBookBeforeBed: Boolean? = null,
    val chattedWithWife: Boolean? = null,
    val stayUpLateReason: String? = null,
    val morningEnergyIndex: Int? = null
) {
    val isSleep: Boolean get() = type == RecordType.SLEEP
}
