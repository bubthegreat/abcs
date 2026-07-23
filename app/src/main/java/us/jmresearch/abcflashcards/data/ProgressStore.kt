package us.jmresearch.abcflashcards.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "progress")

class ProgressStore(private val context: Context) {

    private val progressKey = stringPreferencesKey("progress_v1")
    private val thresholdKey = intPreferencesKey("threshold_v1")
    private val forceUnlockedKey = stringSetPreferencesKey("force_unlocked_v1")

    val progress: Flow<Map<String, ItemProgress>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> decodeProgress(prefs[progressKey] ?: "") }

    val threshold: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[thresholdKey] ?: 3 }

    val forceUnlocked: Flow<Set<String>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[forceUnlockedKey] ?: emptySet() }

    suspend fun updateItem(itemId: String, transform: (ItemProgress) -> ItemProgress) {
        context.dataStore.edit { prefs ->
            val map = decodeProgress(prefs[progressKey] ?: "").toMutableMap()
            map[itemId] = transform(map[itemId] ?: ItemProgress())
            prefs[progressKey] = encodeProgress(map)
        }
    }

    suspend fun resetDeck(itemIds: List<String>) {
        context.dataStore.edit { prefs ->
            val map = decodeProgress(prefs[progressKey] ?: "").toMutableMap()
            itemIds.forEach { map.remove(it) }
            prefs[progressKey] = encodeProgress(map)
        }
    }

    suspend fun setThreshold(value: Int) {
        context.dataStore.edit { it[thresholdKey] = value.coerceIn(1, 10) }
    }

    suspend fun setForceUnlocked(deckId: String, unlocked: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[forceUnlockedKey] ?: emptySet()
            prefs[forceUnlockedKey] = if (unlocked) current + deckId else current - deckId
        }
    }
}
