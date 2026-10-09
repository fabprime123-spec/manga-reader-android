package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BookRepository
import com.example.model.BookEntity
import com.example.model.BookmarkEntity
import com.example.model.ComicFolder
import com.example.model.ReadingStatsEntity
import com.example.model.SeriesGroup
import com.example.ui.theme.SaturatedAccent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class LibraryFilter(val label: String) {
    ALL("All"),
    MANGA("Manga"),
    COMIC("Comic"),
    LIGHT_NOVEL("light novel")
}

enum class DisplayMode {
    GRID,
    LIST
}

enum class BookSortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    RECENT_READ("Recently Read"),
    SIZE_DESC("File Size")
}

class LibraryViewModel(
    private val repository: BookRepository
) : ViewModel() {

    val isScanning: StateFlow<Boolean> = repository.storageScanner.isScanning
    val scannedCount: StateFlow<Int> = repository.storageScanner.scannedCount

    private val _activeAccent = MutableStateFlow(SaturatedAccent.CRIMSON_SHINIGAMI)
    val activeAccent: StateFlow<SaturatedAccent> = _activeAccent.asStateFlow()

    private val _storagePermissionGranted = MutableStateFlow(false)
    val storagePermissionGranted: StateFlow<Boolean> = _storagePermissionGranted.asStateFlow()

    val folders: StateFlow<List<ComicFolder>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentBooks: StateFlow<List<BookEntity>> = repository.recentBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingStats: StateFlow<ReadingStatsEntity?> = repository.readingStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private val _displayMode = MutableStateFlow(DisplayMode.GRID)
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    private val _sortOption = MutableStateFlow(BookSortOption.TITLE_ASC)
    val sortOption: StateFlow<BookSortOption> = _sortOption.asStateFlow()

    // Smart Series Grouping: compares manga, comics, novels and groups by detected series name & volume
    val seriesGroups: StateFlow<List<SeriesGroup>> = repository.allBooks.map { bookList ->
        extractSmartSeries(bookList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val books: StateFlow<List<BookEntity>> = combine(
        repository.allBooks,
        _searchQuery,
        _selectedFilter,
        _selectedFolder,
        _sortOption
    ) { bookList, query, filter, folder, sort ->
        val filtered = bookList.filter { book ->
            val matchesFolder = folder == null || book.folderPath == folder

            val matchesQuery = query.isBlank() ||
                book.title.contains(query, ignoreCase = true) ||
                book.author.contains(query, ignoreCase = true) ||
                book.format.contains(query, ignoreCase = true) ||
                book.folderName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.MANGA -> {
                    val fmt = book.format.uppercase(Locale.ROOT)
                    fmt == "CBZ" || fmt == "MANGA" || fmt == "ZIP" ||
                        book.title.contains("manga", ignoreCase = true) ||
                        book.title.contains("chapter", ignoreCase = true) ||
                        book.title.contains("ch.", ignoreCase = true)
                }
                LibraryFilter.COMIC -> {
                    val fmt = book.format.uppercase(Locale.ROOT)
                    fmt == "CBR" || fmt == "PDF" ||
                        book.title.contains("comic", ignoreCase = true) ||
                        book.title.contains("issue", ignoreCase = true)
                }
                LibraryFilter.LIGHT_NOVEL -> {
                    val fmt = book.format.uppercase(Locale.ROOT)
                    fmt == "TXT" || fmt == "EPUB" ||
                        book.title.contains("novel", ignoreCase = true) ||
                        book.title.contains("vol", ignoreCase = true)
                }
            }

            matchesFolder && matchesQuery && matchesFilter
        }

        when (sort) {
            BookSortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            BookSortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            BookSortOption.RECENT_READ -> filtered.sortedByDescending { it.lastReadTimestamp }
            BookSortOption.SIZE_DESC -> filtered.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAccent(accent: SaturatedAccent) {
        _activeAccent.value = accent
    }

    fun onPermissionResult(granted: Boolean) {
        _storagePermissionGranted.value = granted
        if (granted) {
            triggerStorageScan()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: LibraryFilter) {
        _selectedFilter.value = filter
    }

    fun setDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
    }

    fun setSortOption(option: BookSortOption) {
        _sortOption.value = option
    }

    fun selectFolder(folderPath: String?) {
        _selectedFolder.value = folderPath
    }

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id, book.isFavorite)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
        }
    }

    fun importBook(uri: Uri, fileName: String, onComplete: (BookEntity?) -> Unit) {
        viewModelScope.launch {
            val result = repository.importFile(uri, fileName)
            onComplete(result)
        }
    }

    fun triggerStorageScan() {
        viewModelScope.launch {
            repository.triggerScan()
        }
    }

    fun scanSafFolder(uri: Uri) {
        viewModelScope.launch {
            repository.scanSafFolder(uri)
        }
    }

    // Organizes a series group into a designated folder in Room
    fun organizeSeriesIntoFolder(seriesGroup: SeriesGroup, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val bookIds = seriesGroup.books.map { it.id }
            val newFolderName = seriesGroup.seriesName
            val newFolderPath = "Series/$newFolderName"
            repository.moveBooksToFolder(bookIds, newFolderName, newFolderPath)
            onComplete()
        }
    }

    // Smart Series Extractor comparing titles across comic, manga, novel
    private fun extractSmartSeries(books: List<BookEntity>): List<SeriesGroup> {
        val seriesRegex = Regex("(?i)\\b(vol(ume)?|ch(apter)?|issue|no|part|pt|#|v)\\.?\\s*\\d+([.-]\\d+)?\\b")
        val trailingNumbersRegex = Regex("(?i)[-_\\s]+\\d{1,4}\\s*$")
        val bracketRegex = Regex("(?i)\\[.*?\\]|\\(.*?\\)")

        val grouped = books.groupBy { book ->
            var cleaned = book.title
            cleaned = bracketRegex.replace(cleaned, "")
            cleaned = seriesRegex.replace(cleaned, "")
            cleaned = trailingNumbersRegex.replace(cleaned, "")
            cleaned = cleaned.replace('_', ' ').replace('-', ' ').replace('.', ' ').trim()

            if (cleaned.length < 2) {
                book.folderName.ifEmpty { "Miscellaneous" }
            } else {
                cleaned.split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    }
            }
        }

        return grouped.map { (seriesName, bookList) ->
            val detectedType = when {
                bookList.any { it.format.equals("TXT", true) || it.format.equals("EPUB", true) } -> "Light Novel"
                bookList.any { it.format.equals("CBR", true) || it.format.equals("PDF", true) } -> "Comic"
                else -> "Manga"
            }
            SeriesGroup(
                seriesName = seriesName,
                bookCount = bookList.size,
                books = bookList.sortedBy { it.title.lowercase() },
                formats = bookList.map { it.format }.distinct(),
                detectedType = detectedType,
                totalSizeBytes = bookList.sumOf { it.fileSizeBytes }
            )
        }.sortedByDescending { it.bookCount }
    }
}
