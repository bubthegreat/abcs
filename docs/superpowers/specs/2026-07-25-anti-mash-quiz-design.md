# Preventing button-mash winning in the quiz

**Date:** 2026-07-25
**Status:** Approved

## Problem

Kids discovered they can tap the same screen position repeatedly and still make
progress. The three answer buttons sit in a fixed row, so mashing one spot is a
free 1-in-3 chance on every question, with no reading required.

Measuring where the exploit actually pays out:

- **Generated (math) decks are already immune.** A wrong answer resets the
  streak to 0 and mastery needs 10 in a row, so a 1-in-3 guesser faces
  `(1/3)^10` ≈ 1 in 59,000.
- **Static decks are only a speed bump.** Mastery needs `correctCount >= 3`;
  correct is `+1` and wrong is `-1` floored at 0. Guessing drifts negative
  (−1/3 per question), but the floor at zero is a reflecting barrier — the walk
  can never go below 0, so it keeps re-attempting the climb and reaches 3
  eventually. Slow, not impossible.
- **The star drip is the real slot machine.** `recordKidCorrect` fires on every
  correct answer on an unmastered card, with no streak requirement and mastery
  irrelevant. At 10 corrects per star, a masher banks a star roughly every 30
  taps, indefinitely.

The star drip is almost certainly what the kids actually found.

## Goals

1. Guessing earns nothing — neither mastery nor stars accrue from lucky taps.
2. A genuinely known answer is **never** discounted, however fast it arrives.

Goal 2 rules out every speed-based heuristic. A kid who instantly taps "red"
because they know it must always get credit. The design therefore never
inspects *how* an answer was given — only whether it was right. Correctness
stays the only currency; guessing is defeated by arithmetic, not detection.

Explicit non-goals: no mash detection, no timing gates, no accusatory UI, no
parent reporting.

## Rules

### Rule 1 — a wrong answer resets that card's quiz streak

Mastery becomes "3 right in a row" rather than "3 more rights than wrongs".
This is what generated decks already do, extended to static decks.

**Mastered cards are protected.** Once a card is at or above threshold, a later
wrong answer steps it back by one (to `threshold - 1`) instead of to 0. Cards
do not decay, so a mastered card can still resurface; without this, one unlucky
tap could knock a finished card to 0 and un-complete a nearly-finished deck,
since deck completion requires every card mastered simultaneously.

### Rule 2 — a wrong answer decrements star progress by 1 (floored at 0)

Rule 1 alone does **not** close the star hole: `starProgress` counts raw
corrects in a separate counter, so resetting a card's mastery streak leaves the
drip untouched and a masher keeps banking stars.

This rule is deliberately gentler than Rule 1:

| | per-question drift | effect |
|---|---|---|
| Kid at ~90% | +0.8 | ~12 questions per star vs 10 today — barely notices |
| Masher at 1-in-4 | −0.5 | never accumulates; reaching 10 is astronomically unlikely |

A hard reset here would mean *10 in a row* per star, slowing a genuine learner
roughly 3×. That would discount real answers, violating Goal 2. Mastery can
afford the strict rule (3 in a row is a fair bar for "knows this card"); a
global effort counter should not be wiped by a single slip.

**Banked stars are never removed.** Only in-progress drip is affected. Taking
back a star a kid already earned would be a different, worse app.

### Rule 3 — four answer choices where the deck can supply them

Drops a guess from 1-in-3 to 1-in-4. Tiny decks fall back to three (or fewer)
choices automatically. This is a bonus multiplier, not load-bearing — Rules 1
and 2 do the work.

### Combined effect

| | today | after |
|---|---|---|
| Masher masters a card | reachable by persistence | ~1 in 64 per attempt |
| Masher banks a star | ~every 30 taps | effectively never |
| Kid who knows the material | full speed | full speed |

## Implementation

### `engine/Mastery.kt`

```kotlin
/** Quiz wrong: break the streak, but a mastered card only steps back one. */
fun applyQuizWrong(p: ItemProgress, today: Long, threshold: Int): ItemProgress =
    p.copy(
        correctCount = if (p.correctCount >= threshold) threshold - 1 else 0,
        lastSeenEpochDay = today,
    )
```

`applyWrong` is left untouched — it still serves flashcard practice, which is
not a scored mode.

### `ui/AppViewModel.kt`

`quizWrongAdvance` uses `applyQuizWrong` with the deck's threshold, which lets
the generated-deck special case be deleted: below threshold the new rule
resets to 0, identical to today's math behaviour; at or above threshold it
steps back one, which for a generated deck only applies once the deck is
already complete. Nothing regresses.

It also calls `recordKidWrong()` under the **same** condition that gates the
credit — `earnsStars(deckId) && !reviewMode`. This symmetry matters: if the
daily limit is spent, or the deck is not homework, or it is review mode, a
wrong answer must not burn progress that a correct answer could not have
earned.

### `data/ProgressStore.kt`

Add `recordKidWrong()`, mirroring `recordKidCorrect`, decrementing
`starProgress` with a floor at 0 and never touching the bank.

### `engine/Quiz.kt`

`.take(2)` → `.take(3)`, and widen the small-deck pool test from `>= 2` to
`>= 3`. `take` degrades naturally, so the fallback needs no extra branch.

### `engine/MathGen.kt`

Matching fourth choice, so both quiz types behave consistently.

### `ui/App.kt`

The answer `Row` renders `q.choices`, so four buttons need no structural
change. Verify the row still reads well at four across on a tablet.

## Testing

Unit tests on `applyQuizWrong`:

- unmastered card → 0
- mastered card → `threshold - 1`
- already at 0 stays at 0

Seeded simulations, which are what actually prove the feature:

- A 1-in-4 guesser over a few thousand answers masters nothing and banks no
  stars.
- A ~90% learner still masters decks and banks stars at close to today's rate.
  This is the regression guard on Goal 2 and is the most important test here.

Existing assertions of `choices.size == 3` in `QuizTest` and `MathGenTest`
update to 4, keeping the tiny-deck case asserting graceful fallback.

## Risks

- A kid who slips on the last unmastered card of a deck rebuilds a 3-streak.
  Accepted: mastered cards are protected, so this only affects cards genuinely
  not yet learned.
- Star cadence slows slightly (~12 questions per star instead of 10) for a
  90%-accurate kid. Accepted as within noise; the simulation test pins it.
