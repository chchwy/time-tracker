package com.mattchang.timetracker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattchang.timetracker.R
import com.mattchang.timetracker.domain.model.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTagsScreen(
    onBack: () -> Unit,
    viewModel: ManageTagsViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val activeTags = tags.filter { !it.isArchived }
    val archivedTags = tags.filter { it.isArchived }

    var editingTag by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_tags)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.active_tags),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (activeTags.isEmpty()) {
                item {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                }
            } else {
                items(activeTags, key = { it.id }) { tag ->
                    TagEditItem(
                        tag = tag,
                        onEdit = { editingTag = tag },
                        onArchive = { viewModel.archiveTag(tag) },
                        onDelete = { viewModel.deleteTag(tag) }
                    )
                }
            }

            if (archivedTags.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.archived_tags),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(archivedTags, key = { it.id }) { tag ->
                    TagEditItem(
                        tag = tag,
                        onEdit = { editingTag = tag },
                        onArchive = { viewModel.unarchiveTag(tag) },
                        onDelete = { viewModel.deleteTag(tag) }
                    )
                }
            }
        }
    }

    editingTag?.let { tag ->
        RenameTagDialog(
            tag = tag,
            onDismiss = { editingTag = null },
            onSave = { newName ->
                viewModel.renameTag(tag, newName)
                editingTag = null
            }
        )
    }
}

@Composable
private fun TagEditItem(
    tag: Tag,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tag.isArchived) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onArchive) {
                Icon(
                    if (tag.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = if (tag.isArchived) stringResource(R.string.unarchive)
                                         else stringResource(R.string.archive),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RenameTagDialog(
    tag: Tag,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(tag.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_tag)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.tag_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank()
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
