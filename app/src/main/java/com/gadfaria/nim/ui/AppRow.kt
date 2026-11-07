package com.gadfaria.nim.ui

import android.graphics.Rect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.ui.theme.Foreground
import com.gadfaria.nim.ui.theme.MutedForeground

/**
 * An app row. Used by the drawer and the home favorites — in both, a tap
 * launches and a long press opens the menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppInfo,
    isFavorite: Boolean,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onOpenMenu: (AppInfo, Rect?) -> Unit,
    modifier: Modifier = Modifier,
    highlightOnPress: Boolean = true,
) {
    // Kept in a var instead of state: it's only read on click, and making it
    // state would recompose the row on every scroll.
    var coordinates: LayoutCoordinates? = remember { null }
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates = it }
            .then(if (highlightOnPress) Modifier.pressHighlight(interaction) else Modifier)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                // The opening animation starts where the finger touched, not
                // from the center of the screen.
                onClick = { onLaunch(app, coordinates?.launchBounds()) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenMenu(app, coordinates?.launchBounds())
                },
            )
            .padding(horizontal = ItemPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            color = Foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )

        // Badge and dot go after the label, so every name starts on the same
        // column as the search field and the card labels.
        if (app.isWork) {
            Spacer(Modifier.width(10.dp))
            WorkBadge()
        }

        // The favorite marker is a dot, not an icon: it's the only way to
        // answer "is this one already on the home?" without betraying the
        // no-icons decision.
        if (isFavorite) {
            Text(
                text = "  ·",
                style = MaterialTheme.typography.titleMedium,
                color = MutedForeground,
            )
        }
    }
}

private fun LayoutCoordinates.launchBounds(): Rect? {
    if (!isAttached) return null
    val bounds = boundsInWindow()
    return Rect(
        bounds.left.toInt(),
        bounds.top.toInt(),
        bounds.right.toInt(),
        bounds.bottom.toInt(),
    )
}
