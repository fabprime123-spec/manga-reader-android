package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BookFormat(val extension: String, val badgeColorHex: Long) {
    CBZ("cbz", 0xFFE50914),
    CBR("cbr", 0xFFD81B60),
    PDF("pdf", 0xFFD32F2F),
    ZIP("zip", 0xFF8E24AA),
    EPUB("epub", 0xFF3949AB),
    TXT("txt", 0xFF455A64),
    MANGA("manga", 0xFFE50914)
}

enum class ReaderFont(val displayName: String) {
    GOTHIC("Gothic Fraktur"),
    SERIF("Classic Serif"),
    SANS("Modern Sans"),
    MONO("Terminal Mono"),
    CURSIVE("Script / Cursive")
}

enum class ReaderThemeMode(val displayName: String) {
    VOID_DARK("Death Note Void"),
    OLED_BLACK("Pure OLED Black"),
    RULES_PARCHMENT("Rules Parchment"),
    SEPIA_WARM("Sepia Papyrus"),
    CLEAN_LIGHT("Day Light")
}

enum class ReadingDirection(val displayName: String) {
    RIGHT_TO_LEFT("Manga Mode (Right to Left)"),
    LEFT_TO_RIGHT("Standard (Left to Right)"),
    VERTICAL_WEBTOON("Webtoon (Continuous Vertical)")
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String = "Unknown",
    val format: String = BookFormat.CBZ.name,
    val localUri: String? = null,
    val folderPath: String = "Internal",
    val folderName: String = "Internal Shelf",
    val fileSizeBytes: Long = 0L,
    val sampleType: String? = null,
    val coverUrl: String? = null,
    val totalPages: Int = 1,
    val currentPage: Int = 0,
    val progressPercent: Float = 0f,
    val isFavorite: Boolean = false,
    val collectionName: String = "Main Shelf",
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ComicFolder(
    val folderPath: String,
    val folderName: String,
    val bookCount: Int,
    val formats: List<String>,
    val previewBook: BookEntity? = null,
    val totalSizeBytes: Long = 0L
)

data class SeriesGroup(
    val seriesName: String,
    val bookCount: Int,
    val books: List<BookEntity>,
    val formats: List<String>,
    val detectedType: String = "Manga",
    val totalSizeBytes: Long = 0L
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val bookTitle: String = "",
    val pageIndex: Int,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_stats")
data class ReadingStatsEntity(
    @PrimaryKey
    val id: String = "local_reader",
    val totalMinutesRead: Int = 0,
    val pagesCompleted: Int = 0,
    val streakDays: Int = 1,
    val lastReadDate: String = ""
)

data class ReaderSettings(
    val font: ReaderFont = ReaderFont.GOTHIC,
    val fontSize: Int = 18,
    val lineSpacingMultiplier: Float = 1.5f,
    val themeMode: ReaderThemeMode = ReaderThemeMode.VOID_DARK,
    val readingDirection: ReadingDirection = ReadingDirection.RIGHT_TO_LEFT,
    val keepScreenOn: Boolean = true
)
