package us.jmresearch.abcflashcards.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.ItemProgress

class CardPickerTest {

    private val items = listOf(CardItem("a", "a"), CardItem("b", "b"), CardItem("c", "c"))

    @Test fun emptyDeckReturnsNull() {
        assertNull(pickNext(emptyList(), emptyMap(), 3, null, Random(1)))
    }

    @Test fun neverRepeatsLastShownWhenAlternativesExist() {
        repeat(50) { seed ->
            val pick = pickNext(items, emptyMap(), 3, lastShownId = "a", random = Random(seed))
            assertNotEquals("a", pick!!.id)
        }
    }

    @Test fun singleItemDeckMayRepeat() {
        val one = listOf(CardItem("a", "a"))
        assertEquals("a", pickNext(one, emptyMap(), 3, lastShownId = "a", random = Random(1))!!.id)
    }

    @Test fun allMasteredServesOldestSeen() {
        val progress = mapOf(
            "a" to ItemProgress(3, lastSeenEpochDay = 100),
            "b" to ItemProgress(3, lastSeenEpochDay = 50),
            "c" to ItemProgress(3, lastSeenEpochDay = 75),
        )
        assertEquals("b", pickNext(items, progress, 3, null, Random(1))!!.id)
    }

    @Test fun spacedReviewRollServesMasteredItem() {
        val seed = generateSequence(0) { it + 1 }.first { Random(it).nextInt(8) == 0 }
        val progress = mapOf("a" to ItemProgress(3, lastSeenEpochDay = 10))
        val pick = pickNext(items, progress, 3, null, Random(seed))
        assertEquals("a", pick!!.id)
    }

    @Test fun unmasteredPicksFavorLowCounts() {
        // "a" has count 0 (weight 3), "b" count 2 (weight 1); over many draws "a" must dominate.
        val two = listOf(CardItem("a", "a"), CardItem("b", "b"))
        val progress = mapOf("b" to ItemProgress(2))
        var aWins = 0
        var draws = 0
        repeat(1000) { seed ->
            draws++
            if (pickNext(two, progress, 3, null, Random(seed))!!.id == "a") aWins++
        }
        // expected share = 3/4 of draws; require comfortably above 1/2
        assertTrue("expected a to win most draws, won $aWins/$draws", aWins > 600)
    }
}
