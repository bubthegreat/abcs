package us.jmresearch.abcflashcards.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumTest {

    private val decks = Curriculum.decks
    private fun deck(id: String) = decks.first { it.id == id }

    private fun lettersOfGroups(vararg ids: String): Set<Char> =
        ids.flatMap { deck(it).items }
            .map { it.id.removePrefix("letter_").first() }
            .toSet()

    @Test fun allDeckIdsUniqueAndAllItemIdsUnique() {
        assertEquals(decks.size, decks.map { it.id }.toSet().size)
        val itemIds = decks.flatMap { d -> d.items.map { it.id } }
        assertEquals(itemIds.size, itemIds.toSet().size)
    }

    @Test fun expectedDecksExist() {
        val expected = listOf(
            "letters_1", "letters_2", "letters_3", "letters_4", "letters_5", "letters_review",
            "cvc_1", "cvc_2", "cvc_3", "sight_1", "phrases_1", "phrases_2",
            "numbers", "counting", "addition", "subtraction", "multiplication", "division",
        )
        expected.forEach { id -> assertTrue("missing deck $id", decks.any { it.id == id }) }
    }

    @Test fun cvcWordsAreDecodableFromPrerequisiteLetterGroups() {
        val prereqs = mapOf(
            "cvc_1" to lettersOfGroups("letters_1"),
            "cvc_2" to lettersOfGroups("letters_1", "letters_2"),
            "cvc_3" to lettersOfGroups("letters_1", "letters_2", "letters_3"),
        )
        prereqs.forEach { (deckId, allowed) ->
            deck(deckId).items.forEach { item ->
                val word = item.front
                assertTrue(
                    "word '$word' in $deckId uses letters outside $allowed",
                    word.all { it in allowed },
                )
            }
        }
    }

    @Test fun phrasesUseOnlyWordsFromPrerequisiteDecks() {
        val sight = deck("sight_1").items.map { it.front.lowercase() }.toSet()
        val prereqs = mapOf(
            "phrases_1" to deck("cvc_1").items.map { it.front }.toSet() + sight,
            "phrases_2" to (deck("cvc_1").items + deck("cvc_2").items).map { it.front }.toSet() + sight,
        )
        prereqs.forEach { (deckId, allowedWords) ->
            deck(deckId).items.forEach { item ->
                item.front.split(" ").forEach { word ->
                    assertTrue(
                        "phrase word '$word' in $deckId not in prerequisite decks",
                        word.lowercase() in allowedWords,
                    )
                }
            }
        }
    }

    @Test fun mathFactsAreCorrectAndInRange() {
        deck("addition").items.forEach {
            val (a, b) = it.id.removePrefix("add_").split("_").map(String::toInt)
            assertTrue(a + b <= 10)
            assertEquals((a + b).toString(), it.back)
        }
        deck("subtraction").items.forEach {
            val (a, b) = it.id.removePrefix("sub_").split("_").map(String::toInt)
            assertTrue(a <= 10 && a - b >= 0)
            assertEquals((a - b).toString(), it.back)
        }
        deck("multiplication").items.forEach {
            val (a, b) = it.id.removePrefix("mul_").split("_").map(String::toInt)
            assertEquals((a * b).toString(), it.back)
        }
        deck("division").items.forEach {
            val (a, b) = it.id.removePrefix("div_").split("_").map(String::toInt)
            assertEquals(0, a % b)
            assertEquals((a / b).toString(), it.back)
        }
    }

    @Test fun unlockChainMatchesSpec() {
        fun rule(id: String) = deck(id).unlockRule
        assertEquals(UnlockRule.None, rule("letters_1"))
        assertEquals(UnlockRule.DecksMastered(listOf("letters_1")), rule("letters_2"))
        assertEquals(UnlockRule.DecksMastered(listOf("letters_1")), rule("cvc_1"))
        assertEquals(UnlockRule.DecksMastered(listOf("cvc_2")), rule("sight_1"))
        assertEquals(UnlockRule.DecksMastered(listOf("cvc_1", "sight_1")), rule("phrases_1"))
        assertEquals(UnlockRule.None, rule("numbers"))
        assertEquals(UnlockRule.DecksMastered(listOf("numbers")), rule("counting"))
        assertEquals(UnlockRule.DecksMastered(listOf("addition", "subtraction")), rule("multiplication"))
        assertEquals(UnlockRule.DecksMastered(listOf("multiplication")), rule("division"))
    }
}
