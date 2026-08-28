package com.gadfaria.nim

import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.data.AppRepository
import com.gadfaria.nim.data.RankResult
import com.gadfaria.nim.data.normalizeForSearch
import com.gadfaria.nim.data.rankBySearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NimViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

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

    private val _drawerOpen = MutableStateFlow(false)
    val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()

    fun openDrawer() { _drawerOpen.value = true }

    /** Fechar sempre limpa a busca: a gaveta reabre em branco, nunca no meio. */
    fun closeDrawer() {
        _drawerOpen.value = false
        _query.value = ""
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun launch(app: AppInfo, sourceBounds: Rect?) = repository.launch(app, sourceBounds)
}
