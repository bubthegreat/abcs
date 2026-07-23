package us.jmresearch.abcflashcards.data

const val WORDS_PER_SENTENCE = 3
const val SENTENCES_PER_STORY = 5

fun wordCount(s: String): Int = s.trim().split(Regex("\\s+")).count { it.isNotBlank() }

fun isValidSentence(s: String): Boolean = wordCount(s) >= WORDS_PER_SENTENCE

fun isStoryComplete(sentences: List<String>): Boolean =
    sentences.size >= SENTENCES_PER_STORY &&
        sentences.take(SENTENCES_PER_STORY).all { isValidSentence(it) }

fun startsWithCapital(s: String): Boolean = s.trim().firstOrNull()?.isUpperCase() == true

fun endsWithPunctuation(s: String): Boolean = s.trim().lastOrNull() in setOf('.', '!', '?')

private fun normalized(s: String): String =
    s.lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ").trim()

fun distinctWordCount(s: String): Int =
    normalized(s).split(" ").filter { it.isNotBlank() }.toSet().size

/**
 * First problem with the sentence, or null when it's good.
 * Order: length, repeated words, capital, period, duplicate of an earlier sentence.
 */
fun sentenceProblem(s: String, existing: List<String> = emptyList()): String? = when {
    !isValidSentence(s) -> "A sentence needs at least $WORDS_PER_SENTENCE words! (I read ${wordCount(s)})"
    distinctWordCount(s) < WORDS_PER_SENTENCE -> "Use $WORDS_PER_SENTENCE DIFFERENT words — no repeating the same word!"
    !startsWithCapital(s) -> "Start your sentence with a CAPITAL letter!"
    !endsWithPunctuation(s) -> "Don't forget the period at the end!"
    existing.any { normalized(it) == normalized(s) } -> "You already wrote that sentence! Try a new one."
    else -> null
}
