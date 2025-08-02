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
//    suspend fun saveConfigForApp(config: OverlayConfig) {
//        context.dataStore.edit { prefs ->
//            prefs[floatPreferencesKey("${config.packageName}_horizontal_offset")] = config.hOffset
//            prefs[floatPreferencesKey("${config.packageName}_vertical_offset")] = config.vOffset
//            prefs[intPreferencesKey("${config.packageName}_dot_size")] = config.dotSize
//            prefs[intPreferencesKey("${config.packageName}_line_width")] = config.lineWidth
//        }
//    }
//
    fun loadConfigFlowForApp(pkg: String): Flow<OverlayConfig> {
        Log.d("SettingsRepository", "Loading config flow for package: $pkg")

        return context.dataStore.data.map { prefs ->
            fun float(key: String) =
                prefs[floatPreferencesKey(key)]

            fun int(key: String) =
                prefs[intPreferencesKey(key)]

            OverlayConfig(
//                packageName = pkg,
                hOffset = float("${pkg}_horizontal_offset"),
                vOffset = float("${pkg}_vertical_offset"),
                dotSize = int("${pkg}_dot_size"),
                lineWidth = int("${pkg}_line_width")
            )
        }
    }

    fun <T> loadSingleConfigFlow(key: Key<T>, fallback: T): Flow<T> {
        return context.dataStore.data.map {
            it[key] ?: fallback
        }
    }

    suspend fun <T> saveSingleConfig(key: Key<T>, value: T) {
        Log.d("SettingsRepository", "Saving $value to ${key.name}")
        context.dataStore.edit {
            it[key] = value
        }
    }

    suspend fun <T> removeSingleConfig(key: Key<T>) {
        Log.d("SettingsRepository", "Removing ${key.name}")
        context.dataStore.edit {
            it.remove(key)
        }
    }

    fun hasConfigOfPackage(packageName: String): Boolean {
        return runBlocking { context.dataStore.data.map {
            it.contains(floatPreferencesKey("${packageName}_horizontal_offset")) ||
            it.contains(floatPreferencesKey("${packageName}_vertical_offset")) ||
            it.contains(intPreferencesKey("${packageName}_dot_size")) ||
            it.contains(intPreferencesKey("${packageName}_line_width"))
        }.first() }
    }
}
