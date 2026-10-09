package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun createDarkColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = PureWhite,
    primaryContainer = BloodCrimson,
    onPrimaryContainer = BoneWhite,
    secondary = BoneWhite,
    onSecondary = VoidBlack,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = BoneWhite,
    tertiary = accent,
    onTertiary = PureWhite,
    background = VoidBlack,
    onBackground = BoneWhite,
    surface = SurfaceDark,
    onSurface = BoneWhite,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = SilverMute,
    outline = OutlineDark,
    outlineVariant = DarkCharcoal
)

private fun createLightColorScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = PureWhite,
    secondary = VoidBlack,
    onSecondary = PureWhite,
    background = Color(0xFFF9F8F6),
    onBackground = VoidBlack,
    surface = PureWhite,
    onSurface = VoidBlack,
    surfaceVariant = Color(0xFFEBE8E1),
    onSurfaceVariant = Color(0xFF4A4A52),
    outline = Color(0xFFC7C4BC)
)

@Composable
fun DeathReaderTheme(
    darkTheme: Boolean = true,
    accent: SaturatedAccent = SaturatedAccent.CRIMSON_SHINIGAMI,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        createDarkColorScheme(accent.color)
    } else {
        createLightColorScheme(accent.color)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
