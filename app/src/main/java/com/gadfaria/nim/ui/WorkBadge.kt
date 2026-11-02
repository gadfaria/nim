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

/** Width reserved for the badge on EVERY row, badged or not. */
val WorkBadgeSlot = 22.dp

/**
 * Work profile badge: a small drawn briefcase, not an app icon.
 *
 * This doesn't contradict the plan's "no icons" decision — what was dropped
 * there was loading and caching an icon PER APP, with adaptive icons and the
 * whole bundle of performance pain. This is a single vector shape, shared by
 * every app in the profile.
 */
@Composable
fun WorkBadge(modifier: Modifier = Modifier) {
    Canvas(modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.13f
        val bodyTop = h * 0.32f

        // handle
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

        // body
        drawRoundRect(
            color = NimForegroundMuted,
            topLeft = Offset(stroke / 2, bodyTop),
            size = Size(w - stroke, h - bodyTop - stroke / 2),
            cornerRadius = CornerRadius(stroke * 1.6f),
            style = Stroke(width = stroke),
        )
    }
}
