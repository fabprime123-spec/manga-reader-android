package com.example.ui.tabs

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BookEntity
import com.example.ui.components.BookCard
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.SilverMute
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.LibraryViewModel
import java.io.File

private data class QuickNavTile(
    val id: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun BrowseTab(
    viewModel: LibraryViewModel,
    onOpenBook: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val books by viewModel.books.collectAsState()
    val activeAccent by viewModel.activeAccent.collectAsState()
    val allBookmarks by viewModel.allBookmarks.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeCategoryFilter by remember { mutableStateOf<String?>(null) }

    // Real device storage calculations
    val (internalUsed, internalTotal) = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val avail = stat.availableBlocksLong * stat.blockSizeLong
            val used = total - avail
            val usedGb = used / (1024.0 * 1024.0 * 1024.0)
            val totalGb = total / (1024.0 * 1024.0 * 1024.0)
            Pair("%.1f GB".format(usedGb), "%.1f GB".format(totalGb))
        } catch (e: Exception) {
            Pair("System", "Ready")
        }
    }

    val (sdUsed, sdTotal) = remember {
        try {
            val ext = Environment.getExternalStorageDirectory()
            if (ext != null && ext.exists()) {
                val stat = StatFs(ext.path)
                val total = stat.blockCountLong * stat.blockSizeLong
                val avail = stat.availableBlocksLong * stat.blockSizeLong
                val used = total - avail
                val usedGb = used / (1024.0 * 1024.0 * 1024.0)
                val totalGb = total / (1024.0 * 1024.0 * 1024.0)
                Pair("%.1f GB".format(usedGb), "%.1f GB".format(totalGb))
            } else {
                Pair("Mounted", "Available")
            }
        } catch (e: Exception) {
            Pair("SD Card", "Available")
        }
    }

    val quickTiles = listOf(
        QuickNavTile("download", "download", Icons.Default.Download),
        QuickNavTile("favorites", "favorites", Icons.Default.Favorite),
        QuickNavTile("continue", "continue", Icons.Default.PlayArrow),
        QuickNavTile("read_later", "read later", Icons.Default.Bookmark),
        QuickNavTile("recent", "more", Icons.Default.History),
        QuickNavTile("all_tomes", "more", Icons.Default.MoreHoriz)
    )

    val filteredCategoryBooks = remember(books, activeCategoryFilter, allBookmarks) {
        when (activeCategoryFilter) {
            "download" -> books.filter { it.folderName.contains("download", ignoreCase = true) || it.folderPath.contains("download", ignoreCase = true) }
            "favorites" -> books.filter { it.isFavorite }
            "continue" -> books.filter { it.currentPage > 0 && it.progressPercent < 100f }
            "read_later" -> {
                val bookIds = allBookmarks.map { it.bookId }.toSet()
                books.filter { bookIds.contains(it.id) }
            }
            "recent" -> books.sortedByDescending { it.lastReadTimestamp }
            "all_tomes" -> books
            else -> emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("browse_tab")
    ) {
        // Header matching Screenshot 4: "library       search"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeCategoryFilter != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { activeCategoryFilter = null }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BoneWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeCategoryFilter!!.replace('_', ' '),
                        color = BoneWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                Text(
                    text = "browse",
                    color = BoneWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "search",
                color = if (isSearchExpanded) activeAccent.color else Color(0xFFC0C0C8),
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .clickable {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) {
                            searchQuery = ""
                            viewModel.updateSearchQuery("")
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        HorizontalDivider(
            color = Color(0xFF38383E),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search Field
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.updateSearchQuery(it)
                },
                placeholder = { Text("Filter books by name or path...", color = SilverMute) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SilverMute) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = SilverMute)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BoneWhite,
                    unfocusedTextColor = BoneWhite,
                    focusedBorderColor = activeAccent.color,
                    unfocusedBorderColor = OutlineDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                singleLine = true
            )
        }

        if (activeCategoryFilter != null) {
            // Category drilldown view
            if (filteredCategoryBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tomes found under ${activeCategoryFilter!!.replace('_', ' ')}",
                        color = SilverMute,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCategoryBooks, key = { it.id }) { book ->
                        BookCard(
                            book = book,
                            onClick = { onOpenBook(book) },
                            onToggleFavorite = { viewModel.toggleFavorite(book) },
                            onDelete = { viewModel.deleteBook(book) }
                        )
                    }
                }
            }
        } else {
            // Main Browse View matching Screenshot 4!
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Section 1: "device storage" in blue text
                item {
                    Column {
                        Text(
                            text = "device storage",
                            color = Color(0xFF3877A8), // Blue color from drawing
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 1: internal storage
                        StorageDeviceRow(
                            name = "internal",
                            used = internalUsed,
                            total = internalTotal,
                            icon = Icons.Default.Storage,
                            onClick = { viewModel.triggerStorageScan() }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 2: sd card storage
                        StorageDeviceRow(
                            name = "sd card",
                            used = sdUsed,
                            total = sdTotal,
                            icon = Icons.Default.SdCard,
                            onClick = { viewModel.triggerStorageScan() }
                        )
                    }
                }

                // Section 2: 2 rows of 3 square tiles matching Screenshot 4
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        val tileRows = quickTiles.chunked(3)
                        tileRows.forEach { rowTiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowTiles.forEach { tile ->
                                    QuickTileItem(
                                        tile = tile,
                                        onClick = { activeCategoryFilter = tile.id }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Exactly matches Screenshot 4 storage row
@Composable
private fun StorageDeviceRow(
    name: String,
    used: String,
    total: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Slate rounded box on left (width ~74dp, height ~46dp)
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF5A626C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFD8DCE2),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Text(
                text = name,
                color = BoneWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif
            )
        }

        Text(
            text = "$used / $total",
            color = Color(0xFFC0C0C8),
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

// Exactly matches Screenshot 4 quick tiles (slate rounded square + blue text underneath)
@Composable
private fun QuickTileItem(
    tile: QuickNavTile,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
    ) {
        // Slate rounded square matching Screenshot 4
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF5A626C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = Color(0xFFD8DCE2),
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Blue label matching Screenshot 4
        Text(
            text = tile.title,
            color = Color(0xFF3877A8),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
