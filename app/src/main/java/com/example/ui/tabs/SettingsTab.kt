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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReaderFont
import com.example.model.ReaderSettings
import com.example.model.ReaderThemeMode
import com.example.model.ReadingDirection
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.PaperParchment
import com.example.ui.theme.SaturatedAccent
import com.example.ui.theme.SepiaBackground
import com.example.ui.theme.ShinigamiRed
import com.example.ui.theme.SilverMute
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel

@Composable
fun SettingsTab(
    libraryViewModel: LibraryViewModel,
    readerViewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by readerViewModel.readerSettings.collectAsState()
    val isScanning by libraryViewModel.isScanning.collectAsState()
    val activeAccent by libraryViewModel.activeAccent.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        libraryViewModel.triggerStorageScan()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 100.dp)
            .testTag("settings_tab")
    ) {
        // Title
        Text(
            text = "Settings",
            color = BoneWhite,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Reader preferences, typography, themes & device storage",
            color = SilverMute,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Saturated Accent Colors (14 Options)
        SectionCard(title = "Saturated Accent Palette (14 Vibrant Options)", icon = Icons.Default.ColorLens, accent = activeAccent.color) {
            Text(
                text = "Select your high-contrast saturated theme accent:",
                color = SilverMute,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Palette Grid
            val accentPairs = remember { SaturatedAccent.values().toList().chunked(2) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accentPairs.forEach { rowAccents ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowAccents.forEach { accent ->
                            val isSelected = activeAccent == accent
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) SurfaceVariantDark else SurfaceDark)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) accent.color else OutlineDark,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { libraryViewModel.setAccent(accent) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(accent.color)
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = accent.displayName,
                                        color = if (isSelected) BoneWhite else SilverMute,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = accent.hexString,
                                        color = accent.color,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Reading Theme Section
        SectionCard(title = "Default Reading Canvas", icon = Icons.Default.Palette, accent = activeAccent.color) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReaderThemeMode.values().forEach { mode ->
                    val isSelected = settings.themeMode == mode
                    val (bgColor, borderColor) = when (mode) {
                        ReaderThemeMode.VOID_DARK -> Pair(VoidBlack, if (isSelected) activeAccent.color else OutlineDark)
                        ReaderThemeMode.OLED_BLACK -> Pair(OledBlack, if (isSelected) activeAccent.color else OutlineDark)
                        ReaderThemeMode.RULES_PARCHMENT -> Pair(PaperParchment, if (isSelected) BloodCrimson else OutlineDark)
                        ReaderThemeMode.SEPIA_WARM -> Pair(SepiaBackground, if (isSelected) activeAccent.color else OutlineDark)
                        ReaderThemeMode.CLEAN_LIGHT -> Pair(Color.White, if (isSelected) activeAccent.color else OutlineDark)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                            .clickable { readerViewModel.updateThemeMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (mode == ReaderThemeMode.SEPIA_WARM || mode == ReaderThemeMode.CLEAN_LIGHT) Color.Black else BoneWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Customizable Fonts Section
        SectionCard(title = "Customizable Fonts & Typography", icon = Icons.Default.FormatSize, accent = activeAccent.color) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReaderFont.values().forEach { font ->
                    val isSelected = settings.font == font
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) activeAccent.color else SurfaceVariantDark)
                            .border(1.dp, if (isSelected) activeAccent.color else OutlineDark, RoundedCornerShape(8.dp))
                            .clickable { readerViewModel.updateFont(font) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = font.displayName,
                            color = BoneWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = when (font) {
                                ReaderFont.SERIF -> FontFamily.Serif
                                ReaderFont.MONO -> FontFamily.Monospace
                                ReaderFont.SANS -> FontFamily.SansSerif
                                ReaderFont.GOTHIC -> FontFamily.Serif
                                ReaderFont.CURSIVE -> FontFamily.Cursive
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Font size slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Font Size", color = SilverMute, fontSize = 12.sp)
                Text(
                    text = "${settings.fontSize} sp",
                    color = activeAccent.color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = settings.fontSize.toFloat(),
                onValueChange = { readerViewModel.updateFontSize(it.toInt()) },
                valueRange = 12f..32f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = activeAccent.color,
                    activeTrackColor = activeAccent.color,
                    inactiveTrackColor = SurfaceVariantDark
                )
            )

            // Live preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(VoidBlack)
                    .border(1.dp, OutlineDark, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "“The human whose name is written in this note shall die.”",
                    color = BoneWhite,
                    fontSize = settings.fontSize.sp,
                    fontFamily = when (settings.font) {
                        ReaderFont.SERIF -> FontFamily.Serif
                        ReaderFont.MONO -> FontFamily.Monospace
                        ReaderFont.SANS -> FontFamily.SansSerif
                        ReaderFont.GOTHIC -> FontFamily.Serif
                        ReaderFont.CURSIVE -> FontFamily.Cursive
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Reading Direction Section
        SectionCard(title = "Default Reading Direction", icon = Icons.Default.SwapHoriz, accent = activeAccent.color) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ReadingDirection.values().forEach { dir ->
                    val isSelected = settings.readingDirection == dir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                            .border(1.dp, if (isSelected) activeAccent.color else OutlineDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { readerViewModel.updateReadingDirection(dir) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dir.displayName,
                            color = if (isSelected) BoneWhite else SilverMute,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(activeAccent.color)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. File Formats & Storage Permissions
        SectionCard(title = "File Permissions & Formats", icon = Icons.Default.Security, accent = activeAccent.color) {
            Text(
                text = "Supported Formats: .CBZ, .CBR, .PDF, .ZIP, .EPUB, .TXT",
                color = BoneWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "VLC-style scanning searches internal and external directories for all folders containing comics and books.",
                color = SilverMute,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (!Environment.isExternalStorageManager()) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                context.startActivity(intent)
                            }
                        }
                    } else {
                        permissionLauncher.launch(
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        )
                    }
                    libraryViewModel.triggerStorageScan()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = activeAccent.color),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BoneWhite, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scanning storage...")
                } else {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check / Grant Permission & Scan Storage")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. 100% Offline Notice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, Color(0xFF2E2E3A), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "100% OFFLINE & NO MOCK DATA",
                        color = BoneWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Zero online tracking and zero mock data. The app only displays actual comic and book files from your storage.",
                        color = SilverMute,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, OutlineDark, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = BoneWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}
