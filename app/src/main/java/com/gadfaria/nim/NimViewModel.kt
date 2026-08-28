package com.gadfaria.nim

import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.data.AppRepository
import com.gadfaria.nim.data.normalizeForSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    /**
     * Filtro por "contém", ainda sem ranqueamento — prefixo > iniciais > contém
     * é o M3. Aqui só interessa reduzir a lista.
     */
    val visibleApps: StateFlow<List<AppInfo>> =
        combine(allApps, _query) { apps, query ->
            val term = normalizeForSearch(query.trim())
            if (term.isEmpty()) apps else apps.filter { term in it.normalizedLabel }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

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
