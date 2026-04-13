package com.mattchang.timetracker.ui.addrecord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattchang.timetracker.R
import com.mattchang.timetracker.ui.components.AutoCompleteTextField
import com.mattchang.timetracker.ui.components.CategorySelector
import com.mattchang.timetracker.ui.components.DateTimeField
import com.mattchang.timetracker.ui.components.DurationText
import com.mattchang.timetracker.ui.components.TagSelector

@Composable
fun AddRecordScreen(
    onSaved: () -> Unit = {},
    viewModel: AddRecordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddRecordEvent.SaveSuccess -> onSaved()
            }
        }
    }

    val errorText = when (uiState.errorMessage) {
        "select_category" -> stringResource(R.string.select_category_error)
        "end_before_start" -> stringResource(R.string.end_before_start_error)
        else -> null
    }

    LaunchedEffect(errorText) {
        errorText?.let { snackbarHostState.showSnackbar(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (uiState.isEditing) stringResource(R.string.edit_record_title)
            else stringResource(R.string.add_record_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val recentTitles by viewModel.recentTitles.collectAsStateWithLifecycle()
            AutoCompleteTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                suggestions = recentTitles,
                label = stringResource(R.string.title),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.labelLarge
            )
            CategorySelector(
                categories = categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = { viewModel.selectCategory(it.id) }
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DateTimeField(
                    label = stringResource(R.string.start_time),
                    dateTime = uiState.startTime,
                    onDateTimeChanged = viewModel::updateStartTime,
                    modifier = Modifier.weight(1f)
                )
                DateTimeField(
                    label = stringResource(R.string.end_time),
                    dateTime = uiState.endTime,
                    onDateTimeChanged = viewModel::updateEndTime,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.duration) + ": ",
                    style = MaterialTheme.typography.bodyLarge
                )
                DurationText(
                    minutes = uiState.durationMinutes,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            HorizontalDivider()

            TagSelector(
                tags = tags,
                selectedTagIds = uiState.selectedTagIds,
                onTagToggled = viewModel::toggleTag,
                onCreateTag = viewModel::createTag
            )

            HorizontalDivider()

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::updateNote,
                label = { Text(stringResource(R.string.note)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.isEditing) {
                androidx.compose.material3.OutlinedButton(
                    onClick = viewModel::delete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.save))
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
