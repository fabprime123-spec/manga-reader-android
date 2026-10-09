package com.example.data.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.model.BookEntity
import com.example.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

sealed class PageContent {
    data class ImagePage(val bitmap: Bitmap) : PageContent()
    data class TextPage(val text: String, val chapterTitle: String? = null) : PageContent()
    data class ErrorPage(val message: String) : PageContent()
}

class BookContentEngine(private val context: Context) {

    private var currentPdfRenderer: PdfRenderer? = null
    private var currentFileDescriptor: ParcelFileDescriptor? = null
    private var currentPdfPath: String? = null

    suspend fun getPageCount(book: BookEntity): Int = withContext(Dispatchers.IO) {
        val localUri = book.localUri ?: return@withContext 1
        try {
            when (book.format.uppercase()) {
                BookFormat.PDF.name -> {
                    openPdfDescriptor(localUri)?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            return@withContext renderer.pageCount
                        }
                    }
                }
                BookFormat.CBZ.name, BookFormat.CBR.name, BookFormat.ZIP.name -> {
                    if (localUri.startsWith("content://")) {
                        context.contentResolver.openInputStream(Uri.parse(localUri))?.use { input ->
                            ZipInputStream(input).use { zip ->
                                var count = 0
                                var entry = zip.nextEntry
                                while (entry != null) {
                                    if (!entry.isDirectory && isImageFile(entry.name)) {
                                        count++
                                    }
                                    entry = zip.nextEntry
                                }
                                return@withContext maxOf(1, count)
                            }
                        }
                    } else {
                        val file = File(localUri)
                        if (file.exists()) {
                            ZipFile(file).use { zip ->
                                val count = zip.entries().asSequence().count { entry ->
                                    !entry.isDirectory && isImageFile(entry.name)
                                }
                                return@withContext maxOf(1, count)
                            }
                        }
                    }
                }
                BookFormat.EPUB.name -> {
                    val file = File(localUri)
                    if (file.exists()) {
                        ZipFile(file).use { zip ->
                            val chapters = zip.entries().asSequence().count { entry ->
                                !entry.isDirectory && (entry.name.endsWith(".xhtml") || entry.name.endsWith(".html"))
                            }
                            return@withContext maxOf(1, chapters)
                        }
                    }
                }
                BookFormat.TXT.name -> {
                    val text = if (localUri.startsWith("content://")) {
                        context.contentResolver.openInputStream(Uri.parse(localUri))?.bufferedReader()?.use { it.readText() } ?: ""
                    } else {
                        val file = File(localUri)
                        if (file.exists()) file.readText() else ""
                    }
                    return@withContext maxOf(1, (text.length / 1200) + 1)
                }
            }
        } catch (e: Exception) {
            Log.e("BookContentEngine", "Failed to compute page count", e)
        }
        return@withContext book.totalPages.coerceAtLeast(1)
    }

    suspend fun renderPage(book: BookEntity, pageIndex: Int): PageContent = withContext(Dispatchers.IO) {
        val localUri = book.localUri ?: return@withContext PageContent.ErrorPage("File path not found for this book")

        try {
            when (book.format.uppercase()) {
                BookFormat.PDF.name -> {
                    return@withContext renderPdfPage(localUri, pageIndex)
                }
                BookFormat.CBZ.name, BookFormat.CBR.name, BookFormat.ZIP.name -> {
                    return@withContext renderComicZipPage(localUri, pageIndex)
                }
                BookFormat.EPUB.name -> {
                    return@withContext renderEpubPage(localUri, pageIndex)
                }
                BookFormat.TXT.name -> {
                    return@withContext renderTxtPage(localUri, pageIndex)
                }
                else -> {
                    return@withContext PageContent.TextPage(
                        text = "Viewing: ${book.title}\n\nFormat: ${book.format}\nPath: $localUri\n\nPage $pageIndex",
                        chapterTitle = book.title
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("BookContentEngine", "Error rendering page $pageIndex", e)
            return@withContext PageContent.ErrorPage("Error reading file: ${e.message}")
        }
    }

    private fun openPdfDescriptor(localUri: String): ParcelFileDescriptor? {
        return if (localUri.startsWith("content://")) {
            context.contentResolver.openFileDescriptor(Uri.parse(localUri), "r")
        } else {
            val file = File(localUri)
            if (file.exists()) ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) else null
        }
    }

    private fun renderPdfPage(localUri: String, pageIndex: Int): PageContent {
        try {
            if (currentPdfPath != localUri || currentPdfRenderer == null) {
                currentPdfRenderer?.close()
                currentFileDescriptor?.close()
                currentPdfPath = localUri
                currentFileDescriptor = openPdfDescriptor(localUri)
                if (currentFileDescriptor != null) {
                    currentPdfRenderer = PdfRenderer(currentFileDescriptor!!)
                }
            }

            val renderer = currentPdfRenderer ?: return PageContent.ErrorPage("Could not open PDF file")
            val safeIndex = pageIndex.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))

            val page = renderer.openPage(safeIndex)
            val width = page.width * 2
            val height = page.height * 2
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(AndroidColor.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return PageContent.ImagePage(bitmap)
        } catch (e: Exception) {
            Log.e("BookContentEngine", "PDF render exception", e)
            return PageContent.ErrorPage("Failed to render PDF page: ${e.message}")
        }
    }

    private fun renderComicZipPage(localUri: String, pageIndex: Int): PageContent {
        try {
            val file = if (!localUri.startsWith("content://")) File(localUri) else null
            if (file != null && file.exists()) {
                ZipFile(file).use { zip ->
                    val imageEntries = zip.entries().asSequence()
                        .filter { !it.isDirectory && isImageFile(it.name) }
                        .sortedBy { it.name }
                        .toList()

                    if (imageEntries.isEmpty()) {
                        return PageContent.ErrorPage("No images found inside comic archive: ${file.name}")
                    }

                    val safeIndex = pageIndex.coerceIn(0, imageEntries.size - 1)
                    val target = imageEntries[safeIndex]
                    zip.getInputStream(target).use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            return PageContent.ImagePage(bitmap)
                        }
                    }
                }
            }
            return PageContent.ErrorPage("Unable to decode comic archive: $localUri")
        } catch (e: Exception) {
            Log.e("BookContentEngine", "Comic zip error", e)
            return PageContent.ErrorPage("Error loading comic page: ${e.message}")
        }
    }

    private fun renderEpubPage(localUri: String, pageIndex: Int): PageContent {
        try {
            val file = File(localUri)
            if (file.exists()) {
                ZipFile(file).use { zip ->
                    val textEntries = zip.entries().asSequence()
                        .filter { !it.isDirectory && (it.name.endsWith(".xhtml") || it.name.endsWith(".html") || it.name.endsWith(".txt")) }
                        .sortedBy { it.name }
                        .toList()

                    if (textEntries.isNotEmpty()) {
                        val safeIndex = pageIndex.coerceIn(0, textEntries.size - 1)
                        val entry = textEntries[safeIndex]
                        val rawText = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                        val cleanText = rawText.replace("<[^>]*>".toRegex(), " ")
                            .replace("&nbsp;", " ")
                            .replace("&quot;", "\"")
                            .replace("&amp;", "&")
                            .replace("\\s+".toRegex(), " ")
                            .trim()

                        return PageContent.TextPage(
                            text = cleanText,
                            chapterTitle = "Chapter ${safeIndex + 1}"
                        )
                    }
                }
            }
            return PageContent.ErrorPage("Could not read EPUB file: $localUri")
        } catch (e: Exception) {
            return PageContent.ErrorPage("EPUB parse error: ${e.message}")
        }
    }

    private fun renderTxtPage(localUri: String, pageIndex: Int): PageContent {
        try {
            val text = if (localUri.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(localUri))?.bufferedReader()?.use { it.readText() } ?: ""
            } else {
                val file = File(localUri)
                if (file.exists()) file.readText() else ""
            }

            if (text.isBlank()) {
                return PageContent.ErrorPage("Text file is empty")
            }

            val pageSize = 1200
            val totalPages = maxOf(1, (text.length / pageSize) + 1)
            val safeIndex = pageIndex.coerceIn(0, totalPages - 1)
            val start = (safeIndex * pageSize).coerceAtMost(text.length)
            val end = (start + pageSize).coerceAtMost(text.length)
            val pageText = text.substring(start, end).trim()

            return PageContent.TextPage(
                text = pageText,
                chapterTitle = "Section ${safeIndex + 1}"
            )
        } catch (e: Exception) {
            return PageContent.ErrorPage("Failed to read text file: ${e.message}")
        }
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
               lower.endsWith(".webp") || lower.endsWith(".bmp") || lower.endsWith(".gif")
    }

    suspend fun importFileFromUri(uri: Uri, fileName: String): BookEntity? = withContext(Dispatchers.IO) {
        try {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            val format = when (extension) {
                "cbz" -> BookFormat.CBZ.name
                "cbr" -> BookFormat.CBR.name
                "pdf" -> BookFormat.PDF.name
                "zip" -> BookFormat.ZIP.name
                "epub" -> BookFormat.EPUB.name
                "txt", "text", "md" -> BookFormat.TXT.name
                else -> BookFormat.CBZ.name
            }

            val targetDir = File(context.filesDir, "imported_books").apply { mkdirs() }
            val sanitizedName = "${System.currentTimeMillis()}_${fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"
            val destFile = File(targetDir, sanitizedName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val title = fileName.substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()

            val tempBook = BookEntity(
                id = "book_${System.currentTimeMillis()}",
                title = title.ifEmpty { "Imported Document" },
                author = "Imported File",
                format = format,
                localUri = destFile.absolutePath,
                folderPath = targetDir.absolutePath,
                folderName = "Imported Books",
                fileSizeBytes = destFile.length(),
                totalPages = 1,
                currentPage = 0,
                progressPercent = 0f,
                lastReadTimestamp = System.currentTimeMillis()
            )

            val realPageCount = getPageCount(tempBook)

            return@withContext tempBook.copy(totalPages = realPageCount)
        } catch (e: Exception) {
            Log.e("BookContentEngine", "Failed to import file from URI", e)
            return@withContext null
        }
    }
}
