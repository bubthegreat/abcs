package us.jmresearch.abcflashcards.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageCurriculumTest {

    private val decks = Curriculum.decks
    private fun deck(id: String) = decks.first { it.id == id }

    @Test fun languageDecksExistWithGradeChain() {
        listOf("vocab_prek", "vocab_k", "vocab_1", "vocab_2", "grammar_1", "grammar_2", "grammar_3")
            .forEach { id ->
                assertTrue("missing $id", decks.any { it.id == id })
                assertEquals(Subject.LANGUAGE, deck(id).subject)
                assertTrue("$id needs cards", deck(id).items.size >= 8)
            }
        assertEquals(UnlockRule.None, deck("vocab_prek").unlockRule)
        assertEquals(UnlockRule.DecksMastered(listOf("vocab_prek")), deck("vocab_k").unlockRule)
        assertEquals(UnlockRule.DecksMastered(listOf("vocab_k")), deck("vocab_1").unlockRule)
        assertEquals(UnlockRule.DecksMastered(listOf("vocab_1")), deck("vocab_2").unlockRule)
        assertEquals(UnlockRule.None, deck("grammar_1").unlockRule)
        assertEquals(UnlockRule.DecksMastered(listOf("grammar_1")), deck("grammar_2").unlockRule)
        assertEquals(UnlockRule.DecksMastered(listOf("grammar_2")), deck("grammar_3").unlockRule)
    }

    @Test fun everyLanguageCardHasAnAnswerOnTheBack() {
        listOf("vocab_prek", "vocab_k", "vocab_1", "vocab_2", "grammar_1", "grammar_2", "grammar_3")
            .flatMap { deck(it).items }
            .forEach { item ->
                assertTrue("${item.id} needs a back", !item.back.isNullOrBlank())
                assertTrue("${item.id} needs a front", item.front.isNotBlank())
            }
    }

    @Test fun pickTheWordGrammarAnswersAppearInTheirSentence() {
        (deck("grammar_1").items + deck("grammar_3").items)
            .filter { it.front.startsWith("Pick the") }
            .forEach { item ->
                val sentence = item.front.substringAfter(":").lowercase()
                assertTrue(
                    "${item.id}: answer '${item.back}' must appear in \"${item.front}\"",
                    sentence.contains(item.back!!.lowercase()),
                )
            }
    }

    @Test fun languageCardsSpeakTheirFront() {
        val d = deck("vocab_prek")
        assertEquals(d.items.first().front, utteranceFor(d.items.first(), d))
    }
}
