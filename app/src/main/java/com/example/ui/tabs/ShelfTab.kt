package com.example.ui.tabs

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.SilverMute
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.LibraryViewModel

// Exact colors from Drawing 2: Blue, Purple, Green
private val DrawingColors = listOf(
    Color(0xFF3877A8), // Blue (Column 0)
    Color(0xFF7B62A8), // Purple / Lavender (Column 1)
    Color(0xFF2E7942)  // Green (Column 2)
)

@Composable
fun ShelfTab(
    viewModel: LibraryViewModel,
    onOpenBook: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val books by viewModel.books.collectAsState()
    val recentBooks by viewModel.recentBooks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val activeAccent by viewModel.activeAccent.collectAsState()

    var hasRequestedPermissionOnLaunch by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.triggerStorageScan()
    }

    val needsManageStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()

    LaunchedEffect(Unit) {
        if (!hasRequestedPermissionOnLaunch) {
            hasRequestedPermissionOnLaunch = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    viewModel.triggerStorageScan()
                }
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("shelf_tab")
    ) {
        // Exact Header from Drawing 2
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Manga Reader",
            color = BoneWhite,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "All comic, manga, novels and document",
            color = Color(0xFF9885C4),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (books.isEmpty()) {
            // Clean Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF3877A8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = BoneWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "No Comics or Manga on Device",
                        color = BoneWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (needsManageStorage)
                            "Storage permission is needed so DeathReader can automatically find and display your .cbz, .cbr, .pdf, .zip, .epub, and .txt files."
                        else
                            "Place your comic and novel files in Downloads, Documents, or Comics folders, then tap Scan.",
                        color = SilverMute,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
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
                                viewModel.triggerStorageScan()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeAccent.color),
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
        } else {
            // Exact 3-column Grid from Drawing 2!
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 3-Column Grid Cards with Drawing 2 Colors (Blue, Purple, Green)
                itemsIndexed(books, key = { _, book -> book.id }) { index, book ->
                    val colorIndex = index % 3
                    val cardBgColor = DrawingColors[colorIndex]

                    DrawingGridCard(
                        book = book,
                        backgroundColor = cardBgColor,
                        onClick = { onOpenBook(book) }
                    )
                }

                // "Recent Viewed" Header matching Drawing 2!
                item(span = { GridItemSpan(3) }) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        // Gray circular icon matching Drawing 2
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF888E96)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = BoneWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Recent Viewed",
                            color = BoneWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Recent Viewed row
                item(span = { GridItemSpan(3) }) {
                    if (recentBooks.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(recentBooks) { book ->
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFF7B62A8))
                                        .clickable { onOpenBook(book) }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Column {
                                        Text(
                                            text = book.title,
                                            color = BoneWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${book.progressPercent.toInt()}%",
                                            color = Color(0xFFD8DCE2),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No recently viewed tomes yet.",
                            color = SilverMute,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 40.dp, bottom = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Exactly matches the cards in Drawing 2: tall portrait rounded rectangle with large smooth corners
@Composable
private fun DrawingGridCard(
    book: BookEntity,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f) // Portrait aspect ratio from Drawing 2
            .clip(RoundedCornerShape(20.dp)) // Large smooth rounded corners matching drawing
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            // Format tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = book.format.uppercase(),
                    color = BoneWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = book.title,
                color = BoneWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
