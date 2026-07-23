package us.jmresearch.abcflashcards.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UtteranceTest {

    private fun deck(id: String) = Curriculum.decks.first { it.id == id }
    private fun item(deckId: String, itemId: String) =
        deck(deckId).items.first { it.id == itemId }

    @Test fun lettersSpeakTheLetterName() {
        assertEquals("S", utteranceFor(item("letters_1", "letter_s"), deck("letters_1")))
    }

    @Test fun wordsAndPhrasesSpeakTheirText() {
        assertEquals("sat", utteranceFor(item("cvc_1", "word_sat"), deck("cvc_1")))
        assertEquals("tap it", utteranceFor(item("phrases_1", "phrase_tap_it"), deck("phrases_1")))
    }

    @Test fun mathSpeaksReadableOperators() {
        val d = deck("addition")
        assertEquals("3 plus 2", utteranceFor(CardItem("x", "3 + 2", "5"), d))
        assertEquals("5 minus 2", utteranceFor(CardItem("x", "5 − 2", "3"), d))
        assertEquals("2 times 3", utteranceFor(CardItem("x", "2 × 3", "6"), d))
        assertEquals("6 divided by 2", utteranceFor(CardItem("x", "6 ÷ 2", "3"), d))
    }

    @Test fun countingPromptsToCount() {
        assertEquals("Count the dots!", utteranceFor(item("counting", "count_4"), deck("counting")))
    }

    @Test fun numbersSpeakTheNumeral() {
        assertEquals("7", utteranceFor(item("numbers", "num_7"), deck("numbers")))
    }

    @Test fun colorsShapesAndCombosSpeakTheirNames() {
        assertEquals("red", utteranceFor(item("colors", "color_red"), deck("colors")))
        assertEquals("circle", utteranceFor(item("shapes", "shape_circle"), deck("shapes")))
        assertEquals("red square", utteranceFor(item("colors_shapes", "combo_red_square"), deck("colors_shapes")))
    }

    @Test fun reviewLettersShareMainLetterRecording() {
        assertEquals("letter_s", recordingKeyFor("letterreview_s"))
        assertEquals("letter_s", recordingKeyFor("letter_s"))
        assertEquals("word_sat", recordingKeyFor("word_sat"))
    }
}
