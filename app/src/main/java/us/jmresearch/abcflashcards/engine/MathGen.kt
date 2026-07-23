package us.jmresearch.abcflashcards.engine

import kotlin.random.Random
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.MathOp
import us.jmresearch.abcflashcards.data.generatedItemId

/** A fresh random problem. All cards in a generated deck share one streak id. */
fun genMathCard(op: MathOp, deckId: String, random: Random): CardItem {
    val id = generatedItemId(deckId)
    return when (op) {
        MathOp.ADD -> {
            val a = random.nextInt(0, 11)
            val b = random.nextInt(0, (20 - a).coerceAtMost(10) + 1)
            CardItem(id, "$a + $b", "${a + b}")
        }
        MathOp.SUB -> {
            val a = random.nextInt(0, 21)
            val b = random.nextInt(0, a + 1)
            CardItem(id, "$a − $b", "${a - b}")
        }
        MathOp.MUL -> {
            val a = random.nextInt(0, 11)
            val b = random.nextInt(0, 11)
            CardItem(id, "$a × $b", "${a * b}")
        }
        MathOp.DIV -> {
            val b = random.nextInt(1, 11)
            val q = random.nextInt(0, 11)
            CardItem(id, "${b * q} ÷ $b", "$q")
        }
        MathOp.TABLES -> {
            val a = random.nextInt(0, 21)
            val b = random.nextInt(0, 21)
            CardItem(id, "$a × $b", "${a * b}")
        }
    }
}

/** Quiz for a generated card: numeric near-miss distractors around the answer. */
fun buildGeneratedQuiz(card: CardItem, random: Random): Quiz {
    val answer = card.back!!.toInt()
    val distractors = mutableSetOf<Int>()
    while (distractors.size < 2) {
        val offset = random.nextInt(1, 6) * (if (random.nextBoolean()) 1 else -1)
        val candidate = answer + offset
        if (candidate >= 0 && candidate != answer) distractors.add(candidate)
    }
    return Quiz(
        item = card,
        visualPrompt = card.front,
        spokenPrompt = card.front
            .replace("+", "plus")
            .replace("−", "minus")
            .replace("×", "times")
            .replace("÷", "divided by"),
        choices = (distractors.map { it.toString() } + card.back!!).shuffled(random),
        answer = card.back!!,
    )
}
