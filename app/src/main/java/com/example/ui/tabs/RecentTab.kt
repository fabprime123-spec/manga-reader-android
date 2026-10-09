package com.example.ui.tabs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BookEntity
import com.example.ui.components.BookCard
import com.example.ui.components.ProgressTrackerCard
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.ShinigamiRed
import com.example.ui.theme.SilverMute
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.LibraryViewModel

@Composable
fun RecentTab(
    viewModel: LibraryViewModel,
    onOpenBook: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.readingStats.collectAsState()
    val recentBooks by viewModel.recentBooks.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .testTag("recent_tab")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = if (maxWidth > 600.dp) 3 else 2

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Reading Progress & Streak Tracker
                item(span = { GridItemSpan(columns) }) {
                    ProgressTrackerCard(stats = stats)
                }

                item(span = { GridItemSpan(columns) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = ShinigamiRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "CONTINUE READING & RECENT TOMES",
                            color = BoneWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 1.sp
                        )
                    }
                }

                if (recentBooks.isEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No reading history recorded yet.",
                                    color = BoneWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Open any comic, novel, or manga from Library or Folders to track progress.",
                                    color = SilverMute,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(recentBooks, key = { it.id }) { book ->
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
    }
}
