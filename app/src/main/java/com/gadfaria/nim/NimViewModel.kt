package com.gadfaria.nim

import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.data.AppRepository
import com.gadfaria.nim.data.RankResult
import com.gadfaria.nim.data.Settings
import com.gadfaria.nim.data.normalizeForSearch
import com.gadfaria.nim.data.rankBySearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** As três telas do launcher. Não há navegação aninhada: é sempre uma delas. */
enum class Screen { HOME, DRAWER, SETTINGS }

class NimViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val settings = Settings(application)

    private val allApps: StateFlow<List<AppInfo>> = repository.apps.stateIn(
        scope = viewModelScope,
        // O launcher volta ao primeiro plano o tempo todo. Manter a assinatura
        // viva por alguns segundos evita reler a lista inteira a cada volta.
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
     * O app a abrir sozinho: único na melhor camada do ranqueamento.
     *
     * Não é "a lista inteira tem um item". Com subsequência na busca, "gm" traz
     * Gmail (prefixo) e Gemini (g...m) — a lista tem dois, mas só um é o
     * resultado óbvio, e é ele que deve abrir.
     */
    val unambiguousMatch: StateFlow<AppInfo?> = ranked
        .map { it.unambiguous }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Favoritos resolvidos para apps, na ordem em que foram favoritados.
     *
     * Chave sem app correspondente é ignorada, não apagada: um app pode estar
     * temporariamente indisponível (perfil de trabalho em modo silencioso,
     * atualização em curso) e apagar o favorito aí perderia a escolha do
     * usuário por um sumiço passageiro.
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

    /** Voltar pra home sempre limpa a busca: a gaveta reabre em branco. */
    fun goHome() {
        _screen.value = Screen.HOME
        _query.value = ""
    }

    fun reorderFavorites(apps: List<AppInfo>) {
        viewModelScope.launch { settings.setFavorites(apps.map { it.key }) }
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun launch(app: AppInfo, sourceBounds: Rect?) = repository.launch(app, sourceBounds)
}
