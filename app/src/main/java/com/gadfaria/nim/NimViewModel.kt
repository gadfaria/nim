package com.gadfaria.nim

import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class NimViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    val apps: StateFlow<List<AppInfo>> = repository.apps.stateIn(
        scope = viewModelScope,
        // O launcher volta ao primeiro plano o tempo todo. Manter a assinatura
        // viva por alguns segundos evita reler a lista inteira a cada volta.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _drawerOpen = MutableStateFlow(false)
    val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()

    fun openDrawer() { _drawerOpen.value = true }

    fun closeDrawer() { _drawerOpen.value = false }

    fun launch(app: AppInfo, sourceBounds: Rect?) = repository.launch(app, sourceBounds)
}
