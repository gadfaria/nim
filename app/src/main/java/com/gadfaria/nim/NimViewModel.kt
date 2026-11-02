package com.gadfaria.nim

import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.data.AppRepository
import com.gadfaria.nim.data.HomeWallpaper
import com.gadfaria.nim.data.RankResult
import com.gadfaria.nim.data.Settings
import com.gadfaria.nim.data.normalizeForSearch
import com.gadfaria.nim.data.rankBySearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The launcher's three screens. No nested navigation: it's always one of them. */
enum class Screen { HOME, DRAWER, SETTINGS }

class NimViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val settings = Settings(application)
    private val homeWallpaper = HomeWallpaper(application)

    private val allApps: StateFlow<List<AppInfo>> = repository.apps.stateIn(
        scope = viewModelScope,
        // The launcher comes back to the foreground all the time. Keeping the
        // subscription alive for a few seconds avoids rereading the whole list
        // on every return.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val ranked: StateFlow<RankResult<AppInfo>> =
        combine(allApps, _query) { apps, query ->
            rankBySearch(apps, normalizeForSearch(query.trim())) { it.searchable }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RankResult(emptyList(), null),
        )

    val visibleApps: StateFlow<List<AppInfo>> = ranked
        .map { it.items }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The app to auto-launch: the only one in the best ranking tier.
     *
     * It isn't "the whole list has one item". With subsequence in the search,
     * "gm" brings Gmail (prefix) and Gemini (g...m) — the list has two, but only
     * one is the obvious result, and that's the one that should open.
     */
    val unambiguousMatch: StateFlow<AppInfo?> = ranked
        .map { it.unambiguous }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Favorites resolved to apps, in the order they were favorited.
     *
     * A key with no matching app is ignored, not deleted: an app can be
     * temporarily unavailable (work profile in quiet mode, update in progress)
     * and deleting the favorite then would lose the user's choice over a passing
     * disappearance.
     */
    val favorites: StateFlow<List<AppInfo>> =
        combine(allApps, settings.favoriteKeys) { apps, keys ->
            val byKey = apps.associateBy { it.key }
            keys.mapNotNull { byKey[it] }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteKeys: StateFlow<Set<String>> = settings.favoriteKeys
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(app: AppInfo) {
        viewModelScope.launch { settings.toggleFavorite(app.key) }
    }

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    fun openDrawer() { _screen.value = Screen.DRAWER }

    fun openSettings() { _screen.value = Screen.SETTINGS }

    /** Going home always clears the search: the drawer reopens blank. */
    fun goHome() {
        _screen.value = Screen.HOME
        _query.value = ""
    }

    fun reorderFavorites(apps: List<AppInfo>) {
        viewModelScope.launch { settings.setFavorites(apps.map { it.key }) }
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun launch(app: AppInfo, sourceBounds: Rect?) = repository.launch(app, sourceBounds)

    fun openAppInfo(app: AppInfo, sourceBounds: Rect?) = repository.openAppInfo(app, sourceBounds)

    fun requestUninstall(app: AppInfo) = repository.requestUninstall(app)

    /** Returns whether it worked, so the settings screen can say what happened. */
    suspend fun applyNimWallpaper(): Boolean =
        withContext(Dispatchers.IO) { homeWallpaper.applyNimBackground() }
}
