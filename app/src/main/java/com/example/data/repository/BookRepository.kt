package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.BookDao
import com.example.data.reader.BookContentEngine
import com.example.data.scanner.StorageScanner
import com.example.model.BookEntity
import com.example.model.BookmarkEntity
import com.example.model.ComicFolder
import com.example.model.ReadingStatsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val contentEngine: BookContentEngine,
    val storageScanner: StorageScanner,
    private val scope: CoroutineScope
) {
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val recentBooks: Flow<List<BookEntity>> = bookDao.getRecentBooks()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookDao.getAllBookmarks()
    val readingStats: Flow<ReadingStatsEntity?> = bookDao.getReadingStats()

    val folders: Flow<List<ComicFolder>> = bookDao.getAllBooks().map { books ->
        books.groupBy { it.folderPath }.map { (path, bookList) ->
            val folderName = bookList.firstOrNull()?.folderName ?: path.substringAfterLast('/', "Folder")
            val formats = bookList.map { it.format }.distinct()
            val totalSize = bookList.sumOf { it.fileSizeBytes }
            ComicFolder(
                folderPath = path,
                folderName = folderName,
                bookCount = bookList.size,
                formats = formats,
                previewBook = bookList.firstOrNull(),
                totalSizeBytes = totalSize
            )
        }.sortedByDescending { it.bookCount }
    }

    init {
        scope.launch(Dispatchers.IO) {
            // Purge any mock/sample items if present from past runs
            bookDao.purgeSampleBooks()
        }
    }

    fun getBooksInFolder(folderPath: String): Flow<List<BookEntity>> {
        return bookDao.getBooksInFolder(folderPath)
    }

    suspend fun updateReadingProgress(bookId: String, newPage: Int, totalPages: Int) = withContext(Dispatchers.IO) {
        val safeTotal = totalPages.coerceAtLeast(1)
        val safePage = newPage.coerceIn(0, safeTotal - 1)
        val percent = ((safePage + 1).toFloat() / safeTotal.toFloat()) * 100f
        val timestamp = System.currentTimeMillis()

        bookDao.updateProgress(bookId, safePage, percent, timestamp)

        // Update streak & reading stats locally
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentStats = bookDao.getReadingStats().firstOrNull() ?: ReadingStatsEntity()

        val newStreak = if (currentStats.lastReadDate == todayDate) {
            currentStats.streakDays
        } else {
            currentStats.streakDays + 1
        }

        val updatedStats = currentStats.copy(
            totalMinutesRead = currentStats.totalMinutesRead + 1,
            pagesCompleted = currentStats.pagesCompleted + 1,
            streakDays = newStreak,
            lastReadDate = todayDate
        )
        bookDao.insertOrUpdateStats(updatedStats)
    }

    suspend fun toggleFavorite(bookId: String, currentFavorite: Boolean) = withContext(Dispatchers.IO) {
        bookDao.toggleFavorite(bookId, !currentFavorite)
    }

    suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.deleteBook(book)
    }

    suspend fun moveBooksToFolder(bookIds: List<String>, newFolderName: String, newFolderPath: String) = withContext(Dispatchers.IO) {
        bookDao.moveBooksToFolder(bookIds, newFolderName, newFolderPath)
    }

    suspend fun importFile(uri: Uri, fileName: String): BookEntity? = withContext(Dispatchers.IO) {
        val imported = contentEngine.importFileFromUri(uri, fileName) ?: return@withContext null
        bookDao.insertOrUpdateBook(imported)
        return@withContext imported
    }

    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>> {
        return bookDao.getBookmarksForBook(bookId)
    }

    suspend fun addBookmark(bookId: String, bookTitle: String, pageIndex: Int, title: String, note: String) = withContext(Dispatchers.IO) {
        val bookmark = BookmarkEntity(
            id = "bm_${System.currentTimeMillis()}",
            bookId = bookId,
            bookTitle = bookTitle,
            pageIndex = pageIndex,
            title = title.ifEmpty { "Page ${pageIndex + 1}" },
            note = note,
            createdAt = System.currentTimeMillis()
        )
        bookDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmarkId: String) = withContext(Dispatchers.IO) {
        bookDao.deleteBookmark(bookmarkId)
    }

    suspend fun triggerScan() {
        storageScanner.scanDeviceStorage()
    }

    suspend fun scanSafFolder(uri: Uri) {
        storageScanner.scanSafTreeUri(uri)
    }
}
