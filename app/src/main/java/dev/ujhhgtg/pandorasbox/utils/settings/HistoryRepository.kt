package dev.ujhhgtg.pandorasbox.utils.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import dev.ujhhgtg.pandorasbox.BrowserHistory
import dev.ujhhgtg.pandorasbox.HistoryEntry
import dev.ujhhgtg.pandorasbox.utils.BrowserHistorySerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.historyDataStore by dataStore("browser_history", BrowserHistorySerializer)

class HistoryRepository(private val context: Context) {
    val history: Flow<List<HistoryEntry>> =
        dataStore.data.map { it.entriesList.sortedByDescending { e -> e.timestamp } }

    suspend fun addEntry(url: String, title: String) {
        dataStore.updateData { current ->
            val newEntry = HistoryEntry.newBuilder()
                .setUrl(url)
                .setTitle(title)
                .setTimestamp(System.currentTimeMillis())
                .build()

            // avoid duplicates by url
            val filtered = current.entriesList.filterNot { it.url == url }

            current.toBuilder()
                .clearEntries()
                .addAllEntries(listOf(newEntry) + filtered)
                .build()
        }
    }

    suspend fun clear() {
        dataStore.updateData { it.toBuilder().clearEntries().build() }
    }

    val dataStore: DataStore<BrowserHistory>
        get() = context.historyDataStore
}
