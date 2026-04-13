package com.mattchang.timetracker.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.repository.CategoryRepository
import com.mattchang.timetracker.domain.repository.DriveBackupRepository
import com.mattchang.timetracker.domain.repository.TagRepository
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import com.mattchang.timetracker.util.CsvSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ImportResult(val imported: Int, val skipped: Int)

data class CloudBackupUiState(
    val isSignedIn: Boolean = false,
    val accountEmail: String? = null,
    val lastBackupTime: String? = null,
    val isBusy: Boolean = false
)

sealed class SettingsEvent {
    data class ImportDone(val result: ImportResult) : SettingsEvent()
    data object ImportFailed : SettingsEvent()
    data object BackupDone : SettingsEvent()
    data object BackupFailed : SettingsEvent()
    data class RestoreDone(val result: ImportResult) : SettingsEvent()
    data object RestoreFailed : SettingsEvent()
    data object NoBackupFound : SettingsEvent()
    data object SignInFailed : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val timeRecordRepository: TimeRecordRepository,
    private val tagRepository: TagRepository,
    private val driveBackupRepository: DriveBackupRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cloudState = MutableStateFlow(CloudBackupUiState())
    val cloudState: StateFlow<CloudBackupUiState> = _cloudState.asStateFlow()

    init {
        refreshCloudState()
    }

    private fun refreshCloudState() {
        _cloudState.update {
            it.copy(
                isSignedIn = driveBackupRepository.isSignedIn(),
                accountEmail = driveBackupRepository.getSignedInEmail(),
                lastBackupTime = driveBackupRepository.getLastBackupTimestamp()
            )
        }
    }

    // ── Category management ───────────────────────────────────────────────

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            val sortOrder = (categories.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            categoryRepository.insertCategory(
                Category(
                    name = name,
                    icon = "circle",
                    colorHex = colorHex,
                    isDefault = false,
                    sortOrder = sortOrder
                )
            )
        }
    }

    fun updateCategory(category: Category, newName: String, newColorHex: String) {
        viewModelScope.launch {
            categoryRepository.updateCategory(
                category.copy(name = newName, colorHex = newColorHex)
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    // ── CSV local export/import ───────────────────────────────────────────

    suspend fun generateCsvContent(): String {
        val records = timeRecordRepository.getAllRecords().first()
        val categoryMap = categoryRepository.getAllCategories().first().associateBy { it.id }
        return CsvSerializer.buildCsvContent(records, categoryMap)
    }

    fun saveCsvToUri(context: Context, uri: Uri, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importCsvFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: run {
                    _events.emit(SettingsEvent.ImportFailed)
                    return@launch
                }
                val result = insertCsvRows(CsvSerializer.stripBom(bytes))
                _events.emit(SettingsEvent.ImportDone(result))
            } catch (e: Exception) {
                e.printStackTrace()
                _events.emit(SettingsEvent.ImportFailed)
            }
        }
    }

    // ── Google Drive backup ───────────────────────────────────────────────

    fun getSignInIntent(): Intent = driveBackupRepository.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            val success = driveBackupRepository.handleSignInResult(data)
            refreshCloudState()
            if (!success) _events.emit(SettingsEvent.SignInFailed)
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            driveBackupRepository.signOut()
            refreshCloudState()
        }
    }

    fun backupToCloud() {
        viewModelScope.launch {
            _cloudState.update { it.copy(isBusy = true) }
            try {
                val csv = generateCsvContent()
                val result = driveBackupRepository.uploadBackup(csv)
                if (result.isSuccess) {
                    refreshCloudState()
                    _events.emit(SettingsEvent.BackupDone)
                } else {
                    _events.emit(SettingsEvent.BackupFailed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _events.emit(SettingsEvent.BackupFailed)
            } finally {
                _cloudState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _cloudState.update { it.copy(isBusy = true) }
            try {
                val downloadResult = driveBackupRepository.downloadBackup()
                if (downloadResult.isFailure) {
                    val msg = downloadResult.exceptionOrNull()?.message ?: ""
                    if (msg.contains("No backup")) {
                        _events.emit(SettingsEvent.NoBackupFound)
                    } else {
                        _events.emit(SettingsEvent.RestoreFailed)
                    }
                    return@launch
                }
                val csv = downloadResult.getOrThrow()
                val result = insertCsvRows(csv)
                _events.emit(SettingsEvent.RestoreDone(result))
            } catch (e: Exception) {
                e.printStackTrace()
                _events.emit(SettingsEvent.RestoreFailed)
            } finally {
                _cloudState.update { it.copy(isBusy = false) }
            }
        }
    }

    // ── Shared CSV insert logic ───────────────────────────────────────────

    private suspend fun insertCsvRows(content: String): ImportResult = withContext(Dispatchers.IO) {
        val rows = CsvSerializer.parseContent(content)
        if (rows.isEmpty()) return@withContext ImportResult(0, 0)

        val categoryCache = categoryRepository.getAllCategories().first()
            .associateBy { it.name }.toMutableMap<String, Category>()
        val tagCache = tagRepository.getAllTags().first()
            .associateBy { it.name }.toMutableMap<String, Tag>()

        var imported = 0
        var skipped = 0

        for (fields in rows) {
            try {
                if (fields.size < CsvSerializer.Col.MIN_FIELDS) { skipped++; continue }

                val categoryId: Long? = fields[CsvSerializer.Col.CATEGORY].trim().let { name ->
                    if (name.isBlank()) return@let null
                    categoryCache[name]?.id ?: run {
                        val newId = categoryRepository.insertCategory(
                            Category(name = name, icon = "circle", colorHex = "#9E9E9E", isDefault = false, sortOrder = 99)
                        )
                        categoryCache[name] = Category(id = newId, name = name, icon = "circle", colorHex = "#9E9E9E", isDefault = false, sortOrder = 99)
                        newId
                    }
                }

                val tagIds: List<Long> = CsvSerializer.parseTagNames(fields[CsvSerializer.Col.TAGS])
                    .map { tagName ->
                        tagCache[tagName]?.id ?: run {
                            val newId = tagRepository.insertTag(Tag(name = tagName))
                            tagCache[tagName] = Tag(id = newId, name = tagName)
                            newId
                        }
                    }

                val record = CsvSerializer.rowToRecord(fields, categoryId)
                if (record == null) { skipped++; continue }

                timeRecordRepository.insertRecord(record, tagIds)
                imported++
            } catch (e: Exception) {
                skipped++
            }
        }

        ImportResult(imported, skipped)
    }
}
