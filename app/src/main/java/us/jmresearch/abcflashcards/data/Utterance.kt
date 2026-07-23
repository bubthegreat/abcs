package us.jmresearch.abcflashcards.data

/**
 * Key used to store/look up a parent recording for a card. Review-deck letters
 * share the recording of their main letter card.
 */
fun recordingKeyFor(itemId: String): String = itemId.replace("letterreview_", "letter_")

/**
 * What the parent should say when recording each letter's sound —
 * phonics sound first, then an anchor word (Jolly Phonics style).
 */
val letterRecordingPrompts: Map<Char, String> = mapOf(
    'a' to "\"a\" as in apple",
    'b' to "\"b\" as in ball",
    'c' to "\"k\" as in cat",
    'd' to "\"d\" as in dog",
    'e' to "\"e\" as in egg",
    'f' to "\"fff\" as in fish",
    'g' to "\"g\" as in goat",
    'h' to "\"h\" as in hat",
    'i' to "\"i\" as in igloo",
    'j' to "\"j\" as in jam",
    'k' to "\"k\" as in kite",
    'l' to "\"lll\" as in leg",
    'm' to "\"mmm\" as in moon",
    'n' to "\"nnn\" as in net",
    'o' to "\"o\" as in octopus",
    'p' to "\"p\" as in pig",
    'q' to "\"qu\" as in queen",
    'r' to "\"rrr\" as in rabbit",
    's' to "\"sss\" as in snake",
    't' to "\"t\" as in tap",
    'u' to "\"u\" as in umbrella",
    'v' to "\"vvv\" as in van",
    'w' to "\"w\" as in wind",
    'x' to "\"ks\" as in fox",
    'y' to "\"y\" as in yes",
    'z' to "\"zzz\" as in zebra",
)

/** What TTS should say for a card, when no parent recording exists. */
fun utteranceFor(item: CardItem, deck: Deck): String = when (deck.subject) {
    Subject.COLORS -> item.id.substringAfter("_").replace("_", " ")
    Subject.LETTERS -> item.id.substringAfterLast("_").uppercase()
    Subject.WORDS, Subject.PHRASES -> item.front
    Subject.MATH -> when (deck.id) {
        "counting" -> "Count the dots!"
        else -> item.front
            .replace("+", "plus")
            .replace("−", "minus")
            .replace("×", "times")
            .replace("÷", "divided by")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
