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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Logout
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattchang.timetracker.R
import com.mattchang.timetracker.domain.model.Category
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val cloudState by viewModel.cloudState.collectAsStateWithLifecycle()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val importSuccessTemplate = stringResource(R.string.import_success)
    val importFailedMsg = stringResource(R.string.import_failed)
    val backupDoneMsg = stringResource(R.string.backup_done)
    val backupFailedMsg = stringResource(R.string.backup_failed)
    val restoreFailedMsg = stringResource(R.string.restore_failed)
    val noBackupMsg = stringResource(R.string.no_backup_found)
    val signInFailedMsg = stringResource(R.string.sign_in_failed)
    val restoreSuccessTemplate = stringResource(R.string.restore_success)

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ImportDone -> snackbarHostState.showSnackbar(
                    importSuccessTemplate
                        .replace("%1\$d", event.result.imported.toString())
                        .replace("%2\$d", event.result.skipped.toString())
                )
                SettingsEvent.ImportFailed -> snackbarHostState.showSnackbar(importFailedMsg)
                SettingsEvent.BackupDone -> snackbarHostState.showSnackbar(backupDoneMsg)
                SettingsEvent.BackupFailed -> snackbarHostState.showSnackbar(backupFailedMsg)
                is SettingsEvent.RestoreDone -> snackbarHostState.showSnackbar(
                    restoreSuccessTemplate
                        .replace("%1\$d", event.result.imported.toString())
                        .replace("%2\$d", event.result.skipped.toString())
                )
                SettingsEvent.RestoreFailed -> snackbarHostState.showSnackbar(restoreFailedMsg)
                SettingsEvent.NoBackupFound -> snackbarHostState.showSnackbar(noBackupMsg)
                SettingsEvent.SignInFailed -> snackbarHostState.showSnackbar(signInFailedMsg)
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

            // ── Google Drive backup section ───────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.cloud_backup),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                CloudBackupSection(
                    state = cloudState,
                    onSignIn = { signInLauncher.launch(viewModel.getSignInIntent()) },
                    onSignOut = { viewModel.signOutGoogle() },
                    onBackup = { viewModel.backupToCloud() },
                    onRestore = { showRestoreConfirm = true }
                )
            }
        }

        if (showRestoreConfirm) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirm = false },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                title = { Text(stringResource(R.string.restore_confirm_title)) },
                text = { Text(stringResource(R.string.restore_confirm_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        showRestoreConfirm = false
                        viewModel.restoreFromCloud()
                    }) {
                        Text(stringResource(R.string.restore))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirm = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun CloudBackupSection(
    state: CloudBackupUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.isSignedIn) {
                Text(
                    text = stringResource(R.string.cloud_backup_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.sign_in_google))
                }
            } else {
                // Account info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.accountEmail ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (state.lastBackupTime != null) {
                            Text(
                                text = stringResource(R.string.last_backup, state.lastBackupTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.no_backup_yet),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onSignOut, enabled = !state.isBusy) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = stringResource(R.string.sign_out),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackup,
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(stringResource(R.string.backup_now))
                    }
                    OutlinedButton(
                        onClick = onRestore,
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(stringResource(R.string.restore))
                    }
                }
            }
        }
    }
}


