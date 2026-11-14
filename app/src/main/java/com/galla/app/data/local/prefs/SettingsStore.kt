package com.galla.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.galla.app.core.model.Settings
import com.galla.app.core.model.ThemeChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "galla_settings")

/**
 * Settings saved with Jetpack DataStore: a small key/value file on the phone.
 * Read it as a Flow (it re-emits whenever something changes), change it with [update].
 */
class SettingsStore(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val shopName = stringPreferencesKey("shop_name")
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
    }

    val settings: Flow<Settings> = store.data.map { read(it) }.distinctUntilChanged()

    private fun read(p: Preferences): Settings {
        val d = Settings()
        return Settings(
            onboarded = p[Keys.onboarded] ?: d.onboarded,
            shopName = p[Keys.shopName] ?: d.shopName,
            theme = p[Keys.theme]?.let { runCatching { ThemeChoice.valueOf(it) }.getOrNull() } ?: d.theme,
            dynamicColor = p[Keys.dynamicColor] ?: d.dynamicColor,
        )
    }

    suspend fun current(): Settings = settings.first()

    /** Changes settings: `update { it.copy(shopName = "Sharma Hardware") }`. */
    suspend fun update(change: (Settings) -> Settings) {
        // Read and write inside one DataStore transaction, so two changes made at the same
        // moment cannot undo each other.
        store.edit { p ->
            val next = change(read(p))
            p[Keys.onboarded] = next.onboarded
            p[Keys.shopName] = next.shopName
            p[Keys.theme] = next.theme.name
            p[Keys.dynamicColor] = next.dynamicColor
        }
    }
}
