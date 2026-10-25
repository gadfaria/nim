package com.gadfaria.nim.ui

import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gadfaria.nim.NimViewModel
import com.gadfaria.nim.Screen
import com.gadfaria.nim.data.AppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Espera o usuário parar de digitar antes de abrir sozinho. Curto de propósito:
 * só precisa cobrir o intervalo entre duas teclas de quem digita rápido, não
 * dar tempo de pensar. Zerar dispara no meio de uma palavra — "gmail" abriria
 * já no "gm".
 */
private const val AUTO_LAUNCH_DELAY_MS = 150L

/**
 * Host da home e da gaveta. A troca é por gesto vertical: sobe abre, desce fecha.
 *
 * O gesto aqui é o mínimo que torna a gaveta usável — a transição animada de
 * verdade é assunto do M5.
 */
@Composable
fun NimApp(
    viewModel: NimViewModel,
    homePresses: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    val apps by viewModel.visibleApps.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val unambiguous by viewModel.unambiguousMatch.collectAsStateWithLifecycle()
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteKeys by viewModel.favoriteKeys.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocus = remember { FocusRequester() }

    // App do menu de toque longo, se algum estiver aberto. Guarda os bounds
    // junto porque as ações do menu também animam a partir da linha tocada.
    var menuApp by remember { mutableStateOf<Pair<AppInfo, Rect?>?>(null) }

    fun goHome() {
        keyboard?.hide()
        menuApp = null
        viewModel.goHome()
    }

    // Lançar SEMPRE fecha a gaveta e limpa a busca. Se a busca sobrevivesse ao
    // lançamento, voltar do app reabriria a gaveta com um resultado só e o
    // auto-abrir dispararia de novo: o usuário ficaria preso num laço.
    fun launchAndClose(app: AppInfo, bounds: Rect?) {
        viewModel.launch(app, bounds)
        goHome()
    }

    // Abriu a gaveta: foco no campo e teclado na tela. A gaveta é para digitar,
    // não para rolar 300 nomes.
    LaunchedEffect(screen) {
        if (screen == Screen.DRAWER) {
            searchFocus.requestFocus()
            keyboard?.show()
        }
    }

    // A lista sempre começa do topo: ao abrir e a cada mudança da busca.
    //
    // Isto NÃO pode ser feito no fechamento. A LazyColumn com `key` reancora a
    // rolagem no item que estava no topo quando o conteúdo muda: filtrando até
    // sobrar um app, ele vira a âncora, e ao limpar a busca a lista tenta
    // mantê-lo no topo, batendo no fim da rolagem. Um scrollToItem(0) disparado
    // antes dessa reancoragem é simplesmente desfeito por ela.
    LaunchedEffect(screen, query) {
        if (screen == Screen.DRAWER) listState.scrollToItem(0)
    }

    // Um resultado inequívoco abre sozinho. As chaves do efeito são a busca e o
    // próprio resultado: mudou qualquer um, o timer reinicia — é isso que faz o
    // debounce, sem precisar de operador de flow.
    LaunchedEffect(screen, query, unambiguous) {
        if (screen != Screen.DRAWER || query.isBlank()) return@LaunchedEffect
        val target = unambiguous ?: return@LaunchedEffect
        delay(AUTO_LAUNCH_DELAY_MS)
        launchAndClose(target, null)
    }

    // Apertar HOME já estando no Nim volta pra home limpa. Sem isto o usuário
    // aperta HOME e continua vendo a gaveta onde parou.
    LaunchedEffect(homePresses) {
        homePresses.collect { goHome() }
    }

    // Voltar devolve pra home, venha da gaveta ou das configurações. Já na
    // home ele não faz nada — quem garante isso é o callback no-op da
    // MainActivity, que fica embaixo deste na pilha.
    BackHandler(enabled = screen != Screen.HOME) { goHome() }

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
                    goHome()
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                overscroll = 0f
                return Velocity.Zero
            }
        }
    }

    AnimatedContent(
        targetState = screen,
        // Crossfade dissolvia as duas telas ao mesmo tempo e dava pra ler a
        // lista de apps por trás do relógio. Aqui a que sai termina antes de a
        // que entra aparecer, e a gaveta desliza na direção do próprio gesto.
        transitionSpec = {
            when {
                targetState == Screen.DRAWER ->
                    (slideInVertically(tween(240)) { it / 8 } +
                        fadeIn(tween(180, delayMillis = 60))) togetherWith
                        fadeOut(tween(140))

                initialState == Screen.DRAWER ->
                    fadeIn(tween(200, delayMillis = 100)) togetherWith
                        (slideOutVertically(tween(220)) { it / 8 } + fadeOut(tween(180)))

                else ->
                    fadeIn(tween(200, delayMillis = 100)) togetherWith fadeOut(tween(140))
            }
        },
        label = "telas",
    ) { current ->
        when (current) {
            Screen.DRAWER -> Box(
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(pullDownToClose)
            ) {
                DrawerScreen(
                    apps = apps,
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onLaunch = ::launchAndClose,
                    favoriteKeys = favoriteKeys,
                    onOpenMenu = { app, bounds ->
                        keyboard?.hide()
                        menuApp = app to bounds
                    },
                    searchFocusRequester = searchFocus,
                    listState = listState,
                )
            }

            Screen.SETTINGS -> SettingsScreen(
                favorites = favorites,
                onReorder = viewModel::reorderFavorites,
                onApplyWallpaper = viewModel::applyNimWallpaper,
                modifier = modifier.fillMaxSize(),
            )

            // Segurar no vazio da home abre as configurações — a convenção de
            // launcher. Sobre um favorito não dispara: a linha consome o toque
            // longo antes, pro menu.
            Screen.HOME -> Box(
                modifier = modifier
                    .fillMaxSize()
                    .homeGestures(
                        swipeUpThresholdPx = threshold,
                        onSwipeUp = viewModel::openDrawer,
                        onLongPress = viewModel::openSettings,
                    )
            ) {
                HomeScreen(
                    favorites = favorites,
                    onLaunch = viewModel::launch,
                    onOpenMenu = { app, bounds ->
                        keyboard?.hide()
                        menuApp = app to bounds
                    },
                )
            }
        }
    }

    menuApp?.let { (app, bounds) ->
        // Voltar fecha o menu antes de mexer na tela de baixo.
        BackHandler { menuApp = null }

        AppMenu(
            app = app,
            isFavorite = app.key in favoriteKeys,
            onToggleFavorite = {
                viewModel.toggleFavorite(app)
                menuApp = null
            },
            onAppInfo = {
                viewModel.openAppInfo(app, bounds)
                menuApp = null
            },
            onUninstall = {
                viewModel.requestUninstall(app)
                menuApp = null
            },
            onDismiss = { menuApp = null },
        )
    }
}
