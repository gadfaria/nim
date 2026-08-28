package com.gadfaria.nim.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.ui.theme.NimForegroundMuted
import kotlin.math.roundToInt

private val RowHeight = 56.dp

@Composable
fun SettingsScreen(
    favorites: List<AppInfo>,
    onReorder: (List<AppInfo>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp),
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Favoritos",
            style = MaterialTheme.typography.bodyLarge,
            color = NimForegroundMuted,
        )

        Spacer(Modifier.height(8.dp))

        if (favorites.isEmpty()) {
            Text(
                text = "Nenhum ainda. Segure um app na gaveta para favoritar.",
                style = MaterialTheme.typography.bodyLarge,
                color = NimForegroundMuted,
                modifier = Modifier.padding(vertical = 14.dp),
            )
        } else {
            ReorderableFavorites(favorites = favorites, onReorder = onReorder)

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Segure e arraste para reordenar.",
                style = MaterialTheme.typography.bodyLarge,
                color = NimForegroundMuted,
            )
        }
    }
}

/**
 * Reordenação por arrasto, sem biblioteca e sem LazyColumn.
 *
 * Favorito é coisa de meia dúzia, então uma Column simples cabe inteira na tela
 * e a matemática do índice vira uma divisão: linha de altura fixa, deslocamento
 * dividido pela altura, e a troca acontece ao cruzar cada linha.
 */
@Composable
private fun ReorderableFavorites(
    favorites: List<AppInfo>,
    onReorder: (List<AppInfo>) -> Unit,
) {
    // Cópia local para o arrasto mexer a cada cruzamento, sem esperar o
    // DataStore ir e voltar a cada pixel. Só é regravada quando o dedo levanta.
    //
    // O remember compara por conteúdo: quando o fluxo devolve a mesma ordem que
    // acabamos de gravar, nada é reiniciado e a lista não pisca.
    var items by remember(favorites) { mutableStateOf(favorites) }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }
    val haptics = LocalHapticFeedback.current

    Column(Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, app ->
            // A key faz o Compose MOVER o nó em vez de recriá-lo quando a ordem
            // muda. Sem ela o slot seria reaproveitado por posição, o
            // pointerInput reiniciaria e o arrasto morreria no primeiro cruzamento.
            key(app.key) {
                val dragging = index == draggingIndex

                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    // Normalmente todas acesas. Durante um arrasto as outras
                    // apagam, isolando visualmente a que está na mão.
                    color = if (draggingIndex == -1 || dragging) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        NimForegroundMuted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RowHeight)
                        .wrapContentHeight()
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
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
                                    // Desconta o que já virou troca, senão o
                                    // item foge do dedo.
                                    dragOffset -= (to - from) * rowHeightPx
                                },
                            )
                        },
                )
            }
        }
    }
}
