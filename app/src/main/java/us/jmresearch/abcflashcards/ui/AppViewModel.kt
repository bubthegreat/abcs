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

data class DeckStatus(
    val deck: Deck,
    val unlocked: Boolean,
    val masteredCount: Int,
    val total: Int,
    val quizUnlocked: Boolean,
    val quizMasteredCount: Int,
)

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
    val homework: Set<String> = emptySet(),
    val quizProgress: Map<String, ItemProgress> = emptyMap(),
    val homeworkRewards: Map<String, Int> = emptyMap(),
    val musicVolume: Float = 0.4f,
    val sfxVolume: Float = 0.7f,
)

fun AppState.rewardFor(activityId: String): Int = homeworkRewards[activityId] ?: 1

/** Special homework id for the writing/story activity (not a deck). */
const val WRITING_HOMEWORK_ID = "writing"

private data class Core(
    val progress: Map<String, ItemProgress>,
    val threshold: Int,
    val force: Set<String>,
    val homework: Set<String>,
    val quizProgress: Map<String, ItemProgress>,
    val homeworkRewards: Map<String, Int>,
)

private data class Profs(val profiles: List<Profile>, val activeId: String)

private data class KidBits(
    val bank: Int,
    val starProgress: Int,
    val pin: String?,
    val musicVolume: Float,
    val sfxVolume: Float,
)

const val CORRECTS_PER_STAR = 10

class AppViewModel(private val store: ProgressStore) : ViewModel() {

    private val random = Random(System.nanoTime())

    val state: StateFlow<AppState> =
        combine(
            combine(
                combine(store.progress, store.quizProgress) { p, q -> p to q },
                store.threshold,
                store.forceUnlocked,
                combine(store.homework, store.homeworkRewards) { h, r -> h to r },
            ) { (p, q), t, f, (h, r) -> Core(p, t, f, h, q, r) },
            combine(store.profiles, store.activeProfileId) { pr, id -> Profs(pr, id) },
            combine(store.starBank, store.starProgress, store.parentPin, store.musicVolume, store.sfxVolume) { b, s, pin, mv, sv ->
                KidBits(b, s, pin, mv, sv)
            },
        ) { core, profs, kid ->
            val statuses = Curriculum.decks.map { deck ->
                DeckStatus(
                    deck = deck,
                    unlocked = isDeckUnlocked(deck, Curriculum.decks, core.progress, core.threshold, core.force),
                    masteredCount = deck.items.count { isMastered(core.progress[it.id], core.threshold) },
                    total = deck.items.size,
                    quizUnlocked = isDeckUnlocked(deck, Curriculum.decks, core.quizProgress, core.threshold, core.force),
                    quizMasteredCount = deck.items.count { isMastered(core.quizProgress[it.id], core.threshold) },
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
                parentPin = kid.pin,
                musicVolume = kid.musicVolume,
                sfxVolume = kid.sfxVolume,
                homework = core.homework,
                quizProgress = core.quizProgress,
                homeworkRewards = core.homeworkRewards,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    private val _openDeckId = MutableStateFlow<String?>(null)
    val openDeckId: StateFlow<String?> = _openDeckId

    private val _currentCard = MutableStateFlow<CardItem?>(null)
    val currentCard: StateFlow<CardItem?> = _currentCard

    private val _currentQuiz = MutableStateFlow<Quiz?>(null)
    val currentQuiz: StateFlow<Quiz?> = _currentQuiz

    /** Bumps on every advance so the quiz UI resets even when the same card repeats. */
    private val _quizNonce = MutableStateFlow(0)
    val quizNonce: StateFlow<Int> = _quizNonce

    /** Story-in-progress lives here so tab switches and recompositions can't eat it. */
    // TEMP TEST SEED — remove after validating the story reward flow.
    private val _storySentences = MutableStateFlow<List<String>>(
        listOf(
            "The cat sat down.",
            "A big dog ran fast.",
            "I like red socks.",
            "The sun is hot.",
        ),
    )
    val storySentences: StateFlow<List<String>> = _storySentences

    private val _storyRewarded = MutableStateFlow(false)

    /**
     * Accepting the final sentence IS story completion — the homework reward
     * pays out right here, not when "Tell my story" is tapped.
     */
    fun addStorySentence(sentence: String) {
        val updated = _storySentences.value + sentence
        _storySentences.value = updated
        if (updated.size >= us.jmresearch.abcflashcards.data.SENTENCES_PER_STORY && !_storyRewarded.value) {
            _storyRewarded.value = true
            awardStoryStar()
        }
    }

    fun clearStory() {
        _storySentences.value = emptyList()
        _storyRewarded.value = false
    }

    /** True once the parent tapped "Keep practicing" on a fully mastered deck. */
    private val _reviewMode = MutableStateFlow(false)
    val reviewMode: StateFlow<Boolean> = _reviewMode

    /** True when the open deck was entered in quiz mode (separate progress track). */
    private var quizSession = false

    private fun deckById(id: String?): Deck? = Curriculum.decks.firstOrNull { it.id == id }

    fun openDeck(deckId: String, quiz: Boolean = false) {
        _openDeckId.value = deckId
        _reviewMode.value = false
        quizSession = quiz
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
        val progress = if (quizSession) s.quizProgress else s.progress
        val next = if (_reviewMode.value) {
            // Review of a mastered deck: uniform random over the whole deck, so it
            // doesn't ping-pong between the two oldest-seen items.
            pickNext(deck.items, emptyMap(), s.threshold, lastShownId, random)
        } else {
            pickNext(deck.items, progress, s.threshold, lastShownId, random)
        }
        _currentCard.value = next
        _currentQuiz.value = next?.let { buildQuiz(it, deck, random) }
        _quizNonce.value++
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

    /** Stars come ONLY from assigned homework. */
    private fun earnsStars(activityId: String?): Boolean =
        activityId != null && activityId in state.value.homework

    /** Quiz: correct tap. Advances quiz progress; feeds the star bank when the deck is homework. */
    fun quizCorrect() {
        val card = _currentCard.value ?: return
        val deckId = _openDeckId.value
        val deck = deckById(deckId)
        val today = LocalDate.now().toEpochDay()
        val s = state.value
        val earns = earnsStars(deckId)
        // Does this correct answer push the whole quiz deck over the line?
        val completesDeck = deck != null && !_reviewMode.value &&
            (s.quizProgress[card.id]?.correctCount ?: 0) + 1 >= s.threshold &&
            deck.items.all { it.id == card.id || isMastered(s.quizProgress[it.id], s.threshold) }
        viewModelScope.launch {
            store.updateQuizItem(card.id) { applyCorrect(it, today) }
            if (earns) {
                store.recordKidCorrect(CORRECTS_PER_STAR)
                if (completesDeck && deckId != null) store.addStars(s.rewardFor(deckId))
            }
            advance(lastShownId = card.id)
        }
    }

    /** Quiz: wrong tap, after the correct answer was revealed. Records and moves on. */
    fun quizWrongAdvance() {
        val card = _currentCard.value ?: return
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            store.updateQuizItem(card.id) { applyWrong(it, today) }
            advance(lastShownId = card.id)
        }
    }

    fun resetDeckLearning(deckId: String) {
        val deck = deckById(deckId) ?: return
        viewModelScope.launch {
            store.resetDeckLearning(deck.items.map { it.id })
            refreshOpenDeck(deckId)
        }
    }

    fun resetDeckQuiz(deckId: String) {
        val deck = deckById(deckId) ?: return
        viewModelScope.launch {
            store.resetDeckQuiz(deck.items.map { it.id })
            refreshOpenDeck(deckId)
        }
    }

    /** After an in-deck reset, deal a fresh card once the cleared progress lands. */
    private suspend fun refreshOpenDeck(deckId: String) {
        if (_openDeckId.value != deckId) return
        kotlinx.coroutines.delay(100)
        _reviewMode.value = false
        advance(lastShownId = null)
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

    fun setMusicVolume(volume: Float) {
        viewModelScope.launch { store.setMusicVolume(volume) }
    }

    fun setSfxVolume(volume: Float) {
        viewModelScope.launch { store.setSfxVolume(volume) }
    }

    fun redeemStars(count: Int) {
        viewModelScope.launch { store.redeemStars(count) }
    }

    /** Finishing a 5-sentence story earns the writing homework's reward. */
    fun awardStoryStar() {
        if (!earnsStars(WRITING_HOMEWORK_ID)) return
        viewModelScope.launch { store.addStars(state.value.rewardFor(WRITING_HOMEWORK_ID)) }
    }

    fun toggleHomework(activityId: String) {
        viewModelScope.launch {
            store.setHomework(activityId, activityId !in state.value.homework)
        }
    }

    fun setHomeworkReward(activityId: String, count: Int) {
        viewModelScope.launch { store.setHomeworkReward(activityId, count) }
    }

    fun resetStarProgress() {
        viewModelScope.launch { store.resetStarProgress() }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(ProgressStore(context.applicationContext)) }
        }
    }
}
