package com.mattchang.timetracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Filter suggestions based on current value matching, and ignore exact matches
    val filteredSuggestions = suggestions.filter {
        it.contains(value, ignoreCase = true) && it != value
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredSuggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        if (filteredSuggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredSuggestions.take(5).forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onValueChange(selectionOption)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
