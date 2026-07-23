package us.jmresearch.abcflashcards.data

object Curriculum {

    private fun letterDeck(id: String, title: String, letters: String, rule: UnlockRule) = Deck(
        id = id,
        title = title,
        subject = Subject.LETTERS,
        items = letters.map { c ->
            val display = if (c == 'q') "Qu qu" else "${c.uppercaseChar()} $c"
            CardItem(id = "letter_$c", front = display)
        },
        unlockRule = rule,
    )

    private fun wordDeck(id: String, title: String, words: List<String>, rule: UnlockRule, prefix: String = "word") = Deck(
        id = id,
        title = title,
        subject = if (prefix == "phrase") Subject.PHRASES else Subject.WORDS,
        items = words.map { w -> CardItem(id = "${prefix}_${w.replace(" ", "_")}", front = w) },
        unlockRule = rule,
    )

    private fun dots(n: Int): String {
        // dice-style rows of up to 5
        val full = "●".repeat(5)
        return when {
            n <= 5 -> "●".repeat(n)
            else -> full + "\n" + "●".repeat(n - 5)
        }
    }

    private val colorEmojis = listOf(
        "red" to "🔴", "blue" to "🔵", "green" to "🟢", "yellow" to "🟡",
        "orange" to "🟠", "purple" to "🟣", "pink" to "🌸", "brown" to "🟤",
        "black" to "⚫", "white" to "⚪",
    )

    private val shapeEmojis = listOf(
        "circle" to "⭕", "square" to "⬛", "rectangle" to "▬", "triangle" to "🔺",
        "star" to "⭐", "heart" to "❤️", "diamond" to "🔷",
    )

    private val advancedShapeEmojis = listOf(
        "pentagon" to "⬟", "hexagon" to "⬢", "octagon" to "🛑",
        "oval" to "⬭", "cross" to "➕",
    )

    // Only color+shape pairs that have a real emoji.
    private val comboEmojis = listOf(
        Triple("red", "circle", "🔴"), Triple("blue", "circle", "🔵"), Triple("green", "circle", "🟢"),
        Triple("yellow", "circle", "🟡"), Triple("orange", "circle", "🟠"), Triple("purple", "circle", "🟣"),
        Triple("red", "square", "🟥"), Triple("blue", "square", "🟦"), Triple("green", "square", "🟩"),
        Triple("yellow", "square", "🟨"), Triple("orange", "square", "🟧"), Triple("purple", "square", "🟪"),
        Triple("red", "heart", "❤️"), Triple("blue", "heart", "💙"), Triple("green", "heart", "💚"),
        Triple("yellow", "heart", "💛"), Triple("orange", "heart", "🧡"), Triple("purple", "heart", "💜"),
    )

    val decks: List<Deck> = buildList {
        // Colors & shapes — for the youngest learners; no reading required
        add(Deck(
            id = "colors", title = "Colors", subject = Subject.COLORS,
            items = colorEmojis.map { (name, emoji) -> CardItem("color_$name", emoji) },
            unlockRule = UnlockRule.None,
        ))
        add(Deck(
            id = "shapes", title = "Shapes", subject = Subject.COLORS,
            items = shapeEmojis.map { (name, emoji) -> CardItem("shape_$name", emoji) },
            unlockRule = UnlockRule.None,
        ))
        add(Deck(
            id = "shapes_2", title = "More Shapes", subject = Subject.COLORS,
            items = advancedShapeEmojis.map { (name, emoji) -> CardItem("shape_$name", emoji) },
            unlockRule = UnlockRule.DecksMastered(listOf("shapes")),
        ))
        add(Deck(
            id = "colors_shapes", title = "Colors + Shapes", subject = Subject.COLORS,
            items = comboEmojis.map { (color, shape, emoji) -> CardItem("combo_${color}_$shape", emoji) },
            unlockRule = UnlockRule.DecksMastered(listOf("colors", "shapes")),
        ))

        // Letters — Jolly Phonics groups
        add(letterDeck("letters_1", "Letters 1", "satpin", UnlockRule.None))
        add(letterDeck("letters_2", "Letters 2", "ckehrmd", UnlockRule.DecksMastered(listOf("letters_1"))))
        add(letterDeck("letters_3", "Letters 3", "goulfb", UnlockRule.DecksMastered(listOf("letters_2"))))
        add(letterDeck("letters_4", "Letters 4", "jzwvyx", UnlockRule.DecksMastered(listOf("letters_3"))))
        add(letterDeck("letters_5", "Letters 5", "q", UnlockRule.DecksMastered(listOf("letters_4"))))
        add(
            Deck(
                id = "letters_review",
                title = "All Letters",
                subject = Subject.LETTERS,
                items = ('a'..'z').map { c ->
                    CardItem(id = "letterreview_$c", front = "${c.uppercaseChar()} $c")
                },
                unlockRule = UnlockRule.DecksMastered(
                    listOf("letters_1", "letters_2", "letters_3", "letters_4", "letters_5")
                ),
            )
        )

        // Words
        add(wordDeck(
            "cvc_1", "Words 1",
            listOf("sat", "pat", "tap", "sap", "pin", "tin", "nap", "pan", "tan", "sit", "pit", "nip", "in"),
            UnlockRule.DecksMastered(listOf("letters_1")),
        ))
        add(wordDeck(
            "cvc_2", "Words 2",
            listOf("cat", "hat", "mat", "rat", "red", "hen", "ten", "met", "pet", "dim", "rim", "ram", "mad", "sad", "dad", "ham"),
            UnlockRule.DecksMastered(listOf("cvc_1", "letters_2")),
        ))
        add(wordDeck(
            "cvc_3", "Words 3",
            listOf("dog", "log", "bug", "fun", "run", "sun", "bed", "big", "dig", "leg", "gum", "cup", "cut", "lot", "fog", "bat"),
            UnlockRule.DecksMastered(listOf("cvc_2", "letters_3")),
        ))
        add(wordDeck(
            "sight_1", "Sight Words",
            listOf("the", "a", "I", "to", "is", "it"),
            UnlockRule.DecksMastered(listOf("cvc_2")),
            prefix = "sight",
        ))

        // Phrases
        add(wordDeck(
            "phrases_1", "Phrases 1",
            listOf("I sat", "tap it", "a pin", "sit in it", "it is a nap"),
            UnlockRule.DecksMastered(listOf("cvc_1", "sight_1")),
            prefix = "phrase",
        ))
        add(wordDeck(
            "phrases_2", "Phrases 2",
            listOf("the red hen", "a red hat", "a sad dad", "the cat is mad", "a pet ram"),
            UnlockRule.DecksMastered(listOf("cvc_2", "sight_1")),
            prefix = "phrase",
        ))

        // Math ladder
        add(Deck(
            id = "numbers", title = "Numbers", subject = Subject.MATH,
            items = (0..10).map { n -> CardItem("num_$n", "$n") },
            unlockRule = UnlockRule.None,
        ))
        add(Deck(
            id = "counting", title = "Counting", subject = Subject.MATH,
            items = (1..10).map { n -> CardItem("count_$n", dots(n), back = "$n") },
            unlockRule = UnlockRule.DecksMastered(listOf("numbers")),
        ))
        add(Deck(
            id = "addition", title = "Adding", subject = Subject.MATH,
            items = buildList {
                for (a in 1..9) for (b in 1..9) if (a + b <= 10) add(CardItem("add_${a}_$b", "$a + $b", "${a + b}"))
            },
            unlockRule = UnlockRule.DecksMastered(listOf("counting")),
        ))
        add(Deck(
            id = "subtraction", title = "Taking Away", subject = Subject.MATH,
            items = buildList {
                for (a in 2..10) for (b in 1 until a) add(CardItem("sub_${a}_$b", "$a − $b", "${a - b}"))
            },
            unlockRule = UnlockRule.DecksMastered(listOf("counting")),
        ))
        add(Deck(
            id = "multiplication", title = "Times", subject = Subject.MATH,
            items = buildList {
                for (a in 1..5) for (b in 1..5) add(CardItem("mul_${a}_$b", "$a × $b", "${a * b}"))
            },
            unlockRule = UnlockRule.DecksMastered(listOf("addition", "subtraction")),
        ))
        add(Deck(
            id = "division", title = "Sharing", subject = Subject.MATH,
            items = buildList {
                for (b in 1..5) for (q in 1..5) add(CardItem("div_${b * q}_$b", "${b * q} ÷ $b", "$q"))
            }.distinctBy { it.id },
            unlockRule = UnlockRule.DecksMastered(listOf("multiplication")),
        ))
    }
}
