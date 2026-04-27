package com.mattchang.timetracker.domain.model

data class Tag(
    val id: Long = 0,
    val name: String,
    val isArchived: Boolean = false
)
