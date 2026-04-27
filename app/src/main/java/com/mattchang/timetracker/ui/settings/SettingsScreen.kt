package com.mattchang.timetracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mattchang.timetracker.R
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.util.LocaleHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToTags: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var isEnglish by remember {
        mutableStateOf(LocaleHelper.getSavedLanguage(context) == LocaleHelper.LANG_EN)
    }

    val importSuccessTemplate = stringResource(R.string.import_success)
    val importFailedMsg = stringResource(R.string.import_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ImportDone -> snackbarHostState.showSnackbar(
                    importSuccessTemplate
                        .replace("%1\$d", event.result.imported.toString())
                        .replace("%2\$d", event.result.skipped.toString())
                )
                SettingsEvent.ImportFailed -> snackbarHostState.showSnackbar(importFailedMsg)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings)) }
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
            // ── Language section ───────────────────────────────────────────────
            item {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(
                            text = if (isEnglish) "English" else "中文",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isEnglish,
                            onCheckedChange = { checked ->
                                isEnglish = checked
                                LocaleHelper.saveLanguage(
                                    context,
                                    if (checked) LocaleHelper.LANG_EN else LocaleHelper.LANG_ZH
                                )
                                activity?.recreate()
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Text(
                    text = stringResource(R.string.categories),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToCategories() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                        Text(stringResource(R.string.manage_categories), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToTags() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                        Text(stringResource(R.string.manage_tags), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // ── Local CSV section ─────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.data_management),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                val context = LocalContext.current
                var csvContent by remember { mutableStateOf("") }

                val createDocumentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/csv")
                ) { uri ->
                    uri?.let { viewModel.saveCsvToUri(context, it, csvContent) }
                }

                val openDocumentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { viewModel.importCsvFromUri(context, it) }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                csvContent = viewModel.generateCsvContent()
                                val dateStr = java.time.LocalDate.now()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                createDocumentLauncher.launch("time_tracker_backup_$dateStr.csv")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.export_csv))
                    }

                    OutlinedButton(
                        onClick = { openDocumentLauncher.launch("text/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.import_csv))
                    }
                }
            }
        }
    }
}
