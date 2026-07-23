package us.jmresearch.abcflashcards.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.Curriculum

class QuizTest {

    private fun deck(id: String) = Curriculum.decks.first { it.id == id }

    @Test fun lettersQuizUsesFrontsAndContainsTarget() {
        val d = deck("letters_1")
        val target = d.items.first()
        repeat(20) { seed ->
            val q = buildQuiz(target, d, Random(seed))
            assertEquals(3, q.choices.size)
            assertEquals(1, q.choices.count { it == q.answer })
            assertEquals(target.front, q.answer)
            assertEquals(q.choices.size, q.choices.toSet().size) // no duplicate choices
            assertTrue(q.spokenPrompt.isNotBlank())
            assertEquals(null, q.visualPrompt) // audio-only prompt for letters
        }
    }

    @Test fun mathQuizUsesBacksAndShowsFront() {
        val d = deck("fractions")
        val target = d.items.first { it.id == "frac_1_2_8" }
        repeat(20) { seed ->
            val q = buildQuiz(target, d, Random(seed))
            assertEquals("1/2 of 8", q.visualPrompt)
            assertEquals("4", q.answer)
            assertTrue(q.choices.contains("4"))
            assertEquals(3, q.choices.size)
            assertEquals(q.choices.size, q.choices.toSet().size)
        }
    }

    @Test fun tinyDeckPullsDistractorsFromSameSubject() {
        val d = deck("letters_5") // only "Qu qu"
        val target = d.items.first()
        val q = buildQuiz(target, d, Random(1))
        assertEquals(3, q.choices.size)
        assertTrue(q.choices.contains(target.front))
    }

    @Test fun choicesAreShuffledAcrossSeeds() {
        val d = deck("letters_1")
        val target = d.items.first()
        val positions = (0 until 30).map { seed ->
            buildQuiz(target, d, Random(seed)).choices.indexOf(target.front)
        }.toSet()
        assertTrue("answer should appear at different positions", positions.size > 1)
    }
}
