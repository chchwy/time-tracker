package com.mattchang.timetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mattchang.timetracker.R
import com.mattchang.timetracker.domain.model.Tag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelector(
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    onTagToggled: (Tag) -> Unit,
    onCreateTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.tags),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_tag),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = tag.id in selectedTagIds,
                    onClick = { onTagToggled(tag) },
                    label = { Text(tag.name) }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateTagDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreateTag(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateTagDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var tagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_tag)) },
        text = {
            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text(stringResource(R.string.tag_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (tagName.isNotBlank()) onCreate(tagName.trim()) },
                enabled = tagName.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
