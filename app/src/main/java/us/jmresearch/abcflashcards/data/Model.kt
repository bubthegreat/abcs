package us.jmresearch.abcflashcards.data

data class CardItem(val id: String, val front: String, val back: String? = null)

enum class Subject { COLORS, LETTERS, WORDS, PHRASES, LANGUAGE, MATH }

sealed interface UnlockRule {
    data object None : UnlockRule
    data class DecksMastered(val deckIds: List<String>) : UnlockRule
}

/** Operation for endlessly-generated math decks. */
enum class MathOp { ADD, SUB, MUL, DIV, TABLES }

data class Deck(
    val id: String,
    val title: String,
    val subject: Subject,
    val items: List<CardItem>,
    val unlockRule: UnlockRule = UnlockRule.None,
    /** Non-null = problems are generated at random; mastery is a correct streak. */
    val generator: MathOp? = null,
)

/** Correct answers IN A ROW needed to master a generated deck. */
const val GENERATED_STREAK_TARGET = 10

/** The single synthetic progress id a generated deck tracks its streak under. */
fun generatedItemId(deckId: String): String = "gen_$deckId"

data class ItemProgress(val correctCount: Int = 0, val lastSeenEpochDay: Long = 0)
