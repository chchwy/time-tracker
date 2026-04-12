package com.mattchang.timetracker.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String? = null,
    val colorHex: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
