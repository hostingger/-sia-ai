package com.openjarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object VoidColor {
    val Void950 = Color(0xFF080812)
    val Void900 = Color(0xFF10101E)
    val Void800 = Color(0xFF19192B)
    val Void600 = Color(0xFF34344A)
    val Violet = Color(0xFF9B7BFF)
    val VioletDim = Color(0xFF6F55C8)
    val Cyan = Color(0xFF55D6E8)
    val Green = Color(0xFF4ADE80)
    val Amber = Color(0xFFFBBF24)
    val Red = Color(0xFFF87171)
    val TextPrimary = Color(0xFFF5F3FF)
    val TextSecondary = Color(0xFFB8B5C9)
    val TextDisabled = Color(0xFF777489)
    val BorderGlow = Color(0x669B7BFF)
    val BorderSubtle = Color(0x333F3F55)
}

private val JarvisDarkColors = darkColorScheme(
    primary = VoidColor.Violet,
    secondary = VoidColor.Cyan,
    background = VoidColor.Void950,
    surface = VoidColor.Void900,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = VoidColor.TextPrimary,
    onSurface = VoidColor.TextPrimary
)

@Composable
fun OpenJarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisDarkColors,
        content = content
    )
}
