package us.jmresearch.abcflashcards.engine

import us.jmresearch.abcflashcards.data.Deck
import us.jmresearch.abcflashcards.data.ItemProgress
import us.jmresearch.abcflashcards.data.UnlockRule

fun applyCorrect(p: ItemProgress, today: Long): ItemProgress =
    p.copy(correctCount = p.correctCount + 1, lastSeenEpochDay = today)

fun applyWrong(p: ItemProgress, today: Long): ItemProgress =
    p.copy(correctCount = (p.correctCount - 1).coerceAtLeast(0), lastSeenEpochDay = today)

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
