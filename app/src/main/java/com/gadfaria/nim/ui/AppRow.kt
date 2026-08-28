package com.gadfaria.nim.ui

import android.graphics.Rect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.gadfaria.nim.ui.theme.NimForegroundMuted

/**
 * Uma linha de app. Serve à gaveta e aos favoritos da home — nas duas o toque
 * lança e o toque longo abre o menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppInfo,
    isFavorite: Boolean,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onOpenMenu: (AppInfo, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Guardado numa var em vez de estado: só é lido no clique, e virar estado
    // recomporia a linha a cada scroll.
    var coordinates: LayoutCoordinates? = remember { null }
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates = it }
            .combinedClickable(
                // A animação de abertura sai de onde o dedo tocou, não do
                // centro da tela.
                onClick = { onLaunch(app, coordinates?.launchBounds()) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenMenu(app, coordinates?.launchBounds())
                },
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A marca fica na frente, mas o espaço dela é reservado em TODA linha:
        // sem isso os nomes dos apps de trabalho ficariam deslocados à direita
        // dos demais, e a coluna perderia o alinhamento.
        Box(Modifier.width(WorkBadgeSlot)) {
            if (app.isWork) WorkBadge()
        }

        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )

        // A marca de favorito é um ponto, não um ícone: é o único jeito de
        // responder "esse já está na home?" sem trair a decisão de não ter
        // ícones. Fica depois do rótulo pra não desalinhar a coluna.
        if (isFavorite) {
            Text(
                text = "  ·",
                style = MaterialTheme.typography.titleMedium,
                color = NimForegroundMuted,
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
