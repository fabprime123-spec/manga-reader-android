package com.example.data.scanner

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import com.example.data.local.BookDao
import com.example.data.reader.BookContentEngine
import com.example.model.BookEntity
import com.example.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class StorageScanner(
    private val context: Context,
    private val bookDao: BookDao,
    private val contentEngine: BookContentEngine
) {
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    private val supportedExtensions = setOf(
        "cbz", "cbr", "pdf", "zip", "epub", "txt", "text", "md", "manga"
    )

    suspend fun scanDeviceStorage() = withContext(Dispatchers.IO) {
        _isScanning.value = true
        _scannedCount.value = 0

        val discovered = mutableListOf<BookEntity>()

        try {
            // Common storage directories to scan like VLC
            val candidateDirs = mutableListOf<File>()

            // Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists()) candidateDirs.add(downloadsDir)

            // Documents directory
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir != null && docsDir.exists()) candidateDirs.add(docsDir)

            // Primary External root Comics/Manga/Books
            val rootDir = Environment.getExternalStorageDirectory()
            if (rootDir != null && rootDir.exists()) {
                listOf("Comics", "Manga", "Books", "Novels", "Download", "Documents").forEach { name ->
                    val sub = File(rootDir, name)
                    if (sub.exists()) candidateDirs.add(sub)
                }
            }

            // App internal & external sandbox folders
            val appInternal = File(context.filesDir, "imported_books")
            if (appInternal.exists()) candidateDirs.add(appInternal)

            context.getExternalFilesDirs(null)?.filterNotNull()?.forEach {
                candidateDirs.add(it)
            }

            // Recursively scan candidate directories (up to depth 3)
            for (dir in candidateDirs.distinctBy { it.absolutePath }) {
                scanDirectoryRecursively(dir, 0, discovered)
            }

            if (discovered.isNotEmpty()) {
                bookDao.insertAll(discovered)
            }
        } catch (e: Exception) {
            Log.e("StorageScanner", "Error during storage scan", e)
        } finally {
            _isScanning.value = false
        }
    }

    private suspend fun scanDirectoryRecursively(dir: File, depth: Int, result: MutableList<BookEntity>) {
        if (!dir.exists() || !dir.isDirectory || depth > 3) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirectoryRecursively(file, depth + 1, result)
                }
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (supportedExtensions.contains(ext)) {
                    val format = when (ext) {
                        "cbz" -> BookFormat.CBZ.name
                        "cbr" -> BookFormat.CBR.name
                        "pdf" -> BookFormat.PDF.name
                        "zip" -> BookFormat.ZIP.name
                        "epub" -> BookFormat.EPUB.name
                        "txt", "text", "md" -> BookFormat.TXT.name
                        else -> BookFormat.CBZ.name
                    }

                    val title = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
                    val folderName = dir.name.ifEmpty { "Storage" }
                    val folderPath = dir.absolutePath

                    val book = BookEntity(
                        id = "file_${file.absolutePath.hashCode()}",
                        title = title.ifEmpty { file.name },
                        author = folderName,
                        format = format,
                        localUri = file.absolutePath,
                        folderPath = folderPath,
                        folderName = folderName,
                        fileSizeBytes = file.length(),
                        totalPages = 1,
                        currentPage = 0,
                        progressPercent = 0f,
                        collectionName = folderName,
                        lastReadTimestamp = file.lastModified()
                    )

                    val pages = contentEngine.getPageCount(book)
                    val finalizedBook = book.copy(totalPages = pages)

                    result.add(finalizedBook)
                    _scannedCount.value = result.size
                }
            }
        }
    }

    suspend fun scanSafTreeUri(treeUri: Uri) = withContext(Dispatchers.IO) {
        _isScanning.value = true
        val discovered = mutableListOf<BookEntity>()

        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val folderName = docId.substringAfterLast(':', "Imported Folder")

            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val childDocId = if (idIdx != -1) cursor.getString(idIdx) else null ?: continue
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) else null ?: continue
                    val size = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L

                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (supportedExtensions.contains(ext)) {
                        val format = when (ext) {
                            "cbz" -> BookFormat.CBZ.name
                            "cbr" -> BookFormat.CBR.name
                            "pdf" -> BookFormat.PDF.name
                            "zip" -> BookFormat.ZIP.name
                            "epub" -> BookFormat.EPUB.name
                            "txt", "text", "md" -> BookFormat.TXT.name
                            else -> BookFormat.CBZ.name
                        }

                        val fileDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        val title = name.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()

                        val book = BookEntity(
                            id = "saf_${fileDocUri.hashCode()}",
                            title = title.ifEmpty { name },
                            author = folderName,
                            format = format,
                            localUri = fileDocUri.toString(),
                            folderPath = treeUri.toString(),
                            folderName = folderName,
                            fileSizeBytes = size,
                            totalPages = 1,
                            currentPage = 0,
                            progressPercent = 0f,
                            collectionName = folderName,
                            lastReadTimestamp = System.currentTimeMillis()
                        )
                        discovered.add(book)
                    }
                }
            }

            if (discovered.isNotEmpty()) {
                bookDao.insertAll(discovered)
            }
        } catch (e: Exception) {
            Log.e("StorageScanner", "SAF scan error", e)
        } finally {
            _isScanning.value = false
        }
    }
}
