package us.jmresearch.abcflashcards.engine

import kotlin.random.Random
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.ItemProgress

fun pickNext(
    items: List<CardItem>,
    progress: Map<String, ItemProgress>,
    threshold: Int,
    lastShownId: String?,
    random: Random,
): CardItem? {
    if (items.isEmpty()) return null
    val candidates = items.filter { it.id != lastShownId }.ifEmpty { items }
    val (mastered, unmastered) = candidates.partition { isMastered(progress[it.id], threshold) }

    fun oldestMastered() = mastered.minBy { progress[it.id]?.lastSeenEpochDay ?: 0 }

    if (unmastered.isEmpty()) return oldestMastered()
    if (mastered.isNotEmpty() && random.nextInt(8) == 0) return oldestMastered()

    val weights = unmastered.map { item ->
        val count = progress[item.id]?.correctCount ?: 0
        (threshold - count).coerceAtLeast(1)
    }
    var roll = random.nextInt(weights.sum())
    for ((i, w) in weights.withIndex()) {
        roll -= w
        if (roll < 0) return unmastered[i]
    }
    return unmastered.last()
}
