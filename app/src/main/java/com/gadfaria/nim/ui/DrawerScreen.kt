package com.gadfaria.nim.ui

import android.graphics.Rect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.data.AppInfo

@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    onLaunch: (AppInfo, Rect?) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 24.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            AppRow(app = app, onLaunch = onLaunch)
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    onLaunch: (AppInfo, Rect?) -> Unit,
) {
    // Guardado numa var em vez de estado: só é lido no clique, e virar estado
    // recomporia a linha a cada scroll.
    var coordinates: LayoutCoordinates? = remember { null }

    Text(
        text = app.label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates = it }
            // A animação de abertura sai de onde o dedo tocou, não do centro
            // da tela — é o que faz o toque parecer que abriu aquele item.
            .clickable { onLaunch(app, coordinates?.launchBounds()) }
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
