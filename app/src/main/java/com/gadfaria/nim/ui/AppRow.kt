package com.gadfaria.nim.ui

import android.graphics.Rect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.data.AppInfo

/**
 * Uma linha de app. Serve tanto à gaveta quanto aos favoritos da home — nas
 * duas o toque lança e o toque longo alterna o favorito.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppInfo,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Guardado numa var em vez de estado: só é lido no clique, e virar estado
    // recomporia a linha a cada scroll.
    var coordinates: LayoutCoordinates? = remember { null }
    val haptics = LocalHapticFeedback.current

    Text(
        text = app.label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates = it }
            .combinedClickable(
                // A animação de abertura sai de onde o dedo tocou, não do
                // centro da tela.
                onClick = { onLaunch(app, coordinates?.launchBounds()) },
                onLongClick = {
                    // Sem ícones e sem menu, o toque longo não tem retorno
                    // visual imediato — a vibração é o único aviso de que
                    // alguma coisa aconteceu.
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleFavorite(app)
                },
            )
            .padding(vertical = 14.dp),
    )
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
