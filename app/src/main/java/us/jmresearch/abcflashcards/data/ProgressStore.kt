package us.jmresearch.abcflashcards.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "progress")

private const val DEFAULT_PROFILE_ID = "p1"
private const val DEFAULT_PROFILE_NAME = "Kid 1"

class ProgressStore(private val context: Context) {

    private val profilesKey = stringPreferencesKey("profiles_v1")
    private val activeProfileKey = stringPreferencesKey("active_profile_v1")

    // Legacy single-profile keys (pre-profiles installs); read as fallback for p1.
    private val legacyProgressKey = stringPreferencesKey("progress_v1")
    private val legacyThresholdKey = androidx.datastore.preferences.core.intPreferencesKey("threshold_v1")

    private fun progressKey(pid: String) = stringPreferencesKey("progress_v1_$pid")
    private fun thresholdKey(pid: String) = stringPreferencesKey("threshold_v1_$pid")
    private fun forceUnlockedKey(pid: String) = stringPreferencesKey("force_unlocked_v1_$pid")
    private fun starBankKey(pid: String) = stringPreferencesKey("star_bank_v1_$pid")
    private fun starProgressKey(pid: String) = stringPreferencesKey("star_progress_v1_$pid")
    private val pinKey = stringPreferencesKey("parent_pin_v1")
    private val kidModeKey = stringPreferencesKey("kid_mode_v1")

    private val safeData: Flow<Preferences> = context.dataStore.data
        .catch { emit(emptyPreferences()) }

    private fun activePid(prefs: Preferences): String =
        prefs[activeProfileKey] ?: DEFAULT_PROFILE_ID

    private fun profilesOf(prefs: Preferences): List<Profile> {
        val decoded = decodeProfiles(prefs[profilesKey] ?: "")
        return decoded.ifEmpty { listOf(Profile(DEFAULT_PROFILE_ID, DEFAULT_PROFILE_NAME)) }
    }

    private fun rawProgress(prefs: Preferences, pid: String): String =
        prefs[progressKey(pid)]
            ?: (if (pid == DEFAULT_PROFILE_ID) prefs[legacyProgressKey] else null)
            ?: ""

    private fun rawThreshold(prefs: Preferences, pid: String): Int =
        prefs[thresholdKey(pid)]?.toIntOrNull()
            ?: (if (pid == DEFAULT_PROFILE_ID) prefs[legacyThresholdKey] else null)
            ?: 3

    val profiles: Flow<List<Profile>> = safeData.map { profilesOf(it) }

    val activeProfileId: Flow<String> = safeData.map { activePid(it) }

    val progress: Flow<Map<String, ItemProgress>> = safeData.map { prefs ->
        decodeProgress(rawProgress(prefs, activePid(prefs)))
    }

    val threshold: Flow<Int> = safeData.map { prefs ->
        rawThreshold(prefs, activePid(prefs))
    }

    val forceUnlocked: Flow<Set<String>> = safeData.map { prefs ->
        (prefs[forceUnlockedKey(activePid(prefs))] ?: "")
            .split(";").filter { it.isNotBlank() }.toSet()
    }

    suspend fun updateItem(itemId: String, transform: (ItemProgress) -> ItemProgress) {
        context.dataStore.edit { prefs ->
            val pid = activePid(prefs)
            val map = decodeProgress(rawProgress(prefs, pid)).toMutableMap()
            map[itemId] = transform(map[itemId] ?: ItemProgress())
            prefs[progressKey(pid)] = encodeProgress(map)
        }
    }

    suspend fun resetDeck(itemIds: List<String>) {
        context.dataStore.edit { prefs ->
            val pid = activePid(prefs)
            val map = decodeProgress(rawProgress(prefs, pid)).toMutableMap()
            itemIds.forEach { map.remove(it) }
            prefs[progressKey(pid)] = encodeProgress(map)
        }
    }

    suspend fun setThreshold(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[thresholdKey(activePid(prefs))] = value.coerceIn(1, 10).toString()
        }
    }

    suspend fun setForceUnlocked(deckId: String, unlocked: Boolean) {
        context.dataStore.edit { prefs ->
            val pid = activePid(prefs)
            val key = forceUnlockedKey(pid)
            val current = (prefs[key] ?: "").split(";").filter { it.isNotBlank() }.toMutableSet()
            if (unlocked) current.add(deckId) else current.remove(deckId)
            prefs[key] = current.joinToString(";")
        }
    }

    val starBank: Flow<Int> = safeData.map { prefs ->
        prefs[starBankKey(activePid(prefs))]?.toIntOrNull() ?: 0
    }

    val starProgress: Flow<Int> = safeData.map { prefs ->
        prefs[starProgressKey(activePid(prefs))]?.toIntOrNull() ?: 0
    }

    val parentPin: Flow<String?> = safeData.map { it[pinKey] }

    val kidMode: Flow<Boolean> = safeData.map { it[kidModeKey] == "on" }

    /** One correct answer in kid mode. Every [correctsPerStar] corrects banks a star. */
    suspend fun recordKidCorrect(correctsPerStar: Int = 10) {
        context.dataStore.edit { prefs ->
            val pid = activePid(prefs)
            val progress = (prefs[starProgressKey(pid)]?.toIntOrNull() ?: 0) + 1
            if (progress >= correctsPerStar) {
                val bank = prefs[starBankKey(pid)]?.toIntOrNull() ?: 0
                prefs[starBankKey(pid)] = (bank + 1).toString()
                prefs[starProgressKey(pid)] = "0"
            } else {
                prefs[starProgressKey(pid)] = progress.toString()
            }
        }
    }

    suspend fun addStars(count: Int) {
        context.dataStore.edit { prefs ->
            val pid = activePid(prefs)
            val bank = prefs[starBankKey(pid)]?.toIntOrNull() ?: 0
            prefs[starBankKey(pid)] = (bank + count).toString()
        }
    }

    suspend fun redeemStars(count: Int) {
        context.dataStore.edit { prefs ->
            val pid = activePid(prefs)
            val bank = prefs[starBankKey(pid)]?.toIntOrNull() ?: 0
            prefs[starBankKey(pid)] = (bank - count).coerceAtLeast(0).toString()
        }
    }

    suspend fun setPin(pin: String) {
        context.dataStore.edit { prefs ->
            if (pin.length == 4 && pin.all { it.isDigit() }) prefs[pinKey] = pin
        }
    }

    suspend fun setKidMode(on: Boolean) {
        context.dataStore.edit { it[kidModeKey] = if (on) "on" else "off" }
    }

    suspend fun resetAll() {
        context.dataStore.edit { prefs ->
            val pid = activePid(prefs)
            prefs[progressKey(pid)] = ""
            if (pid == DEFAULT_PROFILE_ID) prefs.remove(legacyProgressKey)
        }
    }

    suspend fun renameProfile(id: String, name: String) {
        context.dataStore.edit { prefs ->
            val clean = sanitizeProfileName(name)
            if (clean.isBlank()) return@edit
            val updated = profilesOf(prefs).map { if (it.id == id) it.copy(name = clean) else it }
            prefs[profilesKey] = encodeProfiles(updated)
        }
    }

    suspend fun addProfile(name: String) {
        context.dataStore.edit { prefs ->
            val clean = sanitizeProfileName(name)
            if (clean.isBlank()) return@edit
            val current = profilesOf(prefs)
            val newProfile = Profile(nextProfileId(current), clean)
            prefs[profilesKey] = encodeProfiles(current + newProfile)
            prefs[activeProfileKey] = newProfile.id
        }
    }

    suspend fun switchProfile(id: String) {
        context.dataStore.edit { prefs ->
            if (profilesOf(prefs).any { it.id == id }) prefs[activeProfileKey] = id
        }
    }

    suspend fun deleteProfile(id: String) {
        context.dataStore.edit { prefs ->
            val current = profilesOf(prefs)
            if (current.size <= 1) return@edit // never delete the last profile
            val remaining = current.filterNot { it.id == id }
            prefs[profilesKey] = encodeProfiles(remaining)
            if (activePid(prefs) == id) prefs[activeProfileKey] = remaining.first().id
            prefs.remove(progressKey(id))
            prefs.remove(thresholdKey(id))
            prefs.remove(forceUnlockedKey(id))
            if (id == DEFAULT_PROFILE_ID) {
                prefs.remove(legacyProgressKey)
                prefs.remove(legacyThresholdKey)
            }
        }
    }
}
