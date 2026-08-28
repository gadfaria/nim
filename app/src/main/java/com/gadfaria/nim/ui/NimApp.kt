package com.gadfaria.nim.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gadfaria.nim.NimViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Host da home e da gaveta. A troca é por gesto vertical: sobe abre, desce fecha.
 *
 * O gesto aqui é o mínimo que torna o M2 usável — a transição animada de
 * verdade é assunto do M5.
 */
@Composable
fun NimApp(
    viewModel: NimViewModel,
    homePresses: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val drawerOpen by viewModel.drawerOpen.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun closeDrawer() {
        viewModel.closeDrawer()
        scope.launch { listState.scrollToItem(0) }
    }

    // Apertar HOME já estando no Nim volta pra home limpa. Sem isto o usuário
    // aperta HOME e continua vendo a gaveta onde parou.
    LaunchedEffect(homePresses) {
        homePresses.collect { closeDrawer() }
    }

    // Voltar fecha a gaveta. Na home ele não faz nada — quem garante isso é o
    // callback no-op da MainActivity, que fica embaixo deste na pilha.
    BackHandler(enabled = drawerOpen) { closeDrawer() }

    val threshold = with(LocalDensity.current) { 56.dp.toPx() }

    // Puxar a lista pra baixo já no topo fecha a gaveta. Via nested scroll
    // porque quem recebe o arrasto primeiro é a LazyColumn, não este Box.
    val pullDownToClose = remember(threshold) {
        object : NestedScrollConnection {
            private var overscroll = 0f

            override fun onPostScroll(
                consumed: Offset, available: Offset, source: NestedScrollSource,
            ): Offset {
                if (available.y > 0f) overscroll += available.y else overscroll = 0f
                if (overscroll > threshold) {
                    overscroll = 0f
                    closeDrawer()
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                overscroll = 0f
                return Velocity.Zero
            }
        }
    }

    Crossfade(targetState = drawerOpen, label = "home-gaveta") { showDrawer ->
        if (showDrawer) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(pullDownToClose)
            ) {
                DrawerScreen(
                    apps = apps,
                    onLaunch = viewModel::launch,
                    listState = listState,
                )
            }
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .pointerInput(threshold) {
                        var dragged = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragged = 0f },
                            onDragEnd = {
                                if (dragged < -threshold) viewModel.openDrawer()
                            },
                        ) { _, delta -> dragged += delta }
                    }
            ) {
                HomeScreen()
            }
        }
    }
}
