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
import us.jmresearch.abcflashcards.engine.Quiz
import us.jmresearch.abcflashcards.engine.applyCorrect
import us.jmresearch.abcflashcards.engine.applyWrong
import us.jmresearch.abcflashcards.engine.buildQuiz
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
    val starBank: Int = 0,
    val starProgress: Int = 0,
    val kidMode: Boolean = false,
    val parentPin: String? = null,
)

private data class Core(
    val progress: Map<String, ItemProgress>,
    val threshold: Int,
    val force: Set<String>,
)

private data class Profs(val profiles: List<Profile>, val activeId: String)

private data class KidBits(
    val bank: Int,
    val starProgress: Int,
    val kidMode: Boolean,
    val pin: String?,
)

const val CORRECTS_PER_STAR = 10

class AppViewModel(private val store: ProgressStore) : ViewModel() {

    private val random = Random(System.nanoTime())

    val state: StateFlow<AppState> =
        combine(
            combine(store.progress, store.threshold, store.forceUnlocked) { p, t, f -> Core(p, t, f) },
            combine(store.profiles, store.activeProfileId) { pr, id -> Profs(pr, id) },
            combine(store.starBank, store.starProgress, store.kidMode, store.parentPin) { b, s, k, pin ->
                KidBits(b, s, k, pin)
            },
        ) { core, profs, kid ->
            val statuses = Curriculum.decks.map { deck ->
                DeckStatus(
                    deck = deck,
                    unlocked = isDeckUnlocked(deck, Curriculum.decks, core.progress, core.threshold, core.force),
                    masteredCount = deck.items.count { isMastered(core.progress[it.id], core.threshold) },
                    total = deck.items.size,
                )
            }
            AppState(
                deckStatuses = statuses,
                threshold = core.threshold,
                forceUnlocked = core.force,
                progress = core.progress,
                profiles = profs.profiles,
                activeProfileId = profs.activeId,
                starBank = kid.bank,
                starProgress = kid.starProgress,
                kidMode = kid.kidMode,
                parentPin = kid.pin,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    private val _openDeckId = MutableStateFlow<String?>(null)
    val openDeckId: StateFlow<String?> = _openDeckId

    private val _currentCard = MutableStateFlow<CardItem?>(null)
    val currentCard: StateFlow<CardItem?> = _currentCard

    private val _currentQuiz = MutableStateFlow<Quiz?>(null)
    val currentQuiz: StateFlow<Quiz?> = _currentQuiz

    /** True once the parent tapped "Keep practicing" on a fully mastered deck. */
    private val _reviewMode = MutableStateFlow(false)
    val reviewMode: StateFlow<Boolean> = _reviewMode

    private var wrongAlreadyThisCard = false

    private fun deckById(id: String?): Deck? = Curriculum.decks.firstOrNull { it.id == id }

    fun openDeck(deckId: String) {
        _openDeckId.value = deckId
        _reviewMode.value = false
        advance(lastShownId = null)
    }

    fun closeDeck() {
        _openDeckId.value = null
        _currentCard.value = null
        _currentQuiz.value = null
        _reviewMode.value = false
    }

    fun keepPracticing() {
        _reviewMode.value = true
        advance(lastShownId = _currentCard.value?.id)
    }

    private fun advance(lastShownId: String?) {
        val deck = deckById(_openDeckId.value) ?: return
        val s = state.value
        wrongAlreadyThisCard = false
        val next = if (_reviewMode.value) {
            // Review of a mastered deck: uniform random over the whole deck, so it
            // doesn't ping-pong between the two oldest-seen items.
            pickNext(deck.items, emptyMap(), s.threshold, lastShownId, random)
        } else {
            pickNext(deck.items, s.progress, s.threshold, lastShownId, random)
        }
        _currentCard.value = next
        _currentQuiz.value = next?.let { buildQuiz(it, deck, random) }
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

    /** Kid mode: correct tap. Advances and feeds the star bank. */
    fun quizCorrect() {
        val card = _currentCard.value ?: return
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            store.updateItem(card.id) { applyCorrect(it, today) }
            store.recordKidCorrect(CORRECTS_PER_STAR)
            advance(lastShownId = card.id)
        }
    }

    /** Kid mode: wrong tap. Walks mastery back once per card; card stays up for retry. */
    fun quizWrong() {
        if (wrongAlreadyThisCard) return
        wrongAlreadyThisCard = true
        val card = _currentCard.value ?: return
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            store.updateItem(card.id) { applyWrong(it, today) }
        }
    }

    fun resetDeck(deckId: String) {
        val deck = deckById(deckId) ?: return
        viewModelScope.launch { store.resetDeck(deck.items.map { it.id }) }
    }

    fun resetAll() {
        viewModelScope.launch { store.resetAll() }
    }

    fun renameProfile(id: String, name: String) {
        viewModelScope.launch { store.renameProfile(id, name) }
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

    fun setPin(pin: String) {
        viewModelScope.launch { store.setPin(pin) }
    }

    fun enterKidMode() {
        closeDeck()
        viewModelScope.launch { store.setKidMode(true) }
    }

    /** Returns false when the pin doesn't match; kid mode stays on. */
    fun exitKidMode(pin: String): Boolean {
        if (pin != state.value.parentPin) return false
        closeDeck()
        viewModelScope.launch { store.setKidMode(false) }
        return true
    }

    fun redeemStars(count: Int) {
        viewModelScope.launch { store.redeemStars(count) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(ProgressStore(context.applicationContext)) }
        }
    }
}
