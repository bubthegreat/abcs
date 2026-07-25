package us.jmresearch.abcflashcards.engine

import us.jmresearch.abcflashcards.data.Deck
import us.jmresearch.abcflashcards.data.ItemProgress
import us.jmresearch.abcflashcards.data.UnlockRule

fun applyCorrect(p: ItemProgress, today: Long): ItemProgress =
    p.copy(correctCount = p.correctCount + 1, lastSeenEpochDay = today)

fun applyWrong(p: ItemProgress, today: Long): ItemProgress =
    p.copy(correctCount = (p.correctCount - 1).coerceAtLeast(0), lastSeenEpochDay = today)

/**
 * Quiz wrong: break the streak so mastery means "threshold right in a row".
 *
 * Plain -1 let a guesser grind: the floor at zero is a reflecting barrier, so a
 * random walk with negative drift still reached the threshold given enough taps.
 *
 * A card already at or above threshold only steps back one instead of resetting.
 * Cards don't decay, so a mastered card can resurface, and deck completion needs
 * every card mastered at once — without this, one unlucky tap on a card the kid
 * genuinely knows could un-complete a finished deck.
 */
fun applyQuizWrong(p: ItemProgress, today: Long, threshold: Int): ItemProgress =
    p.copy(
        correctCount = if (p.correctCount >= threshold) (threshold - 1).coerceAtLeast(0) else 0,
        lastSeenEpochDay = today,
    )

fun isMastered(p: ItemProgress?, threshold: Int): Boolean =
    p != null && p.correctCount >= threshold

/** Generated decks master on a correct streak; static decks per item. */
fun deckItemThreshold(deck: Deck, threshold: Int): Int =
    if (deck.generator != null) us.jmresearch.abcflashcards.data.GENERATED_STREAK_TARGET else threshold

fun isDeckMastered(deck: Deck, progress: Map<String, ItemProgress>, threshold: Int): Boolean =
    deck.items.all { isMastered(progress[it.id], deckItemThreshold(deck, threshold)) }

fun isDeckUnlocked(
    deck: Deck,
    allDecks: List<Deck>,
    progress: Map<String, ItemProgress>,
    threshold: Int,
    forceUnlocked: Set<String>,
): Boolean {
    if (deck.id in forceUnlocked) return true
    return when (val rule = deck.unlockRule) {
        is UnlockRule.None -> true
        is UnlockRule.DecksMastered -> rule.deckIds.all { id ->
            val dep = allDecks.firstOrNull { it.id == id } ?: return false
            isDeckMastered(dep, progress, threshold)
        }
    }
}
