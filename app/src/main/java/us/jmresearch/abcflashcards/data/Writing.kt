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

/** First problem with the sentence, or null when it's good. Order: length, capital, period. */
fun sentenceProblem(s: String): String? = when {
    !isValidSentence(s) -> "A sentence needs at least $WORDS_PER_SENTENCE words! (I read ${wordCount(s)})"
    !startsWithCapital(s) -> "Start your sentence with a CAPITAL letter!"
    !endsWithPunctuation(s) -> "Don't forget the period at the end!"
    else -> null
}
