package us.jmresearch.abcflashcards.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingTest {

    @Test fun sentenceNeedsAtLeastThreeWords() {
        assertFalse(isValidSentence(""))
        assertFalse(isValidSentence("cat"))
        assertFalse(isValidSentence("the cat"))
        assertTrue(isValidSentence("the cat sat"))
        assertTrue(isValidSentence("  the   cat   sat  ")) // whitespace noise tolerated
        assertTrue(isValidSentence("my dog eats loud socks"))
    }

    @Test fun storyNeedsFiveValidSentences() {
        val four = List(4) { "the cat sat" }
        val five = List(5) { "the cat sat" }
        val fiveOneShort = List(4) { "the cat sat" } + "too short"
        assertFalse(isStoryComplete(four))
        assertTrue(isStoryComplete(five))
        assertFalse(isStoryComplete(fiveOneShort))
    }

    @Test fun wordCountCountsWords() {
        assertEquals(0, wordCount("   "))
        assertEquals(3, wordCount("the cat sat"))
    }

    @Test fun sentenceProblemChecksLengthThenCapitalThenPeriod() {
        assertTrue(sentenceProblem("the cat")!!.contains("3 words"))
        assertTrue(sentenceProblem("the cat sat.")!!.contains("CAPITAL"))
        assertTrue(sentenceProblem("The cat sat")!!.contains("period"))
        assertEquals(null, sentenceProblem("The cat sat."))
        assertEquals(null, sentenceProblem("Do dogs eat socks?"))
        assertEquals(null, sentenceProblem("I like loud trucks!"))
    }

    @Test fun repeatedWordsDoNotCount() {
        assertTrue(sentenceProblem("Poo poo poo.")!!.contains("DIFFERENT"))
        assertTrue(sentenceProblem("Poo poo poo poo poo.")!!.contains("DIFFERENT"))
        assertEquals(null, sentenceProblem("Poo is stinky.")) // 3 distinct words is fine
    }

    @Test fun duplicateSentencesAreRejected() {
        val existing = listOf("The cat sat.")
        assertTrue(sentenceProblem("The cat sat.", existing)!!.contains("already"))
        assertTrue(sentenceProblem("the CAT sat", existing)!!.contains("CAPITAL")) // capital checked before dup
        assertTrue(sentenceProblem("The cat sat!", existing)!!.contains("already")) // punctuation ignored in compare
        assertEquals(null, sentenceProblem("The dog sat.", existing))
    }
}
