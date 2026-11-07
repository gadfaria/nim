package com.gadfaria.nim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.ui.theme.Accent
import com.gadfaria.nim.ui.theme.Background
import com.gadfaria.nim.ui.theme.Border
import com.gadfaria.nim.ui.theme.Destructive
import com.gadfaria.nim.ui.theme.Foreground
import com.gadfaria.nim.ui.theme.MutedForeground
import com.gadfaria.nim.ui.theme.RadiusMd
import com.gadfaria.nim.ui.theme.RadiusXl

/** Horizontal padding inside an item, so labels and item text share one column. */
val ItemPadding = 12.dp

private val CardPadding = 4.dp

/** Where text sits inside a [NimCard] (padding + item padding), to align text outside it. */
val CardTextInset = CardPadding + ItemPadding

/** shadcn's Card: rounded-xl, bg-card, ring-1 ring-foreground/10. */
@Composable
fun NimCard(
    modifier: Modifier = Modifier,
    bordered: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Background, RadiusXl)
            .then(if (bordered) Modifier.border(1.dp, Border, RadiusXl) else Modifier)
            .padding(CardPadding),
        content = content,
    )
}

/** SidebarGroupLabel: small, medium weight, muted. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MutedForeground,
        modifier = modifier.padding(horizontal = ItemPadding, vertical = 8.dp),
    )
}

/** shadcn's Separator: a 1dp line in the border color. */
@Composable
fun Separator(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(Border),
    )
}

/**
 * shadcn's hover/active state, mapped to touch: accent background while the
 * finger is down. Inside a LazyColumn the press is delayed until the gesture
 * is clearly not a scroll, so scrolling doesn't flash every row.
 */
@Composable
fun Modifier.pressHighlight(
    interactionSource: InteractionSource,
    destructive: Boolean = false,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val color = when {
        !pressed -> Color.Transparent
        // bg-destructive/10, as in shadcn's destructive menu item.
        destructive -> Destructive.copy(alpha = 0.1f)
        else -> Accent
    }
    return clip(RadiusMd).background(color)
}

/** SidebarMenuButton / DropdownMenuItem: a tappable line of text. */
@Composable
fun NimItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (destructive) Destructive else Foreground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .pressHighlight(interaction, destructive)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = ItemPadding, vertical = 12.dp),
    )
}
