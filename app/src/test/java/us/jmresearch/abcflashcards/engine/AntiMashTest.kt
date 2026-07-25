package us.jmresearch.abcflashcards.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jmresearch.abcflashcards.data.ItemProgress

/**
 * Guards the anti-guessing rules. See
 * docs/superpowers/specs/2026-07-25-anti-mash-quiz-design.md
 *
 * The point of these rules is that a lucky tap earns nothing while a genuinely
 * known answer is never discounted, so the simulations below matter more than
 * the unit tests: they check the arithmetic actually separates the two.
 */
class AntiMashTest {

    private val today = 100L

    // --- applyQuizWrong ---

    @Test fun wrongBreaksAnUnmasteredStreak() {
        val p = ItemProgress(correctCount = 2)
        assertEquals(0, applyQuizWrong(p, today, threshold = 3).correctCount)
    }

    @Test fun wrongOnlyStepsBackAMasteredCard() {
        val p = ItemProgress(correctCount = 3)
        assertEquals(2, applyQuizWrong(p, today, threshold = 3).correctCount)
    }

    @Test fun wrongOnAnOverfilledCardStillLandsJustBelowThreshold() {
        val p = ItemProgress(correctCount = 9)
        assertEquals(2, applyQuizWrong(p, today, threshold = 3).correctCount)
    }

    @Test fun wrongAtZeroStaysAtZero() {
        assertEquals(0, applyQuizWrong(ItemProgress(), today, threshold = 3).correctCount)
    }

    @Test fun wrongRecordsTheDay() {
        assertEquals(today, applyQuizWrong(ItemProgress(correctCount = 1), today, 3).lastSeenEpochDay)
    }

    // --- star drip ---

    @Test fun tenCorrectsBankAStar() {
        var d = StarDrip(progress = 0, bank = 0)
        repeat(10) { d = dripCorrect(d, 10) }
        assertEquals(1, d.bank)
        assertEquals(0, d.progress)
    }

    @Test fun wrongStepsProgressBackButNeverTheBank() {
        val d = dripWrong(StarDrip(progress = 0, bank = 4))
        assertEquals(0, d.progress)
        assertEquals("a banked star must never be taken away", 4, d.bank)
    }

    // --- simulations: the actual proof ---

    private data class Outcome(val stars: Int, val mastered: Int)

    /**
     * Play [questions] answers at a fixed [accuracy] and report what was earned.
     *
     * With [newRules] off this models the behaviour these changes replaced: a
     * wrong answer cost a card one point and cost star progress nothing. The
     * tests below compare the two, because the design's claim is a large drop in
     * what guessing yields — not that guessing yields literally zero. Over
     * thousands of taps a guesser still gets lucky occasionally; the point is
     * that it stops being worth doing.
     */
    private fun run(
        accuracy: Double,
        questions: Int,
        seed: Int,
        newRules: Boolean,
        threshold: Int = 3,
    ): Outcome {
        val random = Random(seed)
        var drip = StarDrip(progress = 0, bank = 0)
        var mastered = 0
        var card = ItemProgress()
        repeat(questions) {
            if (random.nextDouble() < accuracy) {
                card = applyCorrect(card, today)
                drip = dripCorrect(drip, 10)
                if (card.correctCount >= threshold) {
                    mastered++
                    card = ItemProgress() // move on to the next card
                }
            } else {
                card = if (newRules) {
                    applyQuizWrong(card, today, threshold)
                } else {
                    applyWrong(card, today)
                }
                if (newRules) drip = dripWrong(drip)
            }
        }
        return Outcome(drip.bank, mastered)
    }

    private fun totals(accuracy: Double, questions: Int, newRules: Boolean): Outcome {
        var stars = 0
        var mastered = 0
        (0 until 20).forEach { seed ->
            val o = run(accuracy, questions, seed, newRules)
            stars += o.stars
            mastered += o.mastered
        }
        return Outcome(stars, mastered)
    }

    /** The exploit that started this: a star roughly every 30 taps, forever. */
    @Test fun theStarFarmIsClosedForAFourChoiceGuesser() {
        val before = totals(accuracy = 0.25, questions = 2000, newRules = false)
        val after = totals(accuracy = 0.25, questions = 2000, newRules = true)
        assertTrue("baseline should show the farm, got ${before.stars}", before.stars > 500)
        assertTrue(
            "guessing must stop paying: ${after.stars} vs ${before.stars}",
            after.stars * 50 < before.stars,
        )
    }

    /** Three-choice decks suppress the farm heavily, though not absolutely. */
    @Test fun theStarFarmIsAllButClosedForAThreeChoiceGuesser() {
        val before = totals(accuracy = 1.0 / 3.0, questions = 2000, newRules = false)
        val after = totals(accuracy = 1.0 / 3.0, questions = 2000, newRules = true)
        assertTrue(
            "guessing must stop paying: ${after.stars} vs ${before.stars}",
            after.stars * 20 < before.stars,
        )
    }

    @Test fun grindingMasteryGetsHarder() {
        val before = totals(accuracy = 0.25, questions = 2000, newRules = false)
        val after = totals(accuracy = 0.25, questions = 2000, newRules = true)
        assertTrue(
            "streak rule should cut lucky masteries: ${after.mastered} vs ${before.mastered}",
            after.mastered < before.mastered,
        )
    }

    /**
     * The regression guard on "never discount a real answer". A kid who knows the
     * material must keep earning at close to the old rate — this is the test that
     * would fail if the anti-guessing rules were ever tightened too far.
     */
    @Test fun aRealLearnerIsBarelyAffected() {
        val before = totals(accuracy = 0.9, questions = 300, newRules = false)
        val after = totals(accuracy = 0.9, questions = 300, newRules = true)
        assertTrue(
            "a 90% kid must keep earning: ${after.stars} vs ${before.stars}",
            after.stars >= before.stars * 0.75,
        )
        assertTrue(
            "a 90% kid must keep mastering: ${after.mastered} vs ${before.mastered}",
            after.mastered >= before.mastered * 0.75,
        )
    }

    @Test fun aShakyLearnerStillMakesProgress() {
        // 70% is a kid genuinely trying who partly knows it.
        val after = totals(accuracy = 0.7, questions = 500, newRules = true)
        assertTrue("a 70% kid should still bank stars, got ${after.stars}", after.stars > 0)
        assertTrue("a 70% kid should still master cards, got ${after.mastered}", after.mastered > 0)
    }
}
