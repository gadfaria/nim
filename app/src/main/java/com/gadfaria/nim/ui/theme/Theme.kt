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

// Fundo sólido, não o papel de parede. Nem #000 nem #FFF: o preto puro abre um
// buraco duro em OLED e o branco puro vibra em cima dele. Um cinza muito escuro
// e um branco levemente quente dão o mesmo contraste sem a agressividade.
val NimBackground = Color(0xFF121212)
val NimForeground = Color(0xFFE8E6E3)

// A data é a mesma cor com menos presença — hierarquia sem inventar uma
// terceira tinta na paleta.
val NimForegroundMuted = Color(0x99E8E6E3)

private val NimColors = darkColorScheme(
    background = NimBackground,
    surface = NimBackground,
    onBackground = NimForeground,
    onSurface = NimForeground,
    primary = NimForeground,
)

// Tipografia é a interface inteira — sem ícones, o peso e o tamanho da fonte
// são o único recurso visual que resta.
val NimTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        letterSpacing = (-2).sp,
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
 * O Nim é sempre escuro. Não segue o tema claro/escuro do sistema porque o
 * fundo é decisão do launcher, não preferência do aparelho.
 */
@Composable
fun NimTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NimColors,
        typography = NimTypography,
        content = content,
    )
}
