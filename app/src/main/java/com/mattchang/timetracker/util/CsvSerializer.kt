package com.mattchang.timetracker.util

import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.RecordType
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.model.TimeRecord
import java.time.LocalDateTime

/**
 * Pure CSV serialization/deserialization logic with no Android or coroutine dependencies,
 * making it straightforward to unit test.
 */
object CsvSerializer {

    const val HEADER =
        "ID,Type,Category,Title,StartTime,EndTime,DurationMinutes,Note,Tags," +
        "IsSleep,MorningEnergy,ReadBook,UsedComputer,ChattedWithWife,ChildInterrupted,StayUpLateReason,BookTitleBeforeBed"

    // ── Export ────────────────────────────────────────────────────────────

    /**
     * Build a complete CSV string (header + data rows) from a list of records.
     * [categoryMap] maps category IDs to Category objects for name resolution.
     */
    fun buildCsvContent(
        records: List<TimeRecord>,
        categoryMap: Map<Long, Category>
    ): String {
        val sb = StringBuilder()
        sb.append(HEADER).append('\n')
        records.forEach { r ->
            val catName = r.categoryId?.let { categoryMap[it]?.name } ?: ""
            val tags = r.tags.joinToString(";") { it.name }.csvEscape()
            val title = (r.title ?: "").csvEscape()
            val note = (r.note ?: "").csvEscape()
            val reason = (r.stayUpLateReason ?: "").csvEscape()
            val bookTitleBeforeBed = (r.bookTitleBeforeBed ?: "").csvEscape()

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
            sb.append("\"${reason}\",")
            sb.append("\"${bookTitleBeforeBed}\"\n")
        }
        return sb.toString()
    }

    private fun String.csvEscape() = replace("\"", "\"\"")

    // ── Import ────────────────────────────────────────────────────────────

    /**
     * Strip a UTF-8 BOM from raw bytes (written by Excel and our own export) and
     * return the decoded string.
     */
    fun stripBom(bytes: ByteArray): String {
        return if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        } else {
            String(bytes, Charsets.UTF_8)
        }
    }

    /**
     * Parse the full CSV content string into a list of field lists, skipping the
     * header row and blank lines.  Throws nothing — callers should catch per-row.
     */
    fun parseContent(content: String): List<List<String>> =
        content.lines()
            .drop(1)                     // skip header
            .filter { it.isNotBlank() }
            .map { parseCsvRow(it) }

    /**
     * Parse a single CSV row respecting RFC-4180 quoting:
     *  - Fields may be enclosed in double-quotes.
     *  - A double-quote inside a quoted field is escaped as "".
     */
    fun parseCsvRow(line: String): List<String> {
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

    // ── Row → domain helpers (shared with ViewModel) ──────────────────────

    /**
     * Column indices matching [HEADER].
     */
    object Col {
        const val ID = 0
        const val TYPE = 1
        const val CATEGORY = 2
        const val TITLE = 3
        const val START_TIME = 4
        const val END_TIME = 5
        const val DURATION = 6
        const val NOTE = 7
        const val TAGS = 8
        const val IS_SLEEP = 9
        const val MORNING_ENERGY = 10
        const val READ_BOOK = 11
        const val USED_COMPUTER = 12
        const val CHATTED = 13
        const val CHILD_INTERRUPTED = 14
        const val STAY_UP_REASON = 15
        const val BOOK_TITLE_BEFORE_BED = 16
        const val MIN_FIELDS = 16
    }

    /**
     * Convert a parsed field list into a partial [TimeRecord] (id=0, tags empty).
     * Category and tag resolution is the caller's responsibility.
     * Returns null if the row is malformed.
     */
    fun rowToRecord(fields: List<String>, categoryId: Long?): TimeRecord? {
        if (fields.size < Col.MIN_FIELDS) return null
        return runCatching {
            TimeRecord(
                id = 0,
                type = runCatching { RecordType.valueOf(fields[Col.TYPE].trim()) }
                    .getOrDefault(RecordType.NORMAL),
                categoryId = categoryId,
                title = fields[Col.TITLE].trim().ifBlank { null },
                startTime = LocalDateTime.parse(fields[Col.START_TIME].trim()),
                endTime = LocalDateTime.parse(fields[Col.END_TIME].trim()),
                durationMinutes = fields[Col.DURATION].trim().toIntOrNull() ?: 0,
                note = fields[Col.NOTE].trim().ifBlank { null },
                morningEnergyIndex = fields[Col.MORNING_ENERGY].trim().toIntOrNull(),
                readBookBeforeBed = fields[Col.READ_BOOK].trim().toBooleanStrictOrNull(),
                usedComputerBeforeBed = fields[Col.USED_COMPUTER].trim().toBooleanStrictOrNull(),
                chattedWithWife = fields[Col.CHATTED].trim().toBooleanStrictOrNull(),
                childInterrupted = fields[Col.CHILD_INTERRUPTED].trim().toBooleanStrictOrNull(),
                stayUpLateReason = fields[Col.STAY_UP_REASON].trim().ifBlank { null },
                bookTitleBeforeBed = fields.getOrNull(Col.BOOK_TITLE_BEFORE_BED)?.trim()?.ifBlank { null }
            )
        }.getOrNull()
    }

    /** Split a semicolon-joined tag string into individual tag names. */
    fun parseTagNames(tagsField: String): List<String> =
        tagsField.split(";").map { it.trim() }.filter { it.isNotBlank() }
}
