package com.gadfaria.nim.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * Os dois gestos da home — arrastar pra cima abre a gaveta, segurar abre as
 * configurações — num detector só.
 *
 * Empilhar `detectVerticalDragGestures` e `detectTapGestures` no mesmo Box não
 * funciona: os dois recebem o mesmo toque, disputam quem consome, e o arrasto
 * passa a falhar de forma intermitente. Aqui o próprio gesto decide o que é:
 * saiu do slop antes do tempo, é arrasto; ficou parado até estourar, é toque
 * longo.
 */
fun Modifier.homeGestures(
    swipeUpThresholdPx: Float,
    onSwipeUp: () -> Unit,
    onLongPress: () -> Unit,
): Modifier = pointerInput(swipeUpThresholdPx) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalY = 0f

        // null = estourou o tempo com o dedo parado (toque longo)
        // true = virou arrasto
        // false = soltou antes de qualquer um dos dois (toque simples)
        val becameDrag = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var drag = false
            while (true) {
                val change = awaitPointerEvent().changes
                    .firstOrNull { it.id == down.id } ?: break
                totalY += change.positionChange().y
                if (!change.pressed) break
                if (abs(totalY) > viewConfiguration.touchSlop) {
                    drag = true
                    break
                }
            }
            drag
        }

        when (becameDrag) {
            null -> {
                // Down já consumido = o toque começou numa linha de app, que
                // tem o próprio toque longo (o menu). O timeout daqui corre em
                // paralelo ao dela, então sem esta checagem os dois disparavam:
                // o menu abria por cima das configurações, e voltar caía nelas.
                if (!down.isConsumed) onLongPress()
                consumeUntilUp(down.id)
            }

            true -> {
                while (true) {
                    val change = awaitPointerEvent().changes
                        .firstOrNull { it.id == down.id } ?: break
                    totalY += change.positionChange().y
                    change.consume()
                    if (!change.pressed) break
                }
                if (totalY < -swipeUpThresholdPx) onSwipeUp()
            }

            false -> Unit
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilUp(pointer: PointerId) {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer } ?: break
        change.consume()
        if (!change.pressed) break
    }
}
