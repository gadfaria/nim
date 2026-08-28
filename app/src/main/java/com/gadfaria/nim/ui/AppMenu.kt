package com.gadfaria.nim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.ui.theme.NimBackground
import com.gadfaria.nim.ui.theme.NimForegroundMuted

/**
 * Menu do toque longo. Sem Material dropdown de propósito: aquilo traz caixa,
 * sombra e cantos que não existem em nenhum outro lugar do Nim.
 *
 * As opções ficam embaixo, ao alcance do polegar — o dedo já está lá quando o
 * menu abre.
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
            // Opaco. Véu translúcido deixava a gaveta legível por baixo e as
            // linhas se intercalavam com as opções — dois textos disputando a
            // mesma altura da tela. O nome do app no topo já dá o contexto que
            // a transparência tentava dar.
            .background(NimBackground)
            // Tocar fora fecha. Sem ripple: o Nim não tem esse vocabulário.
            .clickable(interactionSource = noRipple, indication = null, onClick = onDismiss)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = NimForegroundMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        MenuItem(
            text = if (isFavorite) "Remover dos favoritos" else "Favoritar",
            onClick = onToggleFavorite,
        )
        MenuItem(text = "Informações do app", onClick = onAppInfo)
        MenuItem(text = "Desinstalar", onClick = onUninstall)

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick)
            .padding(vertical = 16.dp),
    )
}
