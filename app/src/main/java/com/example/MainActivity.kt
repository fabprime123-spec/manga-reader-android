package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.local.BookDatabase
import com.example.data.reader.BookContentEngine
import com.example.data.repository.BookRepository
import com.example.data.scanner.StorageScanner
import com.example.model.BookEntity
import com.example.ui.library.LibraryScreen
import com.example.ui.reader.ImmersiveReaderScreen
import com.example.ui.theme.DeathReaderTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

enum class ScreenState {
    LIBRARY,
    READER
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DeathReaderApp()
        }
    }
}

@Composable
fun DeathReaderApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val database = remember { BookDatabase.getInstance(context) }
    val contentEngine = remember { BookContentEngine(context) }
    val storageScanner = remember { StorageScanner(context, database.bookDao(), contentEngine) }
    val repository = remember {
        BookRepository(
            context = context,
            bookDao = database.bookDao(),
            contentEngine = contentEngine,
            storageScanner = storageScanner,
            scope = coroutineScope
        )
    }

    val libraryViewModel = remember {
        LibraryViewModel(repository)
    }
    val readerViewModel = remember {
        ReaderViewModel(repository, contentEngine)
    }
    val activeAccent by libraryViewModel.activeAccent.collectAsState()

    var currentScreen by remember { mutableStateOf(ScreenState.LIBRARY) }

    DeathReaderTheme(darkTheme = true, accent = activeAccent) {
        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
            when (screen) {
                ScreenState.LIBRARY -> {
                    LibraryScreen(
                        libraryViewModel = libraryViewModel,
                        readerViewModel = readerViewModel,
                        onOpenBook = { book: BookEntity ->
                            readerViewModel.loadBook(book)
                            currentScreen = ScreenState.READER
                        },
                        onOpenBookAtPage = { bookId: String, pageIndex: Int ->
                            coroutineScope.launch {
                                val book = database.bookDao().getBookById(bookId)
                                if (book != null) {
                                    readerViewModel.loadBook(book.copy(currentPage = pageIndex))
                                    readerViewModel.goToPage(pageIndex)
                                    currentScreen = ScreenState.READER
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ScreenState.READER -> {
                    ImmersiveReaderScreen(
                        viewModel = readerViewModel,
                        onBackToLibrary = {
                            currentScreen = ScreenState.LIBRARY
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
