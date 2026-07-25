package us.jmresearch.abcflashcards.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarGatingTest {

    @Test
    fun `activity not assigned as homework never earns`() {
        val state = AppState(homework = emptySet())
        assertFalse(state.canEarn("letters_satpin"))
    }

    @Test
    fun `empty homework means nothing earns, not everything`() {
        // Regression: the old rule was "empty homework = everything earns",
        // which let kids bank stars from plain flashcard practice.
        val state = AppState(homework = emptySet())
        assertFalse(state.canEarn("letters_satpin"))
        assertFalse(state.canEarn(WRITING_HOMEWORK_ID))
    }

    @Test
    fun `assigned homework earns until daily limit reached`() {
        val id = "letters_satpin"
        val assigned = AppState(homework = setOf(id))
        assertTrue(assigned.canEarn(id))

        val atLimit = assigned.copy(earnedToday = mapOf(id to 1))
        assertFalse(atLimit.canEarn(id)) // default limit is once per day
    }

    @Test
    fun `endless limit keeps earning`() {
        val id = "letters_satpin"
        val state = AppState(
            homework = setOf(id),
            dailyLimits = mapOf(id to -1),
            earnedToday = mapOf(id to 99),
        )
        assertTrue(state.canEarn(id))
    }

    @Test
    fun `only the assigned activity earns, not siblings' assignments`() {
        val state = AppState(homework = setOf("cvc_words"))
        assertTrue(state.canEarn("cvc_words"))
        assertFalse(state.canEarn("letters_satpin"))
    }

    @Test
    fun `default reward and limit`() {
        val state = AppState()
        assertEquals(1, state.rewardFor("anything"))
        assertEquals(1, state.limitFor("anything"))
    }
}
