package us.jmresearch.abcflashcards.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.Curriculum
import us.jmresearch.abcflashcards.data.Deck
import us.jmresearch.abcflashcards.data.ItemProgress
import us.jmresearch.abcflashcards.data.Profile
import us.jmresearch.abcflashcards.data.ProgressStore
import us.jmresearch.abcflashcards.engine.applyCorrect
import us.jmresearch.abcflashcards.engine.applyWrong
import us.jmresearch.abcflashcards.engine.isDeckUnlocked
import us.jmresearch.abcflashcards.engine.isMastered
import us.jmresearch.abcflashcards.engine.pickNext

data class DeckStatus(val deck: Deck, val unlocked: Boolean, val masteredCount: Int, val total: Int)

data class AppState(
    val deckStatuses: List<DeckStatus> = emptyList(),
    val threshold: Int = 3,
    val forceUnlocked: Set<String> = emptySet(),
    val progress: Map<String, ItemProgress> = emptyMap(),
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: String = "p1",
)

class AppViewModel(private val store: ProgressStore) : ViewModel() {

    private val random = Random(System.nanoTime())

    val state: StateFlow<AppState> =
        combine(
            store.progress, store.threshold, store.forceUnlocked,
            store.profiles, store.activeProfileId,
        ) { progress, threshold, force, profiles, activePid ->
            val statuses = Curriculum.decks.map { deck ->
                DeckStatus(
                    deck = deck,
                    unlocked = isDeckUnlocked(deck, Curriculum.decks, progress, threshold, force),
                    masteredCount = deck.items.count { isMastered(progress[it.id], threshold) },
                    total = deck.items.size,
                )
            }
            AppState(statuses, threshold, force, progress, profiles, activePid)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    private val _openDeckId = MutableStateFlow<String?>(null)
    val openDeckId: StateFlow<String?> = _openDeckId

    private val _currentCard = MutableStateFlow<CardItem?>(null)
    val currentCard: StateFlow<CardItem?> = _currentCard

    /** True once the parent tapped "Keep practicing" on a fully mastered deck. */
    private val _reviewMode = MutableStateFlow(false)
    val reviewMode: StateFlow<Boolean> = _reviewMode

    private fun deckById(id: String?): Deck? = Curriculum.decks.firstOrNull { it.id == id }

    fun openDeck(deckId: String) {
        _openDeckId.value = deckId
        _reviewMode.value = false
        advance(lastShownId = null)
    }

    fun closeDeck() {
        _openDeckId.value = null
        _currentCard.value = null
        _reviewMode.value = false
    }

    fun keepPracticing() {
        _reviewMode.value = true
        advance(lastShownId = _currentCard.value?.id)
    }

    private fun advance(lastShownId: String?) {
        val deck = deckById(_openDeckId.value) ?: return
        val s = state.value
        _currentCard.value = pickNext(deck.items, s.progress, s.threshold, lastShownId, random)
    }

    fun markCorrect() = mark(::applyCorrect)
    fun markWrong() = mark(::applyWrong)

    private fun mark(transform: (ItemProgress, Long) -> ItemProgress) {
        val card = _currentCard.value ?: return
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            store.updateItem(card.id) { transform(it, today) }
            advance(lastShownId = card.id)
        }
    }

    fun resetDeck(deckId: String) {
        val deck = deckById(deckId) ?: return
        viewModelScope.launch { store.resetDeck(deck.items.map { it.id }) }
    }

    fun setThreshold(value: Int) {
        viewModelScope.launch { store.setThreshold(value) }
    }

    fun toggleForceUnlock(deckId: String) {
        viewModelScope.launch {
            store.setForceUnlocked(deckId, deckId !in state.value.forceUnlocked)
        }
    }

    fun addProfile(name: String) {
        viewModelScope.launch { store.addProfile(name) }
    }

    fun switchProfile(id: String) {
        closeDeck()
        viewModelScope.launch { store.switchProfile(id) }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch { store.deleteProfile(id) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(ProgressStore(context.applicationContext)) }
        }
    }
}
