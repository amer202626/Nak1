package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun parseColorSafe(hex: String, defaultColor: Color): Color {
    if (hex.isEmpty()) return defaultColor
    return try {
        // Handle shorthand or standard hex formats
        val cleaned = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(cleaned))
    } catch (e: Exception) {
        defaultColor
    }
}

@Composable
fun WamServicesTheme(
    primaryHex: String,
    secondaryHex: String,
    content: @Composable () -> Unit
) {
    // Determine dynamic background based on theme presets, or auto-calculate card/background depth
    val primaryColor = parseColorSafe(primaryHex, DefaultPrimary)
    val secondaryColor = parseColorSafe(secondaryHex, DefaultSecondary)
    
    // Choose backing background
    val backgroundColor = when (primaryHex.lowercase()) {
        "#eceff1", "#b0bec5" -> CosmicSilverBackground
        "#ffd700", "#ffeb3b" -> GoldLuxuryBackground
        "#00c853", "#2e7d32" -> EmeraldBackground
        else -> {
            // Calculated dark background
            Color(0xFF121212)
        }
    }

    val customColorScheme = darkColorScheme(
        primary = primaryColor,
        secondary = secondaryColor,
        background = backgroundColor,
        surface = Color(0xFF1E262C),
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
        primaryContainer = secondaryColor.copy(alpha = 0.3f),
        secondaryContainer = primaryColor.copy(alpha = 0.2f)
    )

    MaterialTheme(
        colorScheme = customColorScheme,
        typography = Typography,
        content = content
    )
}
