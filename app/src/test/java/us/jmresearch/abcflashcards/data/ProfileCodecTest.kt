package us.jmresearch.abcflashcards.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileCodecTest {

    @Test fun roundTrip() {
        val profiles = listOf(Profile("p1", "Max"), Profile("p2", "Zoe"))
        assertEquals(profiles, decodeProfiles(encodeProfiles(profiles)))
    }

    @Test fun emptyAndBlankDecodeToEmpty() {
        assertEquals(emptyList<Profile>(), decodeProfiles(""))
    }

    @Test fun corruptEntriesDropped() {
        assertEquals(
            listOf(Profile("p1", "Max")),
            decodeProfiles("p1|Max;garbage;|noid"),
        )
    }

    @Test fun sanitizeStripsSeparators() {
        assertEquals("Max Jr", sanitizeProfileName("M|a;x Jr"))
    }

    @Test fun nextProfileIdSkipsExisting() {
        assertEquals("p1", nextProfileId(emptyList()))
        assertEquals("p3", nextProfileId(listOf(Profile("p1", "a"), Profile("p2", "b"))))
        assertEquals("p8", nextProfileId(listOf(Profile("p7", "a"))))
    }
}
