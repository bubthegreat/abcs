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
}
