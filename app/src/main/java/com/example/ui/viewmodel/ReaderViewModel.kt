package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.reader.BookContentEngine
import com.example.data.reader.PageContent
import com.example.data.repository.BookRepository
import com.example.model.BookEntity
import com.example.model.BookmarkEntity
import com.example.model.ReaderFont
import com.example.model.ReaderSettings
import com.example.model.ReaderThemeMode
import com.example.model.ReadingDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val repository: BookRepository,
    private val contentEngine: BookContentEngine
) : ViewModel() {

    private val _currentBook = MutableStateFlow<BookEntity?>(null)
    val currentBook: StateFlow<BookEntity?> = _currentBook.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _pageContent = MutableStateFlow<PageContent>(PageContent.TextPage("Loading page...", null))
    val pageContent: StateFlow<PageContent> = _pageContent.asStateFlow()

    private val _isLoadingPage = MutableStateFlow(false)
    val isLoadingPage: StateFlow<Boolean> = _isLoadingPage.asStateFlow()

    private val _isHudVisible = MutableStateFlow(true)
    val isHudVisible: StateFlow<Boolean> = _isHudVisible.asStateFlow()

    private val _readerSettings = MutableStateFlow(ReaderSettings())
    val readerSettings: StateFlow<ReaderSettings> = _readerSettings.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    private val _showBookmarksSheet = MutableStateFlow(false)
    val showBookmarksSheet: StateFlow<Boolean> = _showBookmarksSheet.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> = _currentBook.flatMapLatest { book ->
        if (book != null) repository.getBookmarksForBook(book.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var loadJob: Job? = null

    fun loadBook(book: BookEntity) {
        _currentBook.value = book
        _currentPage.value = book.currentPage
        // Set appropriate default reading direction
        if (book.format.equals("MANGA", true)) {
            _readerSettings.value = _readerSettings.value.copy(readingDirection = ReadingDirection.RIGHT_TO_LEFT)
        } else {
            _readerSettings.value = _readerSettings.value.copy(readingDirection = ReadingDirection.LEFT_TO_RIGHT)
        }
        loadPage(book.currentPage)
    }

    fun toggleHud() {
        _isHudVisible.value = !_isHudVisible.value
    }

    fun showHud() {
        _isHudVisible.value = true
    }

    fun goToNextPage() {
        val book = _currentBook.value ?: return
        val next = (_currentPage.value + 1).coerceAtMost(book.totalPages - 1)
        if (next != _currentPage.value) {
            goToPage(next)
        }
    }

    fun goToPreviousPage() {
        val prev = (_currentPage.value - 1).coerceAtLeast(0)
        if (prev != _currentPage.value) {
            goToPage(prev)
        }
    }

    fun goToPage(page: Int) {
        val book = _currentBook.value ?: return
        val target = page.coerceIn(0, (book.totalPages - 1).coerceAtLeast(0))
        _currentPage.value = target
        loadPage(target)
        viewModelScope.launch {
            repository.updateReadingProgress(book.id, target, book.totalPages)
            _currentBook.value = book.copy(
                currentPage = target,
                progressPercent = ((target + 1).toFloat() / book.totalPages.toFloat()) * 100f
            )
        }
    }

    private fun loadPage(pageIndex: Int) {
        val book = _currentBook.value ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoadingPage.value = true
            val content = contentEngine.renderPage(book, pageIndex)
            _pageContent.value = content
            _isLoadingPage.value = false
        }
    }

    fun updateFont(font: ReaderFont) {
        _readerSettings.value = _readerSettings.value.copy(font = font)
    }

    fun updateFontSize(size: Int) {
        _readerSettings.value = _readerSettings.value.copy(fontSize = size.coerceIn(12, 34))
    }

    fun updateThemeMode(theme: ReaderThemeMode) {
        _readerSettings.value = _readerSettings.value.copy(themeMode = theme)
    }

    fun updateReadingDirection(direction: ReadingDirection) {
        _readerSettings.value = _readerSettings.value.copy(readingDirection = direction)
    }

    fun toggleSettingsSheet(show: Boolean) {
        _showSettingsSheet.value = show
    }

    fun toggleBookmarksSheet(show: Boolean) {
        _showBookmarksSheet.value = show
    }

    fun addBookmark(note: String) {
        val book = _currentBook.value ?: return
        val page = _currentPage.value
        viewModelScope.launch {
            repository.addBookmark(
                bookId = book.id,
                bookTitle = book.title,
                pageIndex = page,
                title = "Bookmark at Page ${page + 1}",
                note = note
            )
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
        }
    }
}
