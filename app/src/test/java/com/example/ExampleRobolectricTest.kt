package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BookDatabase
import com.example.model.BookEntity
import com.example.model.BookFormat
import com.example.model.BookmarkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: BookDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DeathReader", appName)
    }

    @Test
    fun testInsertAndRetrieveBook() = runBlocking {
        val bookDao = db.bookDao()
        val testBook = BookEntity(
            id = "test_book_1",
            title = "Death Note: Pilot Chapter",
            author = "Tsugumi Ohba",
            format = BookFormat.CBZ.name,
            folderPath = "/storage/Comics",
            folderName = "Comics",
            totalPages = 24,
            currentPage = 0,
            progressPercent = 0f
        )
        bookDao.insertOrUpdateBook(testBook)

        val retrieved = bookDao.getBookById("test_book_1")
        assertNotNull(retrieved)
        assertEquals("Death Note: Pilot Chapter", retrieved?.title)
        assertEquals(BookFormat.CBZ.name, retrieved?.format)
        assertEquals("Comics", retrieved?.folderName)
    }

    @Test
    fun testBooksInFolderQuery() = runBlocking {
        val bookDao = db.bookDao()
        val book1 = BookEntity(
            id = "cbz_1",
            title = "Manga Vol 1",
            format = BookFormat.CBZ.name,
            folderPath = "/storage/Manga",
            folderName = "Manga"
        )
        val book2 = BookEntity(
            id = "pdf_1",
            title = "Novel Book",
            format = BookFormat.PDF.name,
            folderPath = "/storage/Novels",
            folderName = "Novels"
        )
        bookDao.insertAll(listOf(book1, book2))

        val mangaBooks = bookDao.getBooksInFolder("/storage/Manga").first()
        assertEquals(1, mangaBooks.size)
        assertEquals("Manga Vol 1", mangaBooks[0].title)
    }

    @Test
    fun testUpdateReadingProgress() = runBlocking {
        val bookDao = db.bookDao()
        val testBook = BookEntity(
            id = "test_progress_book",
            title = "Rules of the Notebook",
            totalPages = 10,
            currentPage = 0,
            progressPercent = 0f
        )
        bookDao.insertOrUpdateBook(testBook)

        bookDao.updateProgress("test_progress_book", 4, 50f, System.currentTimeMillis())

        val updated = bookDao.getBookById("test_progress_book")
        assertEquals(4, updated?.currentPage)
        assertEquals(50f, updated?.progressPercent)
    }

    @Test
    fun testBookmarkOperations() = runBlocking {
        val bookDao = db.bookDao()
        val bookmark = BookmarkEntity(
            id = "bm_test_1",
            bookId = "test_book_1",
            bookTitle = "Death Note",
            pageIndex = 7,
            title = "Rule VII Note",
            note = "How to write names accurately"
        )
        bookDao.insertBookmark(bookmark)

        val bookmarks = bookDao.getBookmarksForBook("test_book_1").first()
        assertEquals(1, bookmarks.size)
        assertEquals("Rule VII Note", bookmarks[0].title)
    }
}
