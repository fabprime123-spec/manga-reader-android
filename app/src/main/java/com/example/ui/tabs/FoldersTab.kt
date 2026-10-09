package com.example.ui.tabs

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BookEntity
import com.example.model.ComicFolder
import com.example.model.SeriesGroup
import com.example.ui.components.BookCard
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.SilverMute
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.LibraryViewModel

enum class FolderSortOption(val displayName: String) {
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    COUNT_DESC("Most Tomes"),
    SIZE_DESC("Largest Size")
}

@Composable
fun FoldersTab(
    viewModel: LibraryViewModel,
    onOpenBook: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rawFolders by viewModel.folders.collectAsState()
    val rawSeries by viewModel.seriesGroups.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val booksInFolder by viewModel.books.collectAsState()
    val activeAccent by viewModel.activeAccent.collectAsState()

    var folderSubTab by remember { mutableIntStateOf(0) } // 0 = Device Folders, 1 = Series & Volumes
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(FolderSortOption.COUNT_DESC) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var folderSearchQuery by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.triggerStorageScan()
    }

    val needsManageStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()

    BackHandler(enabled = selectedFolder != null) {
        viewModel.selectFolder(null)
    }

    // Filter and sort device folders
    val folders = remember(rawFolders, folderSearchQuery, sortOption) {
        rawFolders
            .filter {
                folderSearchQuery.isBlank() || it.folderName.contains(folderSearchQuery, ignoreCase = true)
            }
            .let { list ->
                when (sortOption) {
                    FolderSortOption.NAME_ASC -> list.sortedBy { it.folderName.lowercase() }
                    FolderSortOption.NAME_DESC -> list.sortedByDescending { it.folderName.lowercase() }
                    FolderSortOption.COUNT_DESC -> list.sortedByDescending { it.bookCount }
                    FolderSortOption.SIZE_DESC -> list.sortedByDescending { it.totalSizeBytes }
                }
            }
    }

    // Filter and sort smart series groups
    val seriesList = remember(rawSeries, folderSearchQuery, sortOption) {
        rawSeries
            .filter {
                folderSearchQuery.isBlank() || it.seriesName.contains(folderSearchQuery, ignoreCase = true)
            }
            .let { list ->
                when (sortOption) {
                    FolderSortOption.NAME_ASC -> list.sortedBy { it.seriesName.lowercase() }
                    FolderSortOption.NAME_DESC -> list.sortedByDescending { it.seriesName.lowercase() }
                    FolderSortOption.COUNT_DESC -> list.sortedByDescending { it.bookCount }
                    FolderSortOption.SIZE_DESC -> list.sortedByDescending { it.totalSizeBytes }
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("folders_tab")
    ) {
        if (selectedFolder != null) {
            // Folder Drilldown view header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectFolder(null) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Folders",
                        tint = BoneWhite
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = selectedFolder!!.substringAfterLast('/', "Folder"),
                        color = BoneWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "${booksInFolder.size} items",
                        color = SilverMute,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Main Folder List Header: Capitalized header with Sort and Search icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Folders",
                    color = BoneWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort Folders",
                                tint = BoneWhite
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            FolderSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            color = if (sortOption == option) activeAccent.color else BoneWhite,
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) folderSearchQuery = ""
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Folders",
                            tint = if (isSearchExpanded) activeAccent.color else BoneWhite
                        )
                    }
                }
            }

            // Sub-tabs: Device Folders vs Smart Series & Volumes
            TabRow(
                selectedTabIndex = folderSubTab,
                containerColor = VoidBlack,
                contentColor = BoneWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[folderSubTab]),
                        color = activeAccent.color
                    )
                },
                divider = {
                    HorizontalDivider(color = Color(0xFF38383E), thickness = 1.dp)
                },
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = folderSubTab == 0,
                    onClick = { folderSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Device Folders",
                                fontSize = 13.sp,
                                fontWeight = if (folderSubTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
                Tab(
                    selected = folderSubTab == 1,
                    onClick = { folderSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Series & Volumes",
                                fontSize = 13.sp,
                                fontWeight = if (folderSubTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }

            // Search bar if toggled
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = folderSearchQuery,
                    onValueChange = { folderSearchQuery = it },
                    placeholder = { Text("Filter by folder or series name...", color = SilverMute) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SilverMute) },
                    trailingIcon = {
                        if (folderSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { folderSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = SilverMute)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
        }

        // Folder View Content
        if (selectedFolder != null) {
            // Viewing items inside a specific folder
            if (booksInFolder.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No comics or books found in this folder", color = SilverMute, fontSize = 14.sp)
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = if (maxWidth > 600.dp) 3 else 2
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(booksInFolder, key = { it.id }) { book ->
                            BookCard(
                                book = book,
                                onClick = { onOpenBook(book) },
                                onToggleFavorite = { viewModel.toggleFavorite(book) },
                                onDelete = { viewModel.deleteBook(book) }
                            )
                        }
                    }
                }
            }
        } else if (folderSubTab == 0) {
            // Sub-Tab 0: Device Folders (matching Screenshot 2: download, telegram, etc.)
            if (folders.isEmpty()) {
                EmptyFoldersState(
                    needsManageStorage = needsManageStorage,
                    isScanning = isScanning,
                    accentColor = activeAccent.color,
                    onGrant = {
                        if (needsManageStorage) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                context.startActivity(intent)
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                    android.Manifest.permission.READ_MEDIA_IMAGES
                                )
                            )
                            viewModel.triggerStorageScan()
                        }
                    }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(folders, key = { it.folderPath }) { folder ->
                        FolderRowItem(
                            folder = folder,
                            onClick = { viewModel.selectFolder(folder.folderPath) }
                        )
                    }
                }
            }
        } else {
            // Sub-Tab 1: Smart Series & Volumes Grouping
            if (seriesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = activeAccent.color,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Series Detected Yet",
                            color = BoneWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scan storage containing manga, comic, or light novel volumes with title patterns.",
                            color = SilverMute,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(seriesList, key = { it.seriesName }) { series ->
                        SeriesGroupCard(
                            series = series,
                            accentColor = activeAccent.color,
                            onOpenBook = onOpenBook,
                            onOrganize = {
                                viewModel.organizeSeriesIntoFolder(series) {
                                    Toast.makeText(context, "Organized ${series.seriesName} into folder!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Exactly matches Screenshot 2: Slate rounded rectangle on Left, Folder name on Right
@Composable
private fun FolderRowItem(
    folder: ComicFolder,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Slate rounded rectangle matching Screenshot 2 (width 86dp, height 52dp)
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF5A626C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFFD8DCE2),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(22.dp))

        // Lowercase folder name centered vertically on the right
        Column {
            Text(
                text = folder.folderName.lowercase(),
                color = BoneWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${folder.bookCount} tomes",
                color = Color(0xFF888E96),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SeriesGroupCard(
    series: SeriesGroup,
    accentColor: Color,
    onOpenBook: (BookEntity) -> Unit,
    onOrganize: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, OutlineDark, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderCopy,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = series.seriesName,
                            color = BoneWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = series.detectedType,
                                color = accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "${series.bookCount} volumes • ${series.formats.joinToString(", ")}",
                        color = SilverMute,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOrganize) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMove,
                        contentDescription = "Organize Series",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Volumes",
                        tint = BoneWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = OutlineDark, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                series.books.forEachIndexed { idx, book ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .clickable { onOpenBook(book) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${idx + 1}.",
                                color = SilverMute,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(
                                text = book.title,
                                color = BoneWhite,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = book.format.uppercase(),
                                color = BoneWhite,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFoldersState(
    needsManageStorage: Boolean,
    isScanning: Boolean,
    accentColor: Color,
    onGrant: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF565E68)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = BoneWhite,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "No Comic Folders Found",
                color = BoneWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (needsManageStorage)
                    "Storage permission is required so DeathReader can automatically list folders containing your manga, comics, and books."
                else
                    "Ensure comic and novel files (.cbz, .cbr, .pdf, .zip, .epub, .txt) exist in your device storage, then tap Scan.",
                color = SilverMute,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BoneWhite, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scanning device...", color = BoneWhite)
                } else {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (needsManageStorage) "Grant Permission & Scan" else "Scan Device Storage", color = BoneWhite)
                }
            }
        }
    }
}
