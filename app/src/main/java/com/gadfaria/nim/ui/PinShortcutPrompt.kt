package com.gadfaria.nim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.R
import com.gadfaria.nim.ui.theme.Background
import com.gadfaria.nim.ui.theme.Foreground
import com.gadfaria.nim.ui.theme.MutedForeground

/**
 * Confirmation for a pin-shortcut request, in the same shape as [AppMenu]: a
 * card at the bottom with the shortcut name as title.
 *
 * It names the app that asked: the shortcut label is chosen by that app, so the
 * label alone says nothing about where the shortcut leads.
 */
@Composable
fun PinShortcutPrompt(
    label: String,
    requester: String,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val noRipple = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background.copy(alpha = 0.92f))
            // Tapping outside cancels.
            .clickable(interactionSource = noRipple, indication = null, onClick = onCancel)
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        NimCard(
            // Swallows taps on the card's own padding and text, which would
            // otherwise reach the cancel handler behind it.
            Modifier.clickable(interactionSource = noRipple, indication = null) {},
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = Foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = ItemPadding, vertical = 8.dp),
            )
            Text(
                text = stringResource(R.string.pin_requested_by, requester),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedForeground,
                modifier = Modifier.padding(horizontal = ItemPadding, vertical = 8.dp),
            )
            NimItem(text = stringResource(R.string.pin_accept), onClick = onAccept)
            Separator()
            NimItem(text = stringResource(R.string.pin_cancel), onClick = onCancel)
        }
    }
}
