package us.jmresearch.abcflashcards.data

const val WORDS_PER_SENTENCE = 3
const val SENTENCES_PER_STORY = 5

fun wordCount(s: String): Int = s.trim().split(Regex("\\s+")).count { it.isNotBlank() }

fun isValidSentence(s: String): Boolean = wordCount(s) >= WORDS_PER_SENTENCE

fun isStoryComplete(sentences: List<String>): Boolean =
    sentences.size >= SENTENCES_PER_STORY &&
        sentences.take(SENTENCES_PER_STORY).all { isValidSentence(it) }
