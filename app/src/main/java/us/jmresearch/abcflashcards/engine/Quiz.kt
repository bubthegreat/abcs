package us.jmresearch.abcflashcards.engine

import kotlin.random.Random
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.Curriculum
import us.jmresearch.abcflashcards.data.Deck
import us.jmresearch.abcflashcards.data.utteranceFor

/**
 * One self-checking question. Cards with a back (math, counting) show the
 * front and quiz on the back; cards without (letters, words, phrases) are
 * prompted by audio only and quiz on the front.
 */
data class Quiz(
    val item: CardItem,
    val visualPrompt: String?,
    val spokenPrompt: String,
    val choices: List<String>,
    val answer: String,
)

/**
 * Wrong answers offered alongside the right one. Three (a four-way choice) drops
 * a blind guess from 1-in-3 to 1-in-4. Decks too small to supply this many fall
 * back to however many they can — take() degrades on its own.
 */
const val QUIZ_DISTRACTORS = 3

fun buildQuiz(target: CardItem, deck: Deck, random: Random): Quiz {
    val quizOnBack = target.back != null
    val answer = if (quizOnBack) target.back!! else target.front

    fun valueOf(item: CardItem) = if (quizOnBack) item.back else item.front

    // Distractor pool: same deck first; same subject if the deck is too small.
    val sameDeck = deck.items.filter { it.id != target.id }
    val pool = if (sameDeck.size >= QUIZ_DISTRACTORS) sameDeck else {
        Curriculum.decks
            .filter { it.subject == deck.subject }
            .flatMap { it.items }
            .filter { it.id != target.id }
    }

    val distractors = pool
        .mapNotNull { valueOf(it) }
        .distinct()
        .filter { it != answer }
        .shuffled(random)
        .take(QUIZ_DISTRACTORS)

    return Quiz(
        item = target,
        visualPrompt = if (quizOnBack) target.front else null,
        spokenPrompt = utteranceFor(target, deck),
        choices = (distractors + answer).shuffled(random),
        answer = answer,
    )
}
