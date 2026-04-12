package com.mattchang.timetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mattchang.timetracker.domain.model.Category

@Composable
fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(category.name) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(chipColor, CircleShape)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = chipColor.copy(alpha = 0.15f)
        ),
        border = if (selected) {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = true,
                selectedBorderColor = chipColor
            )
        } else {
            FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        categories.forEach { category ->
            CategoryChip(
                category = category,
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}
