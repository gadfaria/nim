package com.gadfaria.nim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.ui.theme.Accent
import com.gadfaria.nim.ui.theme.Foreground
import com.gadfaria.nim.ui.theme.MutedForeground
import com.gadfaria.nim.ui.theme.RadiusMd
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RowHeight = 48.dp

@Composable
fun SettingsScreen(
    favorites: List<AppInfo>,
    onReorder: (List<AppInfo>) -> Unit,
    onApplyWallpaper: suspend () -> Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 32.dp),
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineMedium,
            color = Foreground,
            modifier = Modifier.padding(horizontal = CardTextInset),
        )

        Spacer(Modifier.height(24.dp))

        NimCard {
            GroupLabel("Favoritos")

            if (favorites.isEmpty()) {
                Description("Nenhum ainda. Segure um app na gaveta para favoritar.")
            } else {
                ReorderableFavorites(favorites = favorites, onReorder = onReorder)
                Separator()
                Description("Segure e arraste para reordenar.")
            }
        }

        Spacer(Modifier.height(16.dp))

        WallpaperSection(onApply = onApplyWallpaper)
    }
}

/** CardDescription: muted body text inside a card. */
@Composable
private fun Description(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MutedForeground,
        modifier = Modifier.padding(horizontal = ItemPadding, vertical = 8.dp),
    )
}

/**
 * An action, not a toggle: once applied the wallpaper stays, and Nim has no way
 * to restore the previous one — reading the current wallpaper requires a storage
 * permission a text-only launcher shouldn't ask for. To go back, the user picks
 * another one through Android, as usual.
 */
@Composable
private fun WallpaperSection(onApply: suspend () -> Boolean) {
    val scope = rememberCoroutineScope()
    // null = not tapped yet; otherwise, the result of the last attempt.
    var applied by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }

    NimCard {
        GroupLabel("Papel de parede")

        NimItem(
            text = "Usar o fundo do Nim na tela inicial",
            enabled = !busy,
            onClick = {
                busy = true
                scope.launch {
                    applied = onApply()
                    busy = false
                }
            },
        )

        Separator()

        Description(
            when (applied) {
                null -> "Os apps recentes mostram o papel de parede por trás. " +
                    "Isto troca só o da tela inicial; o da tela de bloqueio fica."
                true -> "Pronto. Para voltar, escolha outro papel de parede pelo Android."
                false -> "O Android não deixou trocar o papel de parede."
            },
        )
    }
}

/**
 * Drag to reorder, with no library and no LazyColumn.
 *
 * Favorites are half a dozen at most, so a plain Column fits entirely on screen
 * and the index math becomes a division: fixed row height, offset divided by the
 * height, and the swap happens when crossing each row.
 */
@Composable
private fun ReorderableFavorites(
    favorites: List<AppInfo>,
    onReorder: (List<AppInfo>) -> Unit,
) {
    // Local copy so the drag can move items on every crossing without waiting
    // for a DataStore round trip per pixel. It's only written back when the
    // finger lifts.
    //
    // remember compares by content: when the flow returns the same order we just
    // wrote, nothing resets and the list doesn't flicker.
    var items by remember(favorites) { mutableStateOf(favorites) }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }
    val haptics = LocalHapticFeedback.current

    Column(Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, app ->
            // The key makes Compose MOVE the node instead of recreating it when
            // the order changes. Without it the slot would be reused by position,
            // pointerInput would restart and the drag would die on the first
            // crossing.
            key(app.key) {
                val dragging = index == draggingIndex

                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    // Normally all lit. During a drag the others dim and the one
                    // in hand gets the accent background, as if lifted.
                    color = if (draggingIndex == -1 || dragging) Foreground else MutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RowHeight)
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                        .background(if (dragging) Accent else Color.Transparent, RadiusMd)
                        .wrapContentHeight()
                        .padding(horizontal = ItemPadding)
                        .pointerInput(app.key) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffset = 0f
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    draggingIndex = -1
                                    dragOffset = 0f
                                    onReorder(items)
                                },
                                onDragCancel = {
                                    draggingIndex = -1
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y

                                    val steps = (dragOffset / rowHeightPx).roundToInt()
                                    if (steps == 0) return@detectDragGesturesAfterLongPress

                                    val from = draggingIndex
                                    val to = (from + steps).coerceIn(0, items.lastIndex)
                                    if (to == from) return@detectDragGesturesAfterLongPress

                                    items = items.toMutableList()
                                        .apply { add(to, removeAt(from)) }
                                    draggingIndex = to
                                    // Subtract what already became a swap, or
                                    // the item runs away from the finger.
                                    dragOffset -= (to - from) * rowHeightPx
                                },
                            )
                        },
                )
            }
        }
    }
}
