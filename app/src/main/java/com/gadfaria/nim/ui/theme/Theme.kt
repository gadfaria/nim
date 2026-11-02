package com.gadfaria.nim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Solid background, not the wallpaper. Neither #000 nor #FFF: pure black punches
// a harsh hole on OLED and pure white vibrates on top of it. A very dark gray
// and a slightly warm white give the same contrast without the harshness.
val NimBackground = Color(0xFF121212)
val NimForeground = Color(0xFFE8E6E3)

// The date is the same color with less presence — hierarchy without inventing
// a third color in the palette.
val NimForegroundMuted = Color(0x99E8E6E3)

private val NimColors = darkColorScheme(
    background = NimBackground,
    surface = NimBackground,
    onBackground = NimForeground,
    onSurface = NimForeground,
    primary = NimForeground,
)

// Typography is the whole interface — without icons, font weight and size are
// the only visual tool left.
val NimTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        letterSpacing = (-2).sp,
    ),
    // Screen title: much smaller than the clock, which is the app's only display.
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
    ),
)

/**
 * Nim is always dark. It doesn't follow the system light/dark theme because the
 * background is the launcher's decision, not a device preference.
 */
@Composable
fun NimTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NimColors,
        typography = NimTypography,
        content = content,
    )
}
