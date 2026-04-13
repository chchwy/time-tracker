package com.mattchang.timetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.RecordType
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.model.TimeRecord
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

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
        val cats = categoryRepository.getAllCategories().first().associateBy { it.id }
        
        val sb = StringBuilder()
        sb.append("ID,Type,Category,Title,StartTime,EndTime,DurationMinutes,Note,Tags,IsSleep,MorningEnergy,ReadBook,UsedComputer,ChattedWithWife,ChildInterrupted,StayUpLateReason\n")
        
        records.forEach { r ->
            val catName = r.categoryId?.let { cats[it]?.name } ?: ""
            val tags = r.tags.joinToString(";") { it.name }.replace("\"", "\"\"")
            val title = (r.title ?: "").replace("\"", "\"\"")
            val note = (r.note ?: "").replace("\"", "\"\"")
            val reason = (r.stayUpLateReason ?: "").replace("\"", "\"\"")
            
            sb.append("${r.id},")
            sb.append("${r.type},")
            sb.append("\"${catName}\",")
            sb.append("\"${title}\",")
            sb.append("${r.startTime},")
            sb.append("${r.endTime},")
            sb.append("${r.durationMinutes},")
            sb.append("\"${note}\",")
            sb.append("\"${tags}\",")
            sb.append("${r.isSleep},")
            sb.append("${r.morningEnergyIndex ?: ""},")
            sb.append("${r.readBookBeforeBed ?: ""},")
            sb.append("${r.usedComputerBeforeBed ?: ""},")
            sb.append("${r.chattedWithWife ?: ""},")
            sb.append("${r.childInterrupted ?: ""},")
            sb.append("\"${reason}\"\n")
        }
        
        return sb.toString()
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

            // Strip UTF-8 BOM if present
            val content = if (bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte()
            ) {
                String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
            } else {
                String(bytes, Charsets.UTF_8)
            }

            val lines = content.lines()
            if (lines.size < 2) return@withContext ImportResult(0, 0)

            // Pre-load existing categories and tags to avoid duplicate inserts
            val categoryCache = categoryRepository.getAllCategories().first()
                .associateBy { it.name }.toMutableMap<String, Category>()
            val tagCache = tagRepository.getAllTags().first()
                .associateBy { it.name }.toMutableMap<String, Tag>()

            var imported = 0
            var skipped = 0

            // Skip header row (index 0)
            for (line in lines.drop(1)) {
                if (line.isBlank()) continue
                try {
                    val f = parseCsvRow(line)
                    // Columns: ID,Type,Category,Title,StartTime,EndTime,
                    //          DurationMinutes,Note,Tags,IsSleep,MorningEnergy,
                    //          ReadBook,UsedComputer,ChattedWithWife,
                    //          ChildInterrupted,StayUpLateReason
                    if (f.size < 16) { skipped++; continue }

                    val type = runCatching { RecordType.valueOf(f[1].trim()) }
                        .getOrDefault(RecordType.NORMAL)
                    val startTime = LocalDateTime.parse(f[4].trim())
                    val endTime = LocalDateTime.parse(f[5].trim())
                    val durationMinutes = f[6].trim().toIntOrNull() ?: 0

                    // Find or create category
                    val categoryId: Long? = f[2].trim().let { name ->
                        if (name.isBlank()) return@let null
                        categoryCache[name]?.id ?: run {
                            val newId = categoryRepository.insertCategory(
                                Category(
                                    name = name, icon = "circle",
                                    colorHex = "#9E9E9E", isDefault = false, sortOrder = 99
                                )
                            )
                            val newCat = Category(
                                id = newId, name = name, icon = "circle",
                                colorHex = "#9E9E9E", isDefault = false, sortOrder = 99
                            )
                            categoryCache[name] = newCat
                            newId
                        }
                    }

                    // Find or create tags
                    val tagIds: List<Long> = f[8].trim()
                        .split(";")
                        .mapNotNull { it.trim().ifBlank { null } }
                        .map { tagName ->
                            tagCache[tagName]?.id ?: run {
                                val newId = tagRepository.insertTag(Tag(name = tagName))
                                tagCache[tagName] = Tag(id = newId, name = tagName)
                                newId
                            }
                        }

                    val record = TimeRecord(
                        id = 0, // always insert as new record
                        type = type,
                        categoryId = categoryId,
                        title = f[3].trim().ifBlank { null },
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = durationMinutes,
                        note = f[7].trim().ifBlank { null },
                        morningEnergyIndex = f[10].trim().toIntOrNull(),
                        readBookBeforeBed = f[11].trim().toBooleanStrictOrNull(),
                        usedComputerBeforeBed = f[12].trim().toBooleanStrictOrNull(),
                        chattedWithWife = f[13].trim().toBooleanStrictOrNull(),
                        childInterrupted = f[14].trim().toBooleanStrictOrNull(),
                        stayUpLateReason = f[15].trim().ifBlank { null }
                    )

                    timeRecordRepository.insertRecord(record, tagIds)
                    imported++
                } catch (e: Exception) {
                    skipped++
                }
            }

            ImportResult(imported, skipped)
        }

    /**
     * Parse a single CSV row, respecting quoted fields and escaped double-quotes ("").
     */
    private fun parseCsvRow(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++ // skip the second quote of the escape pair
                }
                c == '"' && inQuotes -> inQuotes = false
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
