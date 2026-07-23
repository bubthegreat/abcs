package us.jmresearch.abcflashcards.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileCodecTest {

    @Test fun roundTrip() {
        val profiles = listOf(Profile("p1", "Max"), Profile("p2", "Zoe"))
        assertEquals(profiles, decodeProfiles(encodeProfiles(profiles)))
    }

    @Test fun birthdayRoundTripAndLegacyCompat() {
        val profiles = listOf(Profile("p1", "Max", 18000L), Profile("p2", "Zoe", null))
        assertEquals(profiles, decodeProfiles(encodeProfiles(profiles)))
        // pre-birthday entries decode with null birthday
        assertEquals(listOf(Profile("p1", "Max")), decodeProfiles("p1|Max"))
    }

    @Test fun ageComputesWholeYears() {
        val bday = 0L // epoch day zero
        assertEquals(4, ageOf(Profile("p1", "a", bday), (365.2425 * 4.5).toLong()))
        assertEquals(null, ageOf(Profile("p1", "a", null), 1000L))
        assertEquals(null, ageOf(Profile("p1", "a", 2000L), 1000L)) // future birthday
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
