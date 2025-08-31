package dev.ujhhgtg.pandorasbox.utils.settings

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.Preferences.Key
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.ujhhgtg.pandorasbox.models.OverlayConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.prefsDataStore by preferencesDataStore(name = "settings")

class PrefsRepository(private val context: Context) {
    companion object {
        fun bKey(name: String) = booleanPreferencesKey(name)
        fun iKey(name: String) = intPreferencesKey(name)
        fun fKey(name: String) = floatPreferencesKey(name)
    }

    fun loadConfigFlowForApp(pkg: String): Flow<OverlayConfig> {
        Log.d("PB.PrefsRepository", "Loading config flow for package: $pkg")

        return dataStore.data.map { prefs ->
            OverlayConfig(
//                packageName = pkg,
                hOffset = prefs[fKey("o_${pkg}_h")],
                vOffset = prefs[fKey("o_${pkg}_v")],
                dotSize = prefs[iKey("o_${pkg}_s")],
                lineWidth = prefs[iKey("o_${pkg}_w")]
            )
        }
    }

    fun <T> loadSingleConfigFlow(key: Key<T>, fallback: T): Flow<T> {
        return dataStore.data.map {
            it[key] ?: fallback
        }
    }

    suspend fun <T> saveSingleConfig(key: Key<T>, value: T) {
        Log.d("PB.SettingsRepository", "Saving $value to ${key.name}")
        dataStore.edit {
            it[key] = value
        }
    }

    suspend fun <T> removeSingleConfig(key: Key<T>) {
        Log.d("PB.SettingsRepository", "Removing ${key.name}")
        dataStore.edit {
            it.remove(key)
        }
    }

    fun hasOverlayConfigOfPackage(packageName: String): Boolean {
        return runBlocking { dataStore.data.map {
            it.contains(floatPreferencesKey("o_${packageName}_h")) ||
            it.contains(floatPreferencesKey("o_${packageName}_v")) ||
            it.contains(intPreferencesKey("o_${packageName}_s")) ||
            it.contains(intPreferencesKey("o_${packageName}_w"))
        }.first() }
    }

    @Composable
    fun <T> rememberPreference(
        prefKey: Key<T>,
        default: T
    ): State<T> {
        val flow = dataStore.data.map { prefs ->
            prefs[prefKey] ?: default
        }
        return flow.collectAsState(initial = default)
    }


    val dataStore: DataStore<Preferences>
        get() = context.prefsDataStore
}
