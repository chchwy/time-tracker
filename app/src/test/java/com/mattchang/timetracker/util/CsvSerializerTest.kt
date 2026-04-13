package com.mattchang.timetracker.util

import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.RecordType
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.model.TimeRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class CsvSerializerTest {

    // ── Fixtures ──────────────────────────────────────────────────────────

    private val start = LocalDateTime.of(2024, 3, 15, 22, 30, 0)
    private val end   = LocalDateTime.of(2024, 3, 16,  7,  0, 0)

    private val workCategory = Category(id = 1L, name = "Work", colorHex = "#FF0000")

    private val normalRecord = TimeRecord(
        id = 42L,
        type = RecordType.NORMAL,
        categoryId = 1L,
        title = "Deep work",
        startTime = start,
        endTime = end,
        durationMinutes = 510,
        note = "Focused session",
        tags = listOf(Tag(id = 1L, name = "focus"), Tag(id = 2L, name = "coding"))
    )

    private val sleepRecord = TimeRecord(
        id = 7L,
        type = RecordType.SLEEP,
        categoryId = 2L,
        startTime = start,
        endTime = end,
        durationMinutes = 510,
        morningEnergyIndex = 8,
        readBookBeforeBed = true,
        usedComputerBeforeBed = false,
        chattedWithWife = true,
        childInterrupted = false,
        stayUpLateReason = "watched movie"
    )

    // ── buildCsvContent ───────────────────────────────────────────────────

    @Test
    fun `buildCsvContent - empty list - returns header only`() {
        val csv = CsvSerializer.buildCsvContent(emptyList(), emptyMap())
        val lines = csv.trimEnd().lines()
        assertEquals(1, lines.size)
        assertEquals(CsvSerializer.HEADER, lines[0])
    }

    @Test
    fun `buildCsvContent - normal record - correct column count and values`() {
        val csv = CsvSerializer.buildCsvContent(listOf(normalRecord), mapOf(1L to workCategory))
        val lines = csv.trimEnd().lines()
        assertEquals(2, lines.size)

        val fields = CsvSerializer.parseCsvRow(lines[1])
        assertEquals(16, fields.size)
        assertEquals("42", fields[CsvSerializer.Col.ID])
        assertEquals("NORMAL", fields[CsvSerializer.Col.TYPE])
        assertEquals("Work", fields[CsvSerializer.Col.CATEGORY])
        assertEquals("Deep work", fields[CsvSerializer.Col.TITLE])
        assertEquals(start.toString(), fields[CsvSerializer.Col.START_TIME])
        assertEquals(end.toString(), fields[CsvSerializer.Col.END_TIME])
        assertEquals("510", fields[CsvSerializer.Col.DURATION])
        assertEquals("Focused session", fields[CsvSerializer.Col.NOTE])
        assertEquals("focus;coding", fields[CsvSerializer.Col.TAGS])
        assertEquals("false", fields[CsvSerializer.Col.IS_SLEEP])
    }

    @Test
    fun `buildCsvContent - title with comma - is quoted and parseable`() {
        val r = normalRecord.copy(title = "Meeting, standup")
        val csv = CsvSerializer.buildCsvContent(listOf(r), mapOf(1L to workCategory))
        val fields = CsvSerializer.parseCsvRow(csv.trimEnd().lines()[1])
        assertEquals("Meeting, standup", fields[CsvSerializer.Col.TITLE])
    }

    @Test
    fun `buildCsvContent - title with double-quote - is escaped and parseable`() {
        val r = normalRecord.copy(title = "He said \"hello\"")
        val csv = CsvSerializer.buildCsvContent(listOf(r), mapOf(1L to workCategory))
        val fields = CsvSerializer.parseCsvRow(csv.trimEnd().lines()[1])
        assertEquals("He said \"hello\"", fields[CsvSerializer.Col.TITLE])
    }

    @Test
    fun `buildCsvContent - sleep record - sleep fields populated correctly`() {
        val sleepCat = Category(id = 2L, name = "Sleep", colorHex = "#3F51B5")
        val csv = CsvSerializer.buildCsvContent(listOf(sleepRecord), mapOf(2L to sleepCat))
        val fields = CsvSerializer.parseCsvRow(csv.trimEnd().lines()[1])

        assertEquals("SLEEP", fields[CsvSerializer.Col.TYPE])
        assertEquals("true", fields[CsvSerializer.Col.IS_SLEEP])
        assertEquals("8", fields[CsvSerializer.Col.MORNING_ENERGY])
        assertEquals("true", fields[CsvSerializer.Col.READ_BOOK])
        assertEquals("false", fields[CsvSerializer.Col.USED_COMPUTER])
        assertEquals("true", fields[CsvSerializer.Col.CHATTED])
        assertEquals("false", fields[CsvSerializer.Col.CHILD_INTERRUPTED])
        assertEquals("watched movie", fields[CsvSerializer.Col.STAY_UP_REASON])
    }

    @Test
    fun `buildCsvContent - record with no category - empty category field`() {
        val r = normalRecord.copy(categoryId = null)
        val csv = CsvSerializer.buildCsvContent(listOf(r), emptyMap())
        val fields = CsvSerializer.parseCsvRow(csv.trimEnd().lines()[1])
        assertEquals("", fields[CsvSerializer.Col.CATEGORY])
    }

    @Test
    fun `buildCsvContent - multiple tags - semicolon-joined in tags field`() {
        val r = normalRecord.copy(tags = listOf(Tag(1, "a"), Tag(2, "b"), Tag(3, "c")))
        val csv = CsvSerializer.buildCsvContent(listOf(r), mapOf(1L to workCategory))
        val fields = CsvSerializer.parseCsvRow(csv.trimEnd().lines()[1])
        assertEquals("a;b;c", fields[CsvSerializer.Col.TAGS])
    }

    @Test
    fun `buildCsvContent - nullable sleep fields null - empty strings in output`() {
        val r = normalRecord.copy(morningEnergyIndex = null, readBookBeforeBed = null)
        val csv = CsvSerializer.buildCsvContent(listOf(r), mapOf(1L to workCategory))
        val fields = CsvSerializer.parseCsvRow(csv.trimEnd().lines()[1])
        assertEquals("", fields[CsvSerializer.Col.MORNING_ENERGY])
        assertEquals("", fields[CsvSerializer.Col.READ_BOOK])
    }

    // ── parseCsvRow ───────────────────────────────────────────────────────

    @Test
    fun `parseCsvRow - plain values - splits on commas`() {
        val fields = CsvSerializer.parseCsvRow("1,NORMAL,Work,Title,2024-01-01,2024-01-02,60,note,tag,false,,,,,,")
        assertEquals(16, fields.size)
        assertEquals("1", fields[0])
        assertEquals("NORMAL", fields[1])
        assertEquals("Work", fields[2])
    }

    @Test
    fun `parseCsvRow - quoted field with comma inside - treats as single field`() {
        val fields = CsvSerializer.parseCsvRow("1,\"hello, world\",3")
        assertEquals(3, fields.size)
        assertEquals("hello, world", fields[1])
    }

    @Test
    fun `parseCsvRow - escaped double-quote inside quoted field - unescaped`() {
        val fields = CsvSerializer.parseCsvRow("1,\"say \"\"hi\"\"\",3")
        assertEquals(3, fields.size)
        assertEquals("say \"hi\"", fields[1])
    }

    @Test
    fun `parseCsvRow - empty quoted field - returns empty string`() {
        val fields = CsvSerializer.parseCsvRow("1,\"\",3")
        assertEquals(3, fields.size)
        assertEquals("", fields[1])
    }

    @Test
    fun `parseCsvRow - consecutive commas - produces empty strings`() {
        val fields = CsvSerializer.parseCsvRow("a,,c")
        assertEquals(3, fields.size)
        assertEquals("", fields[1])
    }

    // ── stripBom ──────────────────────────────────────────────────────────

    @Test
    fun `stripBom - bytes with UTF-8 BOM - strips BOM and decodes correctly`() {
        val payload = "hello"
        val withBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                payload.toByteArray(Charsets.UTF_8)
        assertEquals(payload, CsvSerializer.stripBom(withBom))
    }

    @Test
    fun `stripBom - bytes without BOM - decodes as-is`() {
        val payload = "hello"
        assertEquals(payload, CsvSerializer.stripBom(payload.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `stripBom - empty bytes - returns empty string`() {
        assertEquals("", CsvSerializer.stripBom(ByteArray(0)))
    }

    // ── parseContent ─────────────────────────────────────────────────────

    @Test
    fun `parseContent - skips header row and parses data rows`() {
        val content = "${CsvSerializer.HEADER}\n1,NORMAL,Work,Title,$start,$end,60,,,false,,,,,,\n"
        val rows = CsvSerializer.parseContent(content)
        assertEquals(1, rows.size)
        assertEquals("1", rows[0][0])
    }

    @Test
    fun `parseContent - blank lines skipped`() {
        val content = "${CsvSerializer.HEADER}\n\n1,NORMAL,,,${start},${end},60,,,false,,,,,,\n\n"
        val rows = CsvSerializer.parseContent(content)
        assertEquals(1, rows.size)
    }

    @Test
    fun `parseContent - empty content - returns empty list`() {
        assertTrue(CsvSerializer.parseContent("").isEmpty())
    }

    @Test
    fun `parseContent - header only - returns empty list`() {
        assertTrue(CsvSerializer.parseContent(CsvSerializer.HEADER).isEmpty())
    }

    // ── parseTagNames ─────────────────────────────────────────────────────

    @Test
    fun `parseTagNames - semicolon-separated names - splits correctly`() {
        assertEquals(listOf("focus", "coding"), CsvSerializer.parseTagNames("focus;coding"))
    }

    @Test
    fun `parseTagNames - empty string - returns empty list`() {
        assertTrue(CsvSerializer.parseTagNames("").isEmpty())
    }

    @Test
    fun `parseTagNames - trailing semicolon - ignores empty segment`() {
        assertEquals(listOf("a", "b"), CsvSerializer.parseTagNames("a;b;"))
    }

    // ── rowToRecord ───────────────────────────────────────────────────────

    @Test
    fun `rowToRecord - valid fields - returns record with correct values`() {
        val fields = listOf(
            "0", "NORMAL", "Work", "My title",
            start.toString(), end.toString(), "510",
            "A note", "tag1", "false",
            "", "", "", "", "", ""
        )
        val record = CsvSerializer.rowToRecord(fields, categoryId = 1L)!!
        assertEquals(RecordType.NORMAL, record.type)
        assertEquals(1L, record.categoryId)
        assertEquals("My title", record.title)
        assertEquals(start, record.startTime)
        assertEquals(end, record.endTime)
        assertEquals(510, record.durationMinutes)
        assertEquals("A note", record.note)
    }

    @Test
    fun `rowToRecord - sleep type with energy - sleep fields parsed`() {
        val fields = listOf(
            "0", "SLEEP", "Sleep", "",
            start.toString(), end.toString(), "510",
            "", "", "true",
            "8", "true", "false", "true", "false", "stayed up late"
        )
        val record = CsvSerializer.rowToRecord(fields, categoryId = 2L)!!
        assertEquals(RecordType.SLEEP, record.type)
        assertEquals(8, record.morningEnergyIndex)
        assertEquals(true, record.readBookBeforeBed)
        assertEquals(false, record.usedComputerBeforeBed)
        assertEquals(true, record.chattedWithWife)
        assertEquals(false, record.childInterrupted)
        assertEquals("stayed up late", record.stayUpLateReason)
    }

    @Test
    fun `rowToRecord - too few fields - returns null`() {
        assertNull(CsvSerializer.rowToRecord(listOf("1", "NORMAL"), categoryId = null))
    }

    @Test
    fun `rowToRecord - invalid datetime - returns null`() {
        val fields = listOf(
            "0", "NORMAL", "", "",
            "not-a-date", end.toString(), "60",
            "", "", "false",
            "", "", "", "", "", ""
        )
        assertNull(CsvSerializer.rowToRecord(fields, categoryId = null))
    }

    // ── Round-trip ────────────────────────────────────────────────────────

    @Test
    fun `round-trip - export then re-parse gives back same field values`() {
        val records = listOf(normalRecord, sleepRecord.copy(categoryId = null))
        val catMap = mapOf(1L to workCategory)

        val csv = CsvSerializer.buildCsvContent(records, catMap)
        val rows = CsvSerializer.parseContent(csv)

        assertEquals(2, rows.size)

        // Normal record
        val r0 = rows[0]
        assertEquals("42", r0[CsvSerializer.Col.ID])
        assertEquals("Work", r0[CsvSerializer.Col.CATEGORY])
        assertEquals("Deep work", r0[CsvSerializer.Col.TITLE])
        assertEquals("focus;coding", r0[CsvSerializer.Col.TAGS])

        // Sleep record (no category)
        val r1 = rows[1]
        assertEquals("SLEEP", r1[CsvSerializer.Col.TYPE])
        assertEquals("", r1[CsvSerializer.Col.CATEGORY])
        assertEquals("8", r1[CsvSerializer.Col.MORNING_ENERGY])
        assertEquals("watched movie", r1[CsvSerializer.Col.STAY_UP_REASON])
    }
}
