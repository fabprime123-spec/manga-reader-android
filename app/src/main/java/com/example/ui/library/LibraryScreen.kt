package com.example.ui.library

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BookEntity
import com.example.ui.tabs.BrowseTab
import com.example.ui.tabs.FoldersTab
import com.example.ui.tabs.LibraryTab
import com.example.ui.tabs.SettingsTab
import com.example.ui.tabs.ShelfTab
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.SilverMute
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel

enum class NavigationTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    FOLDERS("Folders", Icons.Filled.Folder, Icons.Outlined.Folder),
    LIBRARY("Library", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    BROWSE("Browse", Icons.Filled.Explore, Icons.Outlined.Explore),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    readerViewModel: ReaderViewModel,
    onOpenBook: (BookEntity) -> Unit,
    onOpenBookAtPage: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isScanning by libraryViewModel.isScanning.collectAsState()
    val activeAccent by libraryViewModel.activeAccent.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack),
        topBar = {
            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(3.dp),
                    color = activeAccent.color,
                    trackColor = Color.Transparent
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = BoneWhite,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationTab.values().forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.SansSerif
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BoneWhite,
                            selectedTextColor = BoneWhite,
                            indicatorColor = activeAccent.color,
                            unselectedIconColor = SilverMute,
                            unselectedTextColor = SilverMute
                        ),
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        containerColor = VoidBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "TabTransition") { tabIndex ->
                when (tabIndex) {
                    0 -> ShelfTab(
                        viewModel = libraryViewModel,
                        onOpenBook = onOpenBook
                    )
                    1 -> FoldersTab(
                        viewModel = libraryViewModel,
                        onOpenBook = onOpenBook
                    )
                    2 -> LibraryTab(
                        viewModel = libraryViewModel,
                        onOpenBook = onOpenBook
                    )
                    3 -> BrowseTab(
                        viewModel = libraryViewModel,
                        onOpenBook = onOpenBook
                    )
                    4 -> SettingsTab(
                        libraryViewModel = libraryViewModel,
                        readerViewModel = readerViewModel
                    )
                }
            }
        }
    }
}
