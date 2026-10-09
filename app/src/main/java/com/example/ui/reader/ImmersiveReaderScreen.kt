package com.example.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.reader.PageContent
import com.example.model.BookEntity
import com.example.model.ReaderFont
import com.example.model.ReaderThemeMode
import com.example.model.ReadingDirection
import com.example.ui.components.BookmarksSheet
import com.example.ui.components.ReaderSettingsSheet
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PaperParchment
import com.example.ui.theme.PaperText
import com.example.ui.theme.SepiaBackground
import com.example.ui.theme.SepiaText
import com.example.ui.theme.ShinigamiRed
import com.example.ui.theme.SilverMute
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.ReaderViewModel

@Composable
fun ImmersiveReaderScreen(
    viewModel: ReaderViewModel,
    onBackToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val book by viewModel.currentBook.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val pageContent by viewModel.pageContent.collectAsState()
    val isLoading by viewModel.isLoadingPage.collectAsState()
    val isHudVisible by viewModel.isHudVisible.collectAsState()
    val settings by viewModel.readerSettings.collectAsState()
    val showSettings by viewModel.showSettingsSheet.collectAsState()
    val showBookmarks by viewModel.showBookmarksSheet.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    BackHandler {
        onBackToLibrary()
    }

    if (book == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ShinigamiRed)
        }
        return
    }

    val currentBook = book!!

    // Reader Canvas Background and Text Colors
    val (backgroundColor, textColor) = when (settings.themeMode) {
        ReaderThemeMode.VOID_DARK -> Pair(VoidBlack, BoneWhite)
        ReaderThemeMode.OLED_BLACK -> Pair(OledBlack, Color.White)
        ReaderThemeMode.RULES_PARCHMENT -> Pair(PaperParchment, PaperText)
        ReaderThemeMode.SEPIA_WARM -> Pair(SepiaBackground, SepiaText)
        ReaderThemeMode.CLEAN_LIGHT -> Pair(Color.White, Color.Black)
    }

    val activeFontFamily = when (settings.font) {
        ReaderFont.SERIF -> FontFamily.Serif
        ReaderFont.MONO -> FontFamily.Monospace
        ReaderFont.SANS -> FontFamily.SansSerif
        ReaderFont.GOTHIC -> FontFamily.Serif
        ReaderFont.CURSIVE -> FontFamily.Cursive
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag("immersive_reader_screen")
    ) {
        // Main Content Area with Tap Zones
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth

            // Content Viewer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isHudVisible) 60.dp else 0.dp,
                        bottom = if (isHudVisible) 80.dp else 0.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = ShinigamiRed, strokeWidth = 3.dp)
                } else {
                    when (val content = pageContent) {
                        is PageContent.ImagePage -> {
                            ZoomableImage(bitmap = content.bitmap)
                        }
                        is PageContent.TextPage -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp, vertical = 20.dp)
                            ) {
                                if (content.chapterTitle != null) {
                                    Text(
                                        text = content.chapterTitle,
                                        color = ShinigamiRed,
                                        fontSize = (settings.fontSize + 2).sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = activeFontFamily,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                                Text(
                                    text = content.text,
                                    color = textColor,
                                    fontSize = settings.fontSize.sp,
                                    lineHeight = (settings.fontSize * settings.lineSpacingMultiplier).sp,
                                    fontFamily = activeFontFamily,
                                    textAlign = TextAlign.Start
                                )
                                Spacer(modifier = Modifier.height(60.dp))
                            }
                        }
                        is PageContent.ErrorPage -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = ShinigamiRed, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = content.message, color = BoneWhite, fontSize = 14.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            // Invisible Tap Zones (Left 25%, Center 50%, Right 25%)
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Tap Zone
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (settings.readingDirection == ReadingDirection.RIGHT_TO_LEFT) {
                                viewModel.goToNextPage()
                            } else {
                                viewModel.goToPreviousPage()
                            }
                        }
                )

                // Center Tap Zone (Toggles HUD)
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.toggleHud()
                        }
                )

                // Right Tap Zone
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (settings.readingDirection == ReadingDirection.RIGHT_TO_LEFT) {
                                viewModel.goToPreviousPage()
                            } else {
                                viewModel.goToNextPage()
                            }
                        }
                )
            }
        }

        // Top HUD Overlay
        AnimatedVisibility(
            visible = isHudVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VoidBlack.copy(alpha = 0.92f))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackToLibrary,
                            modifier = Modifier.testTag("reader_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Library",
                                tint = BoneWhite
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Text(
                                text = currentBook.title,
                                color = BoneWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentBook.format} • ${settings.readingDirection.displayName.substringBefore('(')}",
                                color = SilverMute,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Bookmark button
                        IconButton(
                            onClick = { viewModel.toggleBookmarksSheet(true) },
                            modifier = Modifier.testTag("reader_bookmark_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmarks",
                                tint = BoneWhite
                            )
                        }

                        // Settings button
                        IconButton(
                            onClick = { viewModel.toggleSettingsSheet(true) },
                            modifier = Modifier.testTag("reader_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Reader Settings",
                                tint = BoneWhite
                            )
                        }
                    }
                }
            }
        }

        // Bottom HUD Overlay (Scrubber & Navigation)
        AnimatedVisibility(
            visible = isHudVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VoidBlack.copy(alpha = 0.92f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Page info text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${currentPage + 1} of ${currentBook.totalPages}",
                            color = BoneWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        val percent = (((currentPage + 1).toFloat() / currentBook.totalPages.toFloat()) * 100f).toInt()
                        Text(
                            text = "$percent%",
                            color = ShinigamiRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Scrubber Slider
                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { viewModel.goToPage(it.toInt()) },
                        valueRange = 0f..(currentBook.totalPages - 1).coerceAtLeast(0).toFloat(),
                        steps = (currentBook.totalPages - 2).coerceAtLeast(0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_scrubber"),
                        colors = SliderDefaults.colors(
                            thumbColor = ShinigamiRed,
                            activeTrackColor = ShinigamiRed,
                            inactiveTrackColor = SurfaceDark
                        )
                    )

                    // Quick Jump Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.goToPreviousPage() },
                            enabled = currentPage > 0,
                            modifier = Modifier.testTag("reader_prev_page")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Page",
                                tint = if (currentPage > 0) BoneWhite else SilverMute.copy(alpha = 0.3f)
                            )
                        }

                        Text(
                            text = if (settings.readingDirection == ReadingDirection.RIGHT_TO_LEFT) "Manga RTL Mode" else "Standard LTR Mode",
                            color = SilverMute,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        IconButton(
                            onClick = { viewModel.goToNextPage() },
                            enabled = currentPage < currentBook.totalPages - 1,
                            modifier = Modifier.testTag("reader_next_page")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Page",
                                tint = if (currentPage < currentBook.totalPages - 1) BoneWhite else SilverMute.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        // Reader Settings Bottom Sheet
        if (showSettings) {
            ReaderSettingsSheet(
                settings = settings,
                onFontChange = { viewModel.updateFont(it) },
                onFontSizeChange = { viewModel.updateFontSize(it) },
                onThemeChange = { viewModel.updateThemeMode(it) },
                onDirectionChange = { viewModel.updateReadingDirection(it) },
                onDismiss = { viewModel.toggleSettingsSheet(false) }
            )
        }

        // Bookmarks Bottom Sheet
        if (showBookmarks) {
            BookmarksSheet(
                currentPage = currentPage,
                bookmarks = bookmarks,
                onJumpToPage = { viewModel.goToPage(it) },
                onAddBookmark = { viewModel.addBookmark(it) },
                onDeleteBookmark = { viewModel.deleteBookmark(it) },
                onDismiss = { viewModel.toggleBookmarksSheet(false) }
            )
        }
    }
}

@Composable
private fun ZoomableImage(bitmap: android.graphics.Bitmap) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(transformState),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Manga Page",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )
    }
}
