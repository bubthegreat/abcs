package us.jmresearch.abcflashcards.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.Deck
import us.jmresearch.abcflashcards.data.ItemProgress
import us.jmresearch.abcflashcards.data.Subject
import us.jmresearch.abcflashcards.data.UnlockRule

class MasteryTest {

    private fun deck(id: String, vararg itemIds: String, rule: UnlockRule = UnlockRule.None) =
        Deck(id, id, Subject.LETTERS, itemIds.map { CardItem(it, it) }, rule)

    @Test fun correctIncrementsAndStampsDay() {
        val p = applyCorrect(ItemProgress(1, 10), today = 20)
        assertEquals(2, p.correctCount)
        assertEquals(20, p.lastSeenEpochDay)
    }

    @Test fun wrongDecrementsWithFloorAtZero() {
        assertEquals(0, applyWrong(ItemProgress(0, 0), 5).correctCount)
        assertEquals(1, applyWrong(ItemProgress(2, 0), 5).correctCount)
        assertEquals(5, applyWrong(ItemProgress(2, 0), 5).lastSeenEpochDay)
    }

    @Test fun masteryIsDerivedFromThreshold() {
        assertFalse(isMastered(ItemProgress(2), threshold = 3))
        assertTrue(isMastered(ItemProgress(3), threshold = 3))
        assertTrue(isMastered(ItemProgress(3), threshold = 2)) // lowering threshold masters retroactively
        assertFalse(isMastered(null, threshold = 3))
    }

    @Test fun deckMasteredRequiresAllItems() {
        val d = deck("d1", "a", "b")
        val partial = mapOf("a" to ItemProgress(3))
        val full = mapOf("a" to ItemProgress(3), "b" to ItemProgress(4))
        assertFalse(isDeckMastered(d, partial, 3))
        assertTrue(isDeckMastered(d, full, 3))
    }

    @Test fun unlockRules() {
        val d1 = deck("d1", "a")
        val d2 = deck("d2", "b", rule = UnlockRule.DecksMastered(listOf("d1")))
        val none = emptyMap<String, ItemProgress>()
        val d1Done = mapOf("a" to ItemProgress(3))
        assertTrue(isDeckUnlocked(d1, listOf(d1, d2), none, 3, emptySet()))
        assertFalse(isDeckUnlocked(d2, listOf(d1, d2), none, 3, emptySet()))
        assertTrue(isDeckUnlocked(d2, listOf(d1, d2), d1Done, 3, emptySet()))
        assertTrue(isDeckUnlocked(d2, listOf(d1, d2), none, 3, setOf("d2"))) // force-unlock
    }
}
