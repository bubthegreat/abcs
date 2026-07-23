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

    // Shape/color cards are DRAWN by the UI from the item id (see ShapeGlyph);
    // front mirrors the id so non-drawing surfaces still have something sane.
    private val colorNames = listOf(
        "red", "blue", "green", "yellow", "orange", "purple", "pink", "brown", "black", "white",
    )

    private val shapeNames = listOf(
        "circle", "square", "rectangle", "triangle", "star", "heart", "diamond",
    )

    private val advancedShapeNames = listOf(
        "pentagon", "hexagon", "octagon", "oval", "cross",
    )

    private val comboColorNames = listOf("red", "blue", "green", "yellow", "purple", "pink")
    private val comboShapeNames = listOf("circle", "square", "triangle", "star")

    val decks: List<Deck> = buildList {
        // Colors & shapes — for the youngest learners; no reading required
        add(Deck(
            id = "colors", title = "Colors", subject = Subject.COLORS,
            items = colorNames.map { name -> CardItem("color_$name", "color_$name") },
            unlockRule = UnlockRule.None,
        ))
        add(Deck(
            id = "shapes", title = "Shapes", subject = Subject.COLORS,
            items = shapeNames.map { name -> CardItem("shape_$name", "shape_$name") },
            unlockRule = UnlockRule.None,
        ))
        add(Deck(
            id = "shapes_2", title = "More Shapes", subject = Subject.COLORS,
            items = advancedShapeNames.map { name -> CardItem("shape_$name", "shape_$name") },
            unlockRule = UnlockRule.DecksMastered(listOf("shapes")),
        ))
        add(Deck(
            id = "colors_shapes", title = "Colors + Shapes", subject = Subject.COLORS,
            items = comboColorNames.flatMap { color ->
                comboShapeNames.map { shape ->
                    CardItem("combo_${color}_$shape", "combo_${color}_$shape")
                }
            },
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

        // Language — vocabulary by grade, grammar through 2nd grade
        fun vocabDeck(id: String, title: String, pairs: List<Pair<String, String>>, rule: UnlockRule) = Deck(
            id = id, title = title, subject = Subject.LANGUAGE,
            items = pairs.map { (word, meaning) -> CardItem("${id}_$word", word, meaning) },
            unlockRule = rule,
        )
        add(vocabDeck(
            "vocab_prek", "Words Pre-K",
            listOf(
                "big" to "very large", "small" to "very little", "happy" to "feeling good",
                "sad" to "feeling bad", "hot" to "very warm", "cold" to "not warm at all",
                "fast" to "moves quickly", "slow" to "takes a long time",
                "loud" to "makes lots of noise", "quiet" to "makes little noise",
            ),
            UnlockRule.None,
        ))
        add(vocabDeck(
            "vocab_k", "Words K",
            listOf(
                "same" to "just like another", "different" to "not like another",
                "first" to "before all others", "last" to "after all others",
                "above" to "higher up", "below" to "lower down",
                "empty" to "nothing inside", "full" to "no room left",
                "open" to "not shut", "closed" to "shut tight",
            ),
            UnlockRule.DecksMastered(listOf("vocab_prek")),
        ))
        add(vocabDeck(
            "vocab_1", "Words 1st Grade",
            listOf(
                "always" to "every single time", "never" to "not one time",
                "early" to "before the usual time", "late" to "after the usual time",
                "easy" to "not hard to do", "difficult" to "hard to do",
                "begin" to "to start", "finish" to "to end",
                "near" to "close by", "far" to "a long way away",
            ),
            UnlockRule.DecksMastered(listOf("vocab_k")),
        ))
        add(vocabDeck(
            "vocab_2", "Words 2nd Grade",
            listOf(
                "brave" to "not afraid", "curious" to "wants to know more",
                "enormous" to "very very big", "tiny" to "very very small",
                "exhausted" to "very tired", "delicious" to "tastes very good",
                "fragile" to "breaks easily", "ancient" to "very very old",
                "rapid" to "very fast", "silent" to "no sound at all",
                "furious" to "very angry", "gentle" to "soft and careful",
            ),
            UnlockRule.DecksMastered(listOf("vocab_1")),
        ))
        add(Deck(
            id = "grammar_1", title = "Nouns & Verbs", subject = Subject.LANGUAGE,
            items = listOf(
                CardItem("grammar_1_dog", "Pick the noun: The dog ran home.", "dog"),
                CardItem("grammar_1_ball", "Pick the noun: Sam threw the ball.", "ball"),
                CardItem("grammar_1_cake", "Pick the noun: We ate the cake.", "cake"),
                CardItem("grammar_1_bird", "Pick the noun: A bird sang loudly.", "bird"),
                CardItem("grammar_1_truck", "Pick the noun: The truck is red.", "truck"),
                CardItem("grammar_1_sleeps", "Pick the verb: The cat sleeps all day.", "sleeps"),
                CardItem("grammar_1_jumped", "Pick the verb: The frog jumped high.", "jumped"),
                CardItem("grammar_1_eats", "Pick the verb: My sister eats apples.", "eats"),
                CardItem("grammar_1_runs", "Pick the verb: Dad runs every morning.", "runs"),
                CardItem("grammar_1_swims", "Pick the verb: The fish swims fast.", "swims"),
            ),
            unlockRule = UnlockRule.None,
        ))
        add(Deck(
            id = "grammar_2", title = "Plurals & Pronouns", subject = Subject.LANGUAGE,
            items = listOf(
                CardItem("grammar_2_cats", "One cat, two ___", "cats"),
                CardItem("grammar_2_dogs", "One dog, two ___", "dogs"),
                CardItem("grammar_2_boxes", "One box, two ___", "boxes"),
                CardItem("grammar_2_buses", "One bus, two ___", "buses"),
                CardItem("grammar_2_babies", "One baby, two ___", "babies"),
                CardItem("grammar_2_feet", "One foot, two ___", "feet"),
                CardItem("grammar_2_he", "Sam is a boy. ___ likes trucks.", "He"),
                CardItem("grammar_2_she", "Mia is a girl. ___ likes bugs.", "She"),
                CardItem("grammar_2_they", "Sam and Mia play. ___ have fun.", "They"),
                CardItem("grammar_2_we", "You and I read. ___ love books.", "We"),
            ),
            unlockRule = UnlockRule.DecksMastered(listOf("grammar_1")),
        ))
        add(Deck(
            id = "grammar_3", title = "Past & Describing", subject = Subject.LANGUAGE,
            items = listOf(
                CardItem("grammar_3_jumped", "Yesterday I ___ (jump)", "jumped"),
                CardItem("grammar_3_walked", "Yesterday we ___ (walk)", "walked"),
                CardItem("grammar_3_played", "Yesterday she ___ (play)", "played"),
                CardItem("grammar_3_ran", "Yesterday he ___ (run)", "ran"),
                CardItem("grammar_3_ate", "Yesterday they ___ (eat)", "ate"),
                CardItem("grammar_3_went", "Yesterday I ___ (go)", "went"),
                CardItem("grammar_3_red", "Pick the describing word: The red ball bounced.", "red"),
                CardItem("grammar_3_soft", "Pick the describing word: A soft kitten purred.", "soft"),
                CardItem("grammar_3_huge", "Pick the describing word: We saw a huge truck.", "huge"),
                CardItem("grammar_3_shiny", "Pick the describing word: Her shiny shoes squeak.", "shiny"),
            ),
            unlockRule = UnlockRule.DecksMastered(listOf("grammar_2")),
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
            id = "division", title = "Division", subject = Subject.MATH,
            items = buildList {
                for (b in 1..5) for (q in 1..5) add(CardItem("div_${b * q}_$b", "${b * q} ÷ $b", "$q"))
            }.distinctBy { it.id },
            unlockRule = UnlockRule.DecksMastered(listOf("multiplication")),
        ))
        add(Deck(
            id = "fractions", title = "Fractions", subject = Subject.MATH,
            items = buildList {
                for (w in listOf(2, 4, 6, 8, 10)) add(CardItem("frac_1_2_$w", "1/2 of $w", "${w / 2}"))
                for (w in listOf(3, 6, 9, 12)) add(CardItem("frac_1_3_$w", "1/3 of $w", "${w / 3}"))
                for (w in listOf(4, 8, 12)) add(CardItem("frac_1_4_$w", "1/4 of $w", "${w / 4}"))
                for (w in listOf(4, 8, 12)) add(CardItem("frac_3_4_$w", "3/4 of $w", "${w * 3 / 4}"))
            },
            unlockRule = UnlockRule.DecksMastered(listOf("division")),
        ))
        add(Deck(
            id = "algebra", title = "Algebra", subject = Subject.MATH,
            items = buildList {
                for (a in 1..3) for (x in 1..5) {
                    val b = ((a + x) % 4) + 1
                    val c = a * x + b
                    val left = if (a == 1) "x + $b" else "${a}x + $b"
                    add(CardItem("alg_${a}_${b}_$x", "$left = $c", "x = $x"))
                }
            },
            unlockRule = UnlockRule.DecksMastered(listOf("fractions")),
        ))
    }
}
