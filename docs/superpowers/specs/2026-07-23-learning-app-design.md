# ABC Flash Cards → Kid Learning App — Design Spec

**Date:** 2026-07-23
**Status:** Approved by user
**Target user:** 4-year-old, parent-guided sessions (solo-kid mode is a future phase; this design lays its foundations)

## 1. Goal

Evolve the current single-screen random-letter app into a structured, evidence-based learning app covering letters, words, phrases, and math, with per-item mastery tracking, deck progression/unlocking, and persistent progress.

## 2. Decisions Made

| Topic | Decision |
|---|---|
| Interaction model | Parent-guided (parent judges ✓/✗). Data model designed so solo-kid quiz mode can be added later without engine rewrite. |
| Mastery model | Confidence counter: item mastered at `correctCount >= threshold` (default 3). ✗ decrements (floor 0). Mastered items still resurface occasionally (spaced review). |
| Letter order | Jolly Phonics / SATPIN-style groups, not A→Z. |
| Words | Decodable CVC words gated on mastered letter groups, plus a small Dolch pre-K sight-word deck. |
| Phrases | Included in first build. Decodable-only phrases gated on mastered word sets. |
| Math | Full ladder: Numbers → Counting (subitizing) → Addition → Subtraction → 🔒Multiplication → 🔒Division. Mult/div built but locked behind prerequisites. |
| UI tech | Jetpack Compose rewrite (current XML UI is ~90 lines; cheapest moment to switch). |
| Persistence | Jetpack DataStore (Preferences), serialized progress map. Room rejected as overkill. |

## 3. Architecture

Single Activity, Compose UI, three packages under `us.jmresearch.abcflashcards`:

- **`data`** — static curriculum content (`Curriculum.kt`) + `ProgressStore` (DataStore wrapper)
- **`engine`** — pure Kotlin, zero Android dependencies: mastery logic, item picker, unlock rules. Fully unit-testable.
- **`ui`** — Compose screens + navigation

### Screens

1. **HomeScreen** — deck grid grouped by subject (Letters, Words, Phrases, Math). Locked decks visible with 🔒. Long-press (3s) on title opens ParentScreen.
2. **DeckScreen** — big card center; tap card = next item; ✓ **Got it** / ✗ **Not yet** buttons; progress dots for deck mastery.
3. **ParentScreen** — per-deck progress, per-deck reset, mastery threshold setting, force-unlock override per deck.

## 4. Data Model

```kotlin
data class CardItem(val id: String, val front: String, val back: String? = null)

sealed interface UnlockRule {
    data object None : UnlockRule
    data class DeckMastered(val deckId: String) : UnlockRule
    data class DecksMastered(val deckIds: List<String>) : UnlockRule
}

data class Deck(
    val id: String,
    val title: String,
    val subject: Subject,        // LETTERS, WORDS, PHRASES, MATH
    val items: List<CardItem>,
    val unlockRule: UnlockRule
)

data class ItemProgress(val correctCount: Int = 0, val lastSeenEpochDay: Long = 0)
```

- Letters item: `front = "S s"` (both cases together), no back.
- Word/phrase item: `front = "sat"`, no back.
- Math item: `front = "3 + 2"`, `back = "5"`. Counting: `front` = dot pattern, `back` = numeral.
- Progress persisted as `Map<itemId, ItemProgress>` in DataStore. Item ids are stable strings (`"letter_s"`, `"word_sat"`, `"add_3_2"`).
- Android auto-backup already enabled → progress survives reinstall.

## 5. Engine Rules (pure functions)

- **✓ tap:** `correctCount++`, update `lastSeenEpochDay`.
- **✗ tap:** `correctCount = max(0, correctCount - 1)`, item stays in rotation.
- **Mastered:** `correctCount >= threshold` (threshold from settings, default 3).
- **Deck mastered:** all items mastered.
- **Item picker:**
  - 1-in-8 draws pull from the mastered pool (oldest `lastSeen` first) — spaced review.
  - Otherwise random among unmastered, weighted toward low `correctCount`.
  - Never the same item twice in a row (unless it is the only item left).
- **Unlocks:** deck locked until its `UnlockRule` is satisfied, OR parent has force-unlocked it.

## 6. Curriculum Content

All content lives in `data/Curriculum.kt` as plain Kotlin lists. No DB, no migrations.

### Letters — 6 decks (Jolly Phonics order)
1. `s a t p i n`
2. `c k e h r m d`
3. `g o u l f b`
4. `j z w v y x`
5. `q` (shown as `Qu qu`)
6. Review: all 26 mixed (unlocks when groups 1–5 mastered)

Each unlocks when the previous group is mastered. Group 1 always unlocked.

### Words
- **CVC Set 1** (unlock: letter group 1): sat, pat, tap, sap, pin, tin, nap, pan, tan, sit, pit, nip, in
- **CVC Set 2** (unlock: letter group 2): cat, hat, mat, rat, red, hen, ten, met, pet, dim, rim, ram, mad, sad, dad, ham
- **CVC Set 3** (unlock: letter group 3): dog, log, bug, fun, run, sun, bed, big, dig, leg, gum, cup, cut, lot, fog, bat
- **Sight Words** (unlock: CVC Set 2): the, a, I, to, is, it (Dolch pre-K subset)

### Phrases
- **Phrase Set 1** (unlock: CVC Set 1 + Sight Words): I sat, tap it, a pin, sit in it, it is a nap
- **Phrase Set 2** (unlock: CVC Set 2 + Sight Words): the red hen, a red hat, a sad dad, the cat is mad, a pet ram

Constraint: phrases use only words from prerequisite decks (+ sight words). Enforced by automated test.

### Math
1. **Numbers 0–10** (always unlocked): numeral recognition cards
2. **Counting 1–10** (unlock: Numbers): subitizing dot patterns (dice-style layouts), back = numeral
3. **Addition to 10** (unlock: Counting): `a + b` where `a+b ≤ 10`, back = sum
4. **Subtraction within 10** (unlock: Counting): `a − b`, `a ≤ 10`, back = difference
5. **🔒 Multiplication** (unlock: Addition + Subtraction): tables 1–5
6. **🔒 Division** (unlock: Multiplication): matching fact family divisions

## 7. Error Handling / Edge Cases

- Empty unmastered pool + spaced-review roll → serve mastered item.
- All decks mastered → deck shows celebratory "All done!" card; parent can reset.
- Progress read failure (corrupt DataStore) → start fresh, log warning; never crash.
- Threshold lowered in settings below an item's count → item immediately counts as mastered (derived, not stored).

## 8. Testing

- **Engine unit tests:** mastery counting, decrement floor, picker weighting/no-repeat, spaced-review ratio, unlock rule evaluation.
- **Curriculum validation test:** every CVC word uses only letters from its prerequisite letter groups; every phrase uses only words from its prerequisite decks + sight words. Fails build if content edit breaks decodability.
- **UI smoke test:** app launches, home renders all subjects, deck opens, ✓ advances card.

## 8.5 Iteration 2 (added 2026-07-23 after first tablet session)

User feedback drove these changes:

- **Tabs:** bottom navigation bar (Letters / Words / Phrases / Math) replaces single scrolling home grid.
- **Deck completion:** when every item in a deck is mastered, DeckScreen shows a celebration screen ("<Deck> Mastered!" + Back to menu / Keep practicing). "Keep practicing" enters review mode and keeps serving cards.
- **In-deck progress:** progress bar + "X/Y ⭐" at top of DeckScreen; pips under the card show current item's correctCount vs threshold. Deck tiles show a mini progress bar and ⭐ when complete.
- **Reset UX:** parent screen reset is an always-enabled button per deck with a confirmation dialog. The force-unlock Switch was removed (its disabled state next to Reset read as "reset disabled"); replaced with explicit "Unlock" / "Re-lock" text buttons shown only when applicable.
- **Parent access:** visible ⚙️ button on home (long-press gate deferred to solo-kid mode).
- **Kid profiles:** multiple named profiles (add/switch via chip on home; delete in parent screen). Each profile has fully separate progress/threshold/force-unlocks, namespaced in the same DataStore (`progress_v1_<pid>` etc.). Legacy un-namespaced keys are read as a fallback for the first profile (`p1`), so pre-profile progress migrates transparently. Threshold is per-profile.
- Curriculum for the 7–9 year old: follow-up project, not in this iteration.

## 9. Future (explicitly out of scope now)

- Solo-kid quiz mode (tap-the-answer cards, audio prompts, TTS or recorded voice)
- Rewards/stars system + parent gate hardening
- Letter-sound audio
- These bolt onto the same engine/data model; no rewrite anticipated.

## 10. Known Repo Issues to Fix During Implementation

- `MainActivity.kt` declares `package us.jmresearch.abcs` but namespace/appId is `us.jmresearch.abcflashcards` — mismatch, fix during Compose migration.
- Old XML layout, nav_graph, and unused Navigation-fragment deps removed in Compose rewrite.
