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
 * Launcher preferences. Only favorites for now; DataStore because it's half a
 * dozen keys and Room would be overkill.
 */
class Settings(private val context: Context) {

    /**
     * Stored as a list, not a Set: the order is the favoriting order, and it's
     * what the home shows. A preferences Set would lose that order on every
     * read.
     */
    private val favoritesKey = stringPreferencesKey("favorites")

    val favoriteKeys: Flow<List<String>> = context.dataStore.data.map { prefs ->
        decodeFavorites(prefs[favoritesKey])
    }

    /** Writes the whole order at once — that's what drag-to-reorder produces. */
    suspend fun setFavorites(keys: List<String>) {
        context.dataStore.edit { prefs -> prefs[favoritesKey] = encodeFavorites(keys) }
    }

    suspend fun toggleFavorite(key: String) {
        context.dataStore.edit { prefs ->
            prefs[favoritesKey] =
                encodeFavorites(toggleFavorite(decodeFavorites(prefs[favoritesKey]), key))
        }
    }
}
