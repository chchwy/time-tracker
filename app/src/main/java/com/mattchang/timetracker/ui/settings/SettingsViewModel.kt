package com.mattchang.timetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import android.net.Uri
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val timeRecordRepository: TimeRecordRepository
) : ViewModel() {

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
}
