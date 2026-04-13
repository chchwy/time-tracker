package com.mattchang.timetracker.ui.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import com.mattchang.timetracker.domain.model.Category
import com.mattchang.timetracker.domain.model.RecordType
import com.mattchang.timetracker.domain.model.Tag
import com.mattchang.timetracker.domain.model.TimeRecord
import com.mattchang.timetracker.domain.repository.CategoryRepository
import com.mattchang.timetracker.domain.repository.TagRepository
import com.mattchang.timetracker.domain.repository.TimeRecordRepository
import com.mattchang.timetracker.util.CsvSerializer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    // ── Test dispatcher ───────────────────────────────────────────────────

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    // ── Mocks ─────────────────────────────────────────────────────────────

    private val categoryRepo = mockk<CategoryRepository>()
    private val recordRepo   = mockk<TimeRecordRepository>()
    private val tagRepo      = mockk<TagRepository>()

    private fun viewModel() = SettingsViewModel(categoryRepo, recordRepo, tagRepo)

    // ── Fixtures ──────────────────────────────────────────────────────────

    private val start = LocalDateTime.of(2024, 3, 15, 22, 30, 0)
    private val end   = LocalDateTime.of(2024, 3, 16,  7,  0, 0)

    private val workCategory = Category(id = 1L, name = "Work", colorHex = "#FF0000")

    private val sampleRecord = TimeRecord(
        id = 1L,
        type = RecordType.NORMAL,
        categoryId = 1L,
        title = "Deep work",
        startTime = start,
        endTime = end,
        durationMinutes = 510,
        tags = listOf(Tag(1L, "focus"))
    )

    /** Build a minimal valid CSV with one record row, then encode to bytes (with BOM). */
    private fun buildCsvBytes(records: List<TimeRecord>, catMap: Map<Long, Category>): ByteArray {
        val content = CsvSerializer.buildCsvContent(records, catMap)
        return byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                content.toByteArray(Charsets.UTF_8)
    }

    private fun mockContext(bytes: ByteArray): Pair<Context, Uri> {
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val ctx = mockk<Context>()
        every { ctx.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(bytes)
        return ctx to uri
    }

    // ── generateCsvContent ────────────────────────────────────────────────

    @Test
    fun `generateCsvContent - with records - returns csv starting with header`() = runTest {
        every { recordRepo.getAllRecords() } returns flowOf(listOf(sampleRecord))
        every { categoryRepo.getAllCategories() } returns flowOf(listOf(workCategory))

        val csv = viewModel().generateCsvContent()

        assertTrue(csv.startsWith(CsvSerializer.HEADER))
    }

    @Test
    fun `generateCsvContent - record with category - category name in output`() = runTest {
        every { recordRepo.getAllRecords() } returns flowOf(listOf(sampleRecord))
        every { categoryRepo.getAllCategories() } returns flowOf(listOf(workCategory))

        val csv = viewModel().generateCsvContent()
        val dataRow = csv.trimEnd().lines()[1]
        val fields = CsvSerializer.parseCsvRow(dataRow)

        assertEquals("Work", fields[CsvSerializer.Col.CATEGORY])
        assertEquals("Deep work", fields[CsvSerializer.Col.TITLE])
        assertEquals("focus", fields[CsvSerializer.Col.TAGS])
    }

    @Test
    fun `generateCsvContent - empty records - returns header only`() = runTest {
        every { recordRepo.getAllRecords() } returns flowOf(emptyList())
        every { categoryRepo.getAllCategories() } returns flowOf(emptyList())

        val csv = viewModel().generateCsvContent()
        assertEquals(1, csv.trimEnd().lines().size)
    }

    // ── importCsvFromUri ──────────────────────────────────────────────────

    @Test
    fun `importCsvFromUri - valid csv - emits ImportDone with correct imported count`() = runTest {
        val bytes = buildCsvBytes(listOf(sampleRecord), mapOf(1L to workCategory))
        val (ctx, uri) = mockContext(bytes)

        every  { categoryRepo.getAllCategories() } returns flowOf(listOf(workCategory))
        every  { tagRepo.getAllTags() } returns flowOf(listOf(Tag(1L, "focus")))
        coEvery { recordRepo.insertRecord(any(), any()) } returns 99L

        val vm = viewModel()
        vm.events.test {
            vm.importCsvFromUri(ctx, uri)
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ImportDone)
            assertEquals(1, (event as SettingsEvent.ImportDone).result.imported)
            assertEquals(0, event.result.skipped)
        }
    }

    @Test
    fun `importCsvFromUri - csv with utf-8 bom - parses correctly`() = runTest {
        // Use a tag-free record so we only test BOM stripping, not tag creation
        val noTagRecord = sampleRecord.copy(tags = emptyList())
        val bytes = buildCsvBytes(listOf(noTagRecord), mapOf(1L to workCategory))
        assertTrue("test setup: BOM present", bytes[0] == 0xEF.toByte())

        val (ctx, uri) = mockContext(bytes)
        every  { categoryRepo.getAllCategories() } returns flowOf(listOf(workCategory))
        every  { tagRepo.getAllTags() } returns flowOf(emptyList())
        coEvery { recordRepo.insertRecord(any(), any()) } returns 1L

        val vm = viewModel()
        vm.events.test {
            vm.importCsvFromUri(ctx, uri)
            val event = awaitItem() as SettingsEvent.ImportDone
            assertEquals(1, event.result.imported)
        }
    }

    @Test
    fun `importCsvFromUri - csv with malformed rows - skips bad rows, counts skipped`() = runTest {
        val goodRow = CsvSerializer.buildCsvContent(listOf(sampleRecord), mapOf(1L to workCategory))
            .trimEnd().lines()[1]
        // A bad row has too few columns
        val badRow = "1,NORMAL"
        val content = "${CsvSerializer.HEADER}\n$goodRow\n$badRow\n"
        val bytes = content.toByteArray(Charsets.UTF_8)
        val (ctx, uri) = mockContext(bytes)

        every  { categoryRepo.getAllCategories() } returns flowOf(listOf(workCategory))
        every  { tagRepo.getAllTags() } returns flowOf(listOf(Tag(1L, "focus")))
        coEvery { recordRepo.insertRecord(any(), any()) } returns 1L

        val vm = viewModel()
        vm.events.test {
            vm.importCsvFromUri(ctx, uri)
            val event = awaitItem() as SettingsEvent.ImportDone
            assertEquals(1, event.result.imported)
            assertEquals(1, event.result.skipped)
        }
    }

    @Test
    fun `importCsvFromUri - io error reading uri - emits ImportFailed`() = runTest {
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val ctx = mockk<Context>()
        every { ctx.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } throws RuntimeException("disk error")

        every { categoryRepo.getAllCategories() } returns flowOf(emptyList())
        every { tagRepo.getAllTags() } returns flowOf(emptyList())

        val vm = viewModel()
        vm.events.test {
            vm.importCsvFromUri(ctx, uri)
            assertTrue(awaitItem() is SettingsEvent.ImportFailed)
        }
    }

    @Test
    fun `importCsvFromUri - category not in db - creates new category`() = runTest {
        val bytes = buildCsvBytes(listOf(sampleRecord), mapOf(1L to workCategory))
        val (ctx, uri) = mockContext(bytes)

        // No existing categories in DB
        every  { categoryRepo.getAllCategories() } returns flowOf(emptyList())
        every  { tagRepo.getAllTags() } returns flowOf(listOf(Tag(1L, "focus")))
        coEvery { categoryRepo.insertCategory(any()) } returns 100L
        coEvery { recordRepo.insertRecord(any(), any()) } returns 1L

        val vm = viewModel()
        vm.events.test {
            vm.importCsvFromUri(ctx, uri)
            awaitItem()
        }

        coVerify(exactly = 1) { categoryRepo.insertCategory(match { it.name == "Work" }) }
    }

    @Test
    fun `importCsvFromUri - category already in db - reuses existing without inserting`() = runTest {
        val bytes = buildCsvBytes(listOf(sampleRecord), mapOf(1L to workCategory))
        val (ctx, uri) = mockContext(bytes)

        // Category already exists
        every  { categoryRepo.getAllCategories() } returns flowOf(listOf(workCategory))
        every  { tagRepo.getAllTags() } returns flowOf(listOf(Tag(1L, "focus")))
        coEvery { recordRepo.insertRecord(any(), any()) } returns 1L

        val vm = viewModel()
        vm.events.test {
            vm.importCsvFromUri(ctx, uri)
            awaitItem()
        }

        coVerify(exactly = 0) { categoryRepo.insertCategory(any()) }
    }

    @Test
    fun `importCsvFromUri - tag not in db - creates new tag`() = runTest {
        val bytes = buildCsvBytes(listOf(sampleRecord), mapOf(1L to workCategory))
        val (ctx, uri) = mockContext(bytes)

        every  { categoryRepo.getAllCategories() } returns flowOf(listOf(workCategory))
        every  { tagRepo.getAllTags() } returns flowOf(emptyList())   // no existing tags
        coEvery { tagRepo.insertTag(any()) } returns 50L
        coEvery { recordRepo.insertRecord(any(), any()) } returns 1L

        val vm = viewModel()
        vm.events.test {
            vm.importCsvFromUri(ctx, uri)
            awaitItem()
        }

        coVerify(exactly = 1) { tagRepo.insertTag(match { it.name == "focus" }) }
    }

    @Test
    fun `importCsvFromUri - tag already in db - reuses existing without inserting`() = runTest {
        val bytes = buildCsvBytes(listOf(sampleRecord), mapOf(1L to workCategory))
        val (ctx, uri) = mockContext(bytes)

        every  { categoryRepo.getAllCategories() } returns flowOf(listOf(workCategory))
        every  { tagRepo.getAllTags() } returns flowOf(listOf(Tag(1L, "focus")))  // already exists
        coEvery { recordRepo.insertRecord(any(), any()) } returns 1L

        val vm = viewModel()
        vm.events.test {
            vm.importCsvFromUri(ctx, uri)
            awaitItem()
        }

        coVerify(exactly = 0) { tagRepo.insertTag(any()) }
    }

    @Test
    fun `importCsvFromUri - multiple records - all inserted and count matches`() = runTest {
        val r2 = sampleRecord.copy(id = 2L, title = "Code review", tags = emptyList())
        val bytes = buildCsvBytes(
            listOf(sampleRecord, r2),
            mapOf(1L to workCategory)
        )
        val (ctx, uri) = mockContext(bytes)

        every  { categoryRepo.getAllCategories() } returns flowOf(listOf(workCategory))
        every  { tagRepo.getAllTags() } returns flowOf(listOf(Tag(1L, "focus")))
        coEvery { recordRepo.insertRecord(any(), any()) } returns 1L

        val vm = viewModel()
        vm.events.test {
            vm.importCsvFromUri(ctx, uri)
            val event = awaitItem() as SettingsEvent.ImportDone
            assertEquals(2, event.result.imported)
            assertEquals(0, event.result.skipped)
        }
    }
}
