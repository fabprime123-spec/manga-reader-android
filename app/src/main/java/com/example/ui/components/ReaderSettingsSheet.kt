package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.SepiaBackground
import com.example.ui.theme.ShinigamiRed
import com.example.ui.theme.SilverMute
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.VoidBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onFontChange: (ReaderFont) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onThemeChange: (ReaderThemeMode) -> Unit,
    onDirectionChange: (ReadingDirection) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("reader_settings_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "READER CUSTOMIZATION",
                    color = BoneWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 1.5.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SilverMute)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Theme Color Selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = ShinigamiRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Reading Theme", color = BoneWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReaderThemeMode.values().forEach { mode ->
                    val isSelected = settings.themeMode == mode
                    val (bgColor, borderColor) = when (mode) {
                        ReaderThemeMode.VOID_DARK -> Pair(VoidBlack, if (isSelected) ShinigamiRed else OutlineDark)
                        ReaderThemeMode.OLED_BLACK -> Pair(OledBlack, if (isSelected) ShinigamiRed else OutlineDark)
                        ReaderThemeMode.RULES_PARCHMENT -> Pair(PaperParchment, if (isSelected) BloodCrimson else OutlineDark)
                        ReaderThemeMode.SEPIA_WARM -> Pair(SepiaBackground, if (isSelected) ShinigamiRed else OutlineDark)
                        ReaderThemeMode.CLEAN_LIGHT -> Pair(Color.White, if (isSelected) ShinigamiRed else OutlineDark)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                            .clickable { onThemeChange(mode) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
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

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Font Family Selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.FormatSize, contentDescription = null, tint = ShinigamiRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Typography & Font", color = BoneWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
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
                            .background(if (isSelected) ShinigamiRed else SurfaceVariantDark)
                            .border(1.dp, if (isSelected) ShinigamiRed else OutlineDark, RoundedCornerShape(8.dp))
                            .clickable { onFontChange(font) }
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

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Font Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Font Size", color = SilverMute, fontSize = 12.sp)
                Text(text = "${settings.fontSize} sp", color = ShinigamiRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = settings.fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange = 12f..32f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = ShinigamiRed,
                    activeTrackColor = ShinigamiRed,
                    inactiveTrackColor = SurfaceVariantDark
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Reading Direction Selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = ShinigamiRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Reading Mode / Direction", color = BoneWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ReadingDirection.values().forEach { dir ->
                    val isSelected = settings.readingDirection == dir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                            .border(1.dp, if (isSelected) ShinigamiRed else OutlineDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { onDirectionChange(dir) }
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
                                    .background(ShinigamiRed)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
