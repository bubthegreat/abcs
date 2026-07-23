package us.jmresearch.abcflashcards.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jmresearch.abcflashcards.data.MathOp

class MathGenTest {

    private fun parse(front: String, symbol: String): Pair<Int, Int> {
        val parts = front.split(symbol).map { it.trim().toInt() }
        return parts[0] to parts[1]
    }

    @Test fun generatedProblemsAreAlwaysCorrect() {
        repeat(200) { seed ->
            val r = Random(seed)
            genMathCard(MathOp.ADD, "addition", r).let {
                val (a, b) = parse(it.front, "+")
                assertEquals((a + b).toString(), it.back)
                assertTrue(a + b <= 20)
            }
            genMathCard(MathOp.SUB, "subtraction", r).let {
                val (a, b) = parse(it.front, "−")
                assertEquals((a - b).toString(), it.back)
                assertTrue(a - b >= 0)
            }
            genMathCard(MathOp.MUL, "multiplication", r).let {
                val (a, b) = parse(it.front, "×")
                assertEquals((a * b).toString(), it.back)
                assertTrue(a <= 10 && b <= 10)
            }
            genMathCard(MathOp.DIV, "division", r).let {
                val (a, b) = parse(it.front, "÷")
                assertEquals(0, a % b)
                assertEquals((a / b).toString(), it.back)
            }
            genMathCard(MathOp.TABLES, "times_tables", r).let {
                val (a, b) = parse(it.front, "×")
                assertEquals((a * b).toString(), it.back)
                assertTrue(a <= 20 && b <= 20)
            }
        }
    }

    @Test fun generatedCardsShareTheirDeckStreakId() {
        val c1 = genMathCard(MathOp.ADD, "addition", Random(1))
        val c2 = genMathCard(MathOp.ADD, "addition", Random(2))
        assertEquals("gen_addition", c1.id)
        assertEquals(c1.id, c2.id)
    }

    @Test fun numericQuizChoicesAreDistinctAndContainAnswer() {
        repeat(50) { seed ->
            val card = genMathCard(MathOp.TABLES, "times_tables", Random(seed))
            val quiz = buildGeneratedQuiz(card, Random(seed))
            assertEquals(3, quiz.choices.size)
            assertEquals(quiz.choices.size, quiz.choices.toSet().size)
            assertTrue(quiz.choices.contains(card.back))
            assertEquals(card.back, quiz.answer)
            assertEquals(card.front, quiz.visualPrompt)
            quiz.choices.forEach { assertTrue(it.toInt() >= 0) }
        }
    }
}
