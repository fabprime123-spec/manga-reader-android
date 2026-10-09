package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// 14 Saturated Vibrant Accent Colors
enum class SaturatedAccent(
    val displayName: String,
    val color: Color,
    val hexString: String
) {
    CRIMSON_SHINIGAMI("Crimson Shinigami", Color(0xFFE50914), "#E50914"),
    NEON_BLAZE("Neon Blaze", Color(0xFFFF1744), "#FF1744"),
    SOLAR_FLARE("Solar Flare", Color(0xFFFF5722), "#FF5722"),
    AMBER_CORE("Amber Core", Color(0xFFFF9100), "#FF9100"),
    RYUK_GOLD("Ryuk Gold", Color(0xFFFFD600), "#FFD600"),
    TOXIC_ACID("Toxic Acid", Color(0xFF00E676), "#00E676"),
    BIO_EMERALD("Bio Emerald", Color(0xFF00C853), "#00C853"),
    CYAN_RAY("Cyan Ray", Color(0xFF00E5FF), "#00E5FF"),
    KIRA_COBALT("Kira Cobalt", Color(0xFF2979FF), "#2979FF"),
    ABYSSAL_VIOLET("Abyssal Violet", Color(0xFF651FFF), "#651FFF"),
    GOTHIC_NEON("Gothic Neon", Color(0xFFD500F9), "#D500F9"),
    POISON_MAGENTA("Poison Magenta", Color(0xFFF50057), "#F50057"),
    PHANTOM_ROSE("Phantom Rose", Color(0xFFC51162), "#C51162"),
    HYPER_TURQUOISE("Hyper Turquoise", Color(0xFF1DE9B6), "#1DE9B6")
}

// Format specific saturated badges
val FormatCbzColor = Color(0xFFE50914)   // Crimson
val FormatCbrColor = Color(0xFFD500F9)   // Neon Purple
val FormatPdfColor = Color(0xFFFF1744)   // Neon Red
val FormatZipColor = Color(0xFFFF9100)   // Vibrant Orange
val FormatEpubColor = Color(0xFF2979FF)  // Electric Blue
val FormatTxtColor = Color(0xFF00E5FF)   // Cyan Ray

fun getFormatColor(format: String): Color {
    return when (format.uppercase()) {
        "CBZ" -> FormatCbzColor
        "CBR" -> FormatCbrColor
        "PDF" -> FormatPdfColor
        "ZIP" -> FormatZipColor
        "EPUB" -> FormatEpubColor
        "TXT", "TEXT", "MD" -> FormatTxtColor
        else -> FormatCbzColor
    }
}
