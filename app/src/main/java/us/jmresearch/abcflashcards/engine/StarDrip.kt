package us.jmresearch.abcflashcards.engine

/**
 * The running tally toward the next star: [progress] corrects banked so far, and
 * the [bank] of stars already earned.
 *
 * Kept as pure functions so the drip rules have one definition that both the
 * store and the tests use — the anti-guessing behaviour is arithmetic, and
 * arithmetic that only exists inside a DataStore edit block cannot be tested.
 */
data class StarDrip(val progress: Int, val bank: Int)

/** One correct answer. Every [correctsPerStar] corrects banks a star. */
fun dripCorrect(drip: StarDrip, correctsPerStar: Int): StarDrip =
    if (drip.progress + 1 >= correctsPerStar) {
        StarDrip(progress = 0, bank = drip.bank + 1)
    } else {
        drip.copy(progress = drip.progress + 1)
    }

/**
 * One wrong answer. Nudges progress back so that guessing cannot accumulate: a
 * kid answering well drifts upward and barely notices the step back, while a
 * random tapper drifts down and never reaches the next star.
 *
 * A star already banked is never taken away — only in-progress drip moves.
 */
fun dripWrong(drip: StarDrip): StarDrip =
    drip.copy(progress = (drip.progress - 1).coerceAtLeast(0))
