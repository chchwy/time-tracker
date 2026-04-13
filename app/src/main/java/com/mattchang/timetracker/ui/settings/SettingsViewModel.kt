package com.mattchang.timetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.repository.CategoryRepository
import com.mattchang.timetracker.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import android.net.Uri
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import com.mattchang.timetracker.util.CsvSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ImportResult(val imported: Int, val skipped: Int)

sealed class SettingsEvent {
    data class ImportDone(val result: ImportResult) : SettingsEvent()
    data object ImportFailed : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val timeRecordRepository: TimeRecordRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                category.copy(
                    name = newName,
                    colorHex = newColorHex
                )
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    suspend fun generateCsvContent(): String {
        val records = timeRecordRepository.getAllRecords().first()
        val categoryMap = categoryRepository.getAllCategories().first().associateBy { it.id }
        return CsvSerializer.buildCsvContent(records, categoryMap)
    }

    fun saveCsvToUri(context: Context, uri: Uri, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // write UTF-8 BOM
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── CSV Import ────────────────────────────────────────────────────────

    fun importCsvFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val result = parseCsvAndInsert(context, uri)
                _events.emit(SettingsEvent.ImportDone(result))
            } catch (e: Exception) {
                e.printStackTrace()
                _events.emit(SettingsEvent.ImportFailed)
            }
        }
    }

    private suspend fun parseCsvAndInsert(context: Context, uri: Uri): ImportResult =
        withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext ImportResult(0, 0)

            val content = CsvSerializer.stripBom(bytes)
            val rows = CsvSerializer.parseContent(content)
            if (rows.isEmpty()) return@withContext ImportResult(0, 0)

            // Pre-load existing categories and tags to avoid duplicate inserts
            val categoryCache = categoryRepository.getAllCategories().first()
                .associateBy { it.name }.toMutableMap<String, Category>()
            val tagCache = tagRepository.getAllTags().first()
                .associateBy { it.name }.toMutableMap<String, Tag>()

            var imported = 0
            var skipped = 0

            for (fields in rows) {
                try {
                    if (fields.size < CsvSerializer.Col.MIN_FIELDS) { skipped++; continue }

                    // Find or create category
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

                    // Find or create tags
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
