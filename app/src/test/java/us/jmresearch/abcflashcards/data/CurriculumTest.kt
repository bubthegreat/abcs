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

    @Test fun everyDeckHasAnAgeUnlockAndNoStaleIds() {
        decks.forEach { deck ->
            assertTrue("deck ${deck.id} missing from ageUnlocks", Curriculum.ageUnlocks.containsKey(deck.id))
        }
        Curriculum.ageUnlocks.keys.forEach { id ->
            assertTrue("ageUnlocks has stale id $id", decks.any { it.id == id })
        }
    }

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
            "times_tables", "fractions", "algebra",
            "colors", "shapes", "shapes_2", "colors_shapes",
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

    @Test fun arithmeticDecksAreGeneratedWithStreakIds() {
        listOf("addition", "subtraction", "multiplication", "division", "times_tables").forEach { id ->
            val d = deck(id)
            assertTrue("$id must be generated", d.generator != null)
            assertEquals(1, d.items.size)
            assertEquals(generatedItemId(id), d.items.first().id)
        }
        // static math decks stay static
        assertTrue(deck("fractions").generator == null)
        assertTrue(deck("algebra").generator == null)
    }

    @Test fun mathFactsAreCorrectAndInRange() {
        deck("fractions").items.forEach {
            val (n, d, w) = it.id.removePrefix("frac_").split("_").map(String::toInt)
            assertEquals(0, (w * n) % d)
            assertEquals((w * n / d).toString(), it.back)
        }
        deck("algebra").items.forEach {
            val (a, b, x) = it.id.removePrefix("alg_").split("_").map(String::toInt)
            assertEquals("x = $x", it.back)
            assertTrue("front must show the equation", it.front.endsWith("= ${a * x + b}"))
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
        assertEquals(UnlockRule.DecksMastered(listOf("division")), rule("fractions"))
        assertEquals(UnlockRule.DecksMastered(listOf("fractions")), rule("algebra"))
        assertEquals(UnlockRule.None, rule("colors"))
        assertEquals(UnlockRule.None, rule("shapes"))
        assertEquals(UnlockRule.DecksMastered(listOf("shapes")), rule("shapes_2"))
        assertEquals(UnlockRule.DecksMastered(listOf("colors", "shapes")), rule("colors_shapes"))
    }

    @Test fun colorAndShapeCardsAreDrawableSpecs() {
        // Drawn by ShapeGlyph from the id; front mirrors the id.
        (deck("colors").items + deck("shapes").items + deck("shapes_2").items + deck("colors_shapes").items)
            .forEach { item ->
                assertEquals(item.id, item.front)
                assertTrue(
                    "id must carry a drawable prefix: ${item.id}",
                    item.id.startsWith("color_") || item.id.startsWith("shape_") || item.id.startsWith("combo_"),
                )
            }
        deck("colors_shapes").items.forEach { item ->
            assertEquals(2, item.id.removePrefix("combo_").split("_").size) // color_shape
        }
    }
}
