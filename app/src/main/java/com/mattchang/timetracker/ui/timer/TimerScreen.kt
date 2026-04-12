package com.mattchang.timetracker.ui.timer

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattchang.timetracker.R
import com.mattchang.timetracker.service.TimerForegroundService
import com.mattchang.timetracker.ui.components.CategoryChip
import com.mattchang.timetracker.ui.components.TagSelector

@Composable
fun TimerScreen(
    onNavigateToEdit: (Long) -> Unit = {},
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val savedText  = stringResource(R.string.timer_record_saved)
    val editText   = stringResource(R.string.edit)

    // Collect one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TimerEvent.RecordCreated -> {
                    // Stop foreground service
                    ContextCompat.startForegroundService(
                        context, TimerForegroundService.stopIntent(context)
                    )

                    val result = snackbarHostState.showSnackbar(
                        message = savedText.format(event.durationMinutes),
                        actionLabel = editText
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onNavigateToEdit(event.recordId)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Clock display ──────────────────────────────────────────────
            TimerDisplay(elapsedSeconds = uiState.elapsedSeconds, isRunning = uiState.isRunning)

            Spacer(modifier = Modifier.height(48.dp))

            // ── Start / Stop button ────────────────────────────────────────
            StartStopButton(
                isRunning = uiState.isRunning,
                onStart = {
                    viewModel.start()
                    startForegroundService(context, System.currentTimeMillis())
                },
                onStop = { viewModel.stop() }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Category picker (horizontal scroll) ───────────────────────
            if (categories.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.category),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            CategoryChip(
                                category = category,
                                selected = category.id == uiState.selectedCategoryId,
                                onClick = { viewModel.selectCategory(category.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Tag multi-select ───────────────────────────────────────────
            TagSelector(
                tags = tags,
                selectedTagIds = uiState.selectedTagIds,
                onTagToggled = viewModel::toggleTag,
                onCreateTag = viewModel::createTag,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Clock display ──────────────────────────────────────────────────────────

@Composable
private fun TimerDisplay(elapsedSeconds: Long, isRunning: Boolean) {
    val h = elapsedSeconds / 3600
    val m = (elapsedSeconds % 3600) / 60
    val s = elapsedSeconds % 60
    val text = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

    val color by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(400),
        label = "timerColor"
    )

    Text(
        text = text,
        fontSize = 72.sp,
        fontWeight = FontWeight.Thin,
        fontFamily = FontFamily.Monospace,
        color = color
    )
}

// ─── Start / Stop button ─────────────────────────────────────────────────────

@Composable
private fun StartStopButton(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1.1f else 1f,
        animationSpec = tween(300),
        label = "btnScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "btnColor"
    )

    Button(
        onClick = if (isRunning) onStop else onStart,
        modifier = Modifier
            .size(96.dp)
            .scale(scale),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isRunning) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = stringResource(R.string.stop),
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.start),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun startForegroundService(context: Context, startEpoch: Long) {
    ContextCompat.startForegroundService(
        context,
        TimerForegroundService.startIntent(context, startEpoch)
    )
}
