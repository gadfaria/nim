package com.gadfaria.nim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.ui.theme.NimForegroundMuted

/** Largura reservada pra marca em TODA linha, tenha ela marca ou não. */
val WorkBadgeSlot = 22.dp

/**
 * Marca de perfil de trabalho: uma pastinha desenhada, não um ícone de app.
 *
 * Isso não contradiz a decisão de "sem ícones" do plano — o que foi descartado
 * lá era carregar e cachear um ícone POR APP, com adaptive icons e o pacote
 * inteiro de dor de performance. Aqui é uma forma só, vetorial, compartilhada
 * por todos os apps do perfil.
 */
@Composable
fun WorkBadge(modifier: Modifier = Modifier) {
    Canvas(modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.13f
        val bodyTop = h * 0.32f

        // alça
        drawLine(
            color = NimForegroundMuted,
            start = Offset(w * 0.33f, bodyTop),
            end = Offset(w * 0.33f, h * 0.16f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = NimForegroundMuted,
            start = Offset(w * 0.67f, bodyTop),
            end = Offset(w * 0.67f, h * 0.16f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = NimForegroundMuted,
            start = Offset(w * 0.33f, h * 0.16f),
            end = Offset(w * 0.67f, h * 0.16f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        // corpo
        drawRoundRect(
            color = NimForegroundMuted,
            topLeft = Offset(stroke / 2, bodyTop),
            size = Size(w - stroke, h - bodyTop - stroke / 2),
            cornerRadius = CornerRadius(stroke * 1.6f),
            style = Stroke(width = stroke),
        )
    }
}
