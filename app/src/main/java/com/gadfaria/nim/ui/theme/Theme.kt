package com.gadfaria.nim.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gadfaria.nim.R

// shadcn/ui "neutral" dark tokens, the same as hub's src/index.css (.dark) and
// Nim TV, converted from oklch to sRGB.
val Background = Color(0xFF0A0A0A)      // --background  oklch(0.145 0 0)
val Foreground = Color(0xFFFAFAFA)      // --foreground  oklch(0.985 0 0)
val Accent = Color(0xFF262626)          // --accent / --muted / --secondary  oklch(0.269 0 0)
val MutedForeground = Color(0xFFA1A1A1) // --muted-foreground  oklch(0.708 0 0)
val Border = Color(0x1AFFFFFF)          // --border  oklch(1 0 0 / 10%)
val InputBorder = Color(0x26FFFFFF)     // --input  oklch(1 0 0 / 15%)
val Ring = Color(0xFF737373)            // --ring  oklch(0.556 0 0)
val Destructive = Color(0xFFFF6467)     // --destructive  oklch(0.704 0.191 22.216)

// --radius: 0.5rem, with shadcn's scale: md = 0.8x, lg = 1x, xl = 1.4x.
val RadiusMd = RoundedCornerShape(6.dp)
val RadiusLg = RoundedCornerShape(8.dp)
val RadiusXl = RoundedCornerShape(12.dp)

private val Geist = FontFamily(
    Font(R.font.geist_light, FontWeight.Light),
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
)

private val NimColors = darkColorScheme(
    background = Background,
    surface = Background,
    onBackground = Foreground,
    onSurface = Foreground,
    primary = Foreground,
    onPrimary = Background,
    error = Destructive,
)

// Typography is the whole interface — without icons, font weight and size are
// the only visual tool left.
val NimTypography = Typography(
    // The clock: the app's only display text.
    displayLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        letterSpacing = (-2).sp,
    ),
    // Screen and card titles: font-medium tracking-tight, like CardTitle.
    headlineMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        letterSpacing = (-0.5).sp,
    ),
    // App rows and menu items.
    titleMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
    ),
    // Secondary text: the date, descriptions.
    bodyLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    // Group labels, like SidebarGroupLabel (text-xs font-medium, muted).
    labelMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    ),
)

/**
 * Nim is always dark. It doesn't follow the system light/dark theme because the
 * background is the launcher's decision, not a device preference.
 */
@Composable
fun NimTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NimColors, typography = NimTypography, content = content)
}
