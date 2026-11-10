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
 * Waits for the user to stop typing before auto-launching. Short on purpose: it
 * only needs to cover the gap between two keystrokes of a fast typist, not give
 * time to think. Zero fires mid-word — "gmail" would already open at "gm".
 */
private const val AUTO_LAUNCH_DELAY_MS = 150L

/**
 * Host for the home and the drawer. Switching is by vertical gesture: up opens,
 * down closes.
 *
 * The gesture here is the minimum that makes the drawer usable — the real
 * animated transition belongs to M5.
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

    // App for the long-press menu, if one is open. Keeps the bounds alongside
    // because the menu actions also animate from the tapped row.
    var menuApp by remember { mutableStateOf<Pair<AppInfo, Rect?>?>(null) }

    fun goHome() {
        keyboard?.hide()
        menuApp = null
        viewModel.goHome()
    }

    // Launching ALWAYS closes the drawer and clears the search. If the search
    // survived the launch, coming back from the app would reopen the drawer with
    // a single result and auto-launch would fire again: the user would be stuck
    // in a loop.
    fun launchAndClose(app: AppInfo, bounds: Rect?) {
        viewModel.launch(app, bounds)
        goHome()
    }

    // Drawer opened: focus the field and show the keyboard. The drawer is for
    // typing, not for scrolling through 300 names.
    LaunchedEffect(screen) {
        if (screen == Screen.DRAWER) {
            searchFocus.requestFocus()
            keyboard?.show()
        }
    }

    // The list always starts at the top: on open and on every search change.
    //
    // This CANNOT be done on close. A LazyColumn with `key` re-anchors the
    // scroll on the item that was at the top when the content changes: filtering
    // down to one app makes it the anchor, and when the search is cleared the
    // list tries to keep it at the top, hitting the end of the scroll. A
    // scrollToItem(0) fired before that re-anchoring is simply undone by it.
    LaunchedEffect(screen, query) {
        if (screen == Screen.DRAWER) listState.scrollToItem(0)
    }

    // An unambiguous result opens on its own. The effect's keys are the search
    // and the result itself: if either changes, the timer restarts — that's what
    // does the debounce, with no flow operator needed.
    LaunchedEffect(screen, query, unambiguous) {
        if (screen != Screen.DRAWER || query.isBlank()) return@LaunchedEffect
        val target = unambiguous ?: return@LaunchedEffect
        delay(AUTO_LAUNCH_DELAY_MS)
        launchAndClose(target, null)
    }

    // Pressing HOME while already in Nim returns to a clean home. Without this
    // the user presses HOME and still sees the drawer where they left it.
    LaunchedEffect(homePresses) {
        homePresses.collect { goHome() }
    }

    // Back returns to the home, from the drawer or from settings. On the home
    // itself it does nothing — that's ensured by MainActivity's no-op callback,
    // which sits below this one in the stack.
    BackHandler(enabled = screen != Screen.HOME) { goHome() }

    val threshold = with(LocalDensity.current) { 56.dp.toPx() }

    // Pulling the list down while already at the top closes the drawer. Via
    // nested scroll because the LazyColumn gets the drag first, not this Box.
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
        // Crossfade dissolved both screens at the same time and the app list
        // was readable behind the clock. Here the outgoing one finishes before
        // the incoming one appears, and the drawer slides in its gesture's
        // direction.
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
        label = "screens",
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

            // Holding on empty home space opens settings — the launcher
            // convention. It doesn't fire over a favorite: the row consumes the
            // down, and homeGestures leaves the long press to it (the menu).
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
        // Back closes the menu before touching the screen below.
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
