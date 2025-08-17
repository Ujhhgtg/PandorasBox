package dev.ujhhgtg.pandorasbox.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences.Key
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.ujhhgtg.pandorasbox.models.OverlayConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    fun loadConfigFlowForApp(pkg: String): Flow<OverlayConfig> {
        Log.d("PB.SettingsRepository", "Loading config flow for package: $pkg")

        return context.dataStore.data.map { prefs ->
            fun float(key: String) =
                prefs[floatPreferencesKey(key)]

            fun int(key: String) =
                prefs[intPreferencesKey(key)]

            OverlayConfig(
//                packageName = pkg,
                hOffset = float("o_${pkg}_h"),
                vOffset = float("o_${pkg}_v"),
                dotSize = int("o_${pkg}_s"),
                lineWidth = int("o_${pkg}_w")
            )
        }
    }

    fun <T> loadSingleConfigFlow(key: Key<T>, fallback: T): Flow<T> {
        return context.dataStore.data.map {
            it[key] ?: fallback
        }
    }

    suspend fun <T> saveSingleConfig(key: Key<T>, value: T) {
        Log.d("PB.SettingsRepository", "Saving $value to ${key.name}")
        context.dataStore.edit {
            it[key] = value
        }
    }

    suspend fun <T> removeSingleConfig(key: Key<T>) {
        Log.d("PB.SettingsRepository", "Removing ${key.name}")
        context.dataStore.edit {
            it.remove(key)
        }
    }

    fun hasOverlayConfigOfPackage(packageName: String): Boolean {
        return runBlocking { context.dataStore.data.map {
            it.contains(floatPreferencesKey("o_${packageName}_h")) ||
            it.contains(floatPreferencesKey("o_${packageName}_v")) ||
            it.contains(intPreferencesKey("o_${packageName}_s")) ||
            it.contains(intPreferencesKey("o_${packageName}_w"))
        }.first() }
    }
}
