package com.gadfaria.nim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.R
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.ui.theme.Background
import com.gadfaria.nim.ui.theme.Foreground

/**
 * The long-press menu, shaped like shadcn's DialogContent: a card with the app
 * name as title and the destructive action set apart.
 *
 * The card sits at the bottom, within thumb reach — the finger is already
 * there when the menu opens.
 */
@Composable
fun AppMenu(
    app: AppInfo,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val noRipple = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxSize()
            // Nearly opaque, unlike shadcn's light overlay: with a see-through
            // scrim the drawer rows above the card read like extra options.
            .background(Background.copy(alpha = 0.92f))
            // Tapping outside closes it.
            .clickable(interactionSource = noRipple, indication = null, onClick = onDismiss)
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        NimCard(
            // Swallows taps on the card's own padding and title, which would
            // otherwise reach the dismiss handler behind it.
            Modifier.clickable(interactionSource = noRipple, indication = null) {},
        ) {
            // DialogTitle: font-medium, one step above the items.
            Text(
                text = app.label,
                style = MaterialTheme.typography.headlineMedium,
                color = Foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = ItemPadding, vertical = 8.dp),
            )
            NimItem(
                text = stringResource(
                    if (isFavorite) R.string.menu_unfavorite else R.string.menu_favorite,
                ),
                onClick = onToggleFavorite,
            )
            NimItem(text = stringResource(R.string.menu_app_info), onClick = onAppInfo)
            Separator()
            NimItem(
                text = stringResource(
                    if (app.isShortcut) R.string.menu_remove_shortcut else R.string.menu_uninstall,
                ),
                onClick = onUninstall,
                destructive = true,
            )
        }
    }
}
