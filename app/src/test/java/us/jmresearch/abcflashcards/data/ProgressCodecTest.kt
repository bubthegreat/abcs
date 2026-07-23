package us.jmresearch.abcflashcards.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCodecTest {

    @Test fun roundTrip() {
        val map = mapOf(
            "letter_s" to ItemProgress(3, 19923),
            "add_2_3" to ItemProgress(1, 20000),
        )
        assertEquals(map, decodeProgress(encodeProgress(map)))
    }

    @Test fun emptyRoundTrip() {
        assertEquals(emptyMap<String, ItemProgress>(), decodeProgress(encodeProgress(emptyMap())))
        assertEquals(emptyMap<String, ItemProgress>(), decodeProgress(""))
    }

    @Test fun corruptEntriesDroppedNotCrashed() {
        val decoded = decodeProgress("letter_s:3:100;garbage;word_sat:x:5;word_pat:2:50")
        assertEquals(ItemProgress(3, 100), decoded["letter_s"])
        assertEquals(ItemProgress(2, 50), decoded["word_pat"])
        assertTrue("garbage entries must be dropped", decoded.size == 2)
    }
}
