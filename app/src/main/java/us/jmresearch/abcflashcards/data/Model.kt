package us.jmresearch.abcflashcards.data

data class CardItem(val id: String, val front: String, val back: String? = null)

enum class Subject { COLORS, LETTERS, WORDS, PHRASES, LANGUAGE, MATH }

sealed interface UnlockRule {
    data object None : UnlockRule
    data class DecksMastered(val deckIds: List<String>) : UnlockRule
}

data class Deck(
    val id: String,
    val title: String,
    val subject: Subject,
    val items: List<CardItem>,
    val unlockRule: UnlockRule = UnlockRule.None,
)

data class ItemProgress(val correctCount: Int = 0, val lastSeenEpochDay: Long = 0)
