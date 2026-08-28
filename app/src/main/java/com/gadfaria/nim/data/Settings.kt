package com.gadfaria.nim.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nim")

/**
 * Preferências do launcher. Hoje só os favoritos; DataStore porque são meia
 * dúzia de chaves e Room seria exagero.
 */
class Settings(private val context: Context) {

    /**
     * Guardado como lista, não como Set: a ordem é a de favoritar, e é ela que
     * a home mostra. Um Set de preferências perderia essa ordem em cada
     * leitura.
     *
     * `\n` como separador é seguro — a chave é um ComponentName achatado mais
     * o serial do perfil, e nenhum dos dois contém quebra de linha.
     */
    private val favoritesKey = stringPreferencesKey("favorites")

    val favoriteKeys: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[favoritesKey]?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
    }

    suspend fun toggleFavorite(key: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[favoritesKey]
                ?.split(SEPARATOR)
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            val updated = if (key in current) current - key else current + key
            prefs[favoritesKey] = updated.joinToString(SEPARATOR)
        }
    }

    private companion object {
        const val SEPARATOR = "\n"
    }
}
