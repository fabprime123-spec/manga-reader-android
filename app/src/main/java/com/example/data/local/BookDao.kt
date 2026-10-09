package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.BookEntity
import com.example.model.BookmarkEntity
import com.example.model.ReadingStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookByIdFlow(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE folderPath = :folderPath ORDER BY title ASC")
    fun getBooksInFolder(folderPath: String): Flow<List<BookEntity>>

    @Query("SELECT DISTINCT folderPath FROM books WHERE folderPath != ''")
    fun getDistinctFolderPaths(): Flow<List<String>>

    @Query("SELECT * FROM books WHERE currentPage > 0 ORDER BY lastReadTimestamp DESC LIMIT 30")
    fun getRecentBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY lastReadTimestamp DESC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: String)

    @Query("DELETE FROM books WHERE id LIKE 'sample_%' OR sampleType IS NOT NULL")
    suspend fun purgeSampleBooks()

    @Query("UPDATE books SET currentPage = :page, progressPercent = :percent, lastReadTimestamp = :timestamp WHERE id = :bookId")
    suspend fun updateProgress(bookId: String, page: Int, percent: Float, timestamp: Long)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun toggleFavorite(bookId: String, isFavorite: Boolean)

    @Query("UPDATE books SET folderName = :newFolderName, folderPath = :newFolderPath WHERE id IN (:bookIds)")
    suspend fun moveBooksToFolder(bookIds: List<String>, newFolderName: String, newFolderPath: String)

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageIndex ASC")
    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: String)

    // Reading Stats
    @Query("SELECT * FROM reading_stats WHERE id = :id LIMIT 1")
    fun getReadingStats(id: String = "local_reader"): Flow<ReadingStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: ReadingStatsEntity)
}
