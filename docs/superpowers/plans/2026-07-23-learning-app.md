# Kid Learning App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild ABCFlashCards as a Compose app with mastery-tracked decks for letters (phonics order), decodable words, phrases, and a math ladder, with persistent progress and parent controls.

**Architecture:** Single Activity, Jetpack Compose UI, three packages: `data` (static curriculum + DataStore progress), `engine` (pure-Kotlin mastery/picker/unlock logic, fully unit tested), `ui` (Compose screens, state-based navigation — no navigation library). Spec: `docs/superpowers/specs/2026-07-23-learning-app-design.md`.

**Tech Stack:** Kotlin 1.9.24, AGP 8.4.0, Jetpack Compose (BOM 2024.06.00, compiler ext 1.5.14), DataStore Preferences 1.1.1, JUnit4.

## Global Constraints

- Package/namespace: `us.jmresearch.abcflashcards` (fix the existing `us.jmresearch.abcs` mismatch).
- `engine` package: zero Android imports. Pure Kotlin only.
- All curriculum content in `data/Curriculum.kt`; stable item ids (`letter_s`, `word_sat`, `add_2_3`, …) — ids are persisted, never rename them later.
- Mastery threshold default 3, runtime-configurable; mastery is DERIVED (`correctCount >= threshold`), never stored as a flag.
- Every phrase/word must be decodable from prerequisite decks — enforced by `CurriculumTest`.
- Run all JVM tests with: `./gradlew testDebugUnitTest`. Full build: `./gradlew assembleDebug`.
- Windows host: use `gradlew.bat` via Bash tool as `./gradlew`.

---

### Task 1: Toolchain — Compose enabled, package mismatch fixed, build green

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/us/jmresearch/abcflashcards/MainActivity.kt` (package line only)
- Delete: nothing yet (old XML stays until Task 7)

**Interfaces:**
- Produces: a compiling project with Compose available; later tasks add code under `us.jmresearch.abcflashcards`.

- [ ] **Step 1: Update version catalog**

Replace `gradle/libs.versions.toml` content:

```toml
[versions]
agp = "8.4.0"
kotlin = "1.9.24"
coreKtx = "1.13.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
appcompat = "1.6.1"
material = "1.10.0"
composeBom = "2024.06.00"
composeCompiler = "1.5.14"
activityCompose = "1.9.0"
lifecycle = "2.8.2"
datastore = "1.1.1"
coroutinesTest = "1.8.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
jetbrains-kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

(Navigation-fragment/ui and constraintlayout entries removed — nothing will reference them after Task 7; removing now is fine because Task 1 also removes their `dependencies` lines.)

- [ ] **Step 2: Update app/build.gradle.kts**

Replace the `android {}` `buildFeatures` block and `dependencies` block; add composeOptions. Full file:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "us.jmresearch.abcflashcards"
    compileSdk = 34

    defaultConfig {
        applicationId = "us.jmresearch.abcflashcards"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
```

- [ ] **Step 3: Fix package mismatch**

In `app/src/main/java/us/jmresearch/abcflashcards/MainActivity.kt` change line 1 from `package us.jmresearch.abcs` to `package us.jmresearch.abcflashcards`. Leave the rest — it is replaced in Task 7.

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL` (first run downloads dependencies; allow up to 10 min timeout).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/us/jmresearch/abcflashcards/MainActivity.kt
git commit -m "chore: enable Compose, bump Kotlin to 1.9.24, fix package mismatch"
```

---

### Task 2: Data model + mastery engine

**Files:**
- Create: `app/src/main/java/us/jmresearch/abcflashcards/data/Model.kt`
- Create: `app/src/main/java/us/jmresearch/abcflashcards/engine/Mastery.kt`
- Test: `app/src/test/java/us/jmresearch/abcflashcards/engine/MasteryTest.kt`

**Interfaces:**
- Produces (used by every later task):
  - `data class CardItem(id: String, front: String, back: String? = null)`
  - `enum class Subject { LETTERS, WORDS, PHRASES, MATH }`
  - `sealed interface UnlockRule` with `data object None` and `data class DecksMastered(val deckIds: List<String>)`
  - `data class Deck(id: String, title: String, subject: Subject, items: List<CardItem>, unlockRule: UnlockRule)`
  - `data class ItemProgress(correctCount: Int = 0, lastSeenEpochDay: Long = 0)`
  - `fun applyCorrect(p: ItemProgress, today: Long): ItemProgress`
  - `fun applyWrong(p: ItemProgress, today: Long): ItemProgress`
  - `fun isMastered(p: ItemProgress?, threshold: Int): Boolean`
  - `fun isDeckMastered(deck: Deck, progress: Map<String, ItemProgress>, threshold: Int): Boolean`
  - `fun isDeckUnlocked(deck: Deck, allDecks: List<Deck>, progress: Map<String, ItemProgress>, threshold: Int, forceUnlocked: Set<String>): Boolean`

Note: the spec sketches both `DeckMastered` and `DecksMastered`; a single-element `DecksMastered` covers the former — implement only `DecksMastered` (DRY).

- [ ] **Step 1: Write Model.kt** (data only, no test needed beyond compilation)

```kotlin
package us.jmresearch.abcflashcards.data

data class CardItem(val id: String, val front: String, val back: String? = null)

enum class Subject { LETTERS, WORDS, PHRASES, MATH }

sealed interface UnlockRule {
    data object None : UnlockRule
    data class DecksMastered(val deckIds: List<String>) : UnlockRule
}

data class Deck(
    val id: String,
    val title: String,
    val subject: Subject,
    val items: List<CardItem>,
    val unlockRule: UnlockRule = UnlockRule.None,
)

data class ItemProgress(val correctCount: Int = 0, val lastSeenEpochDay: Long = 0)
```

- [ ] **Step 2: Write failing tests** — `MasteryTest.kt`:

```kotlin
package us.jmresearch.abcflashcards.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.Deck
import us.jmresearch.abcflashcards.data.ItemProgress
import us.jmresearch.abcflashcards.data.Subject
import us.jmresearch.abcflashcards.data.UnlockRule

class MasteryTest {

    private fun deck(id: String, vararg itemIds: String, rule: UnlockRule = UnlockRule.None) =
        Deck(id, id, Subject.LETTERS, itemIds.map { CardItem(it, it) }, rule)

    @Test fun correctIncrementsAndStampsDay() {
        val p = applyCorrect(ItemProgress(1, 10), today = 20)
        assertEquals(2, p.correctCount)
        assertEquals(20, p.lastSeenEpochDay)
    }

    @Test fun wrongDecrementsWithFloorAtZero() {
        assertEquals(0, applyWrong(ItemProgress(0, 0), 5).correctCount)
        assertEquals(1, applyWrong(ItemProgress(2, 0), 5).correctCount)
        assertEquals(5, applyWrong(ItemProgress(2, 0), 5).lastSeenEpochDay)
    }

    @Test fun masteryIsDerivedFromThreshold() {
        assertFalse(isMastered(ItemProgress(2), threshold = 3))
        assertTrue(isMastered(ItemProgress(3), threshold = 3))
        assertTrue(isMastered(ItemProgress(3), threshold = 2)) // lowering threshold masters retroactively
        assertFalse(isMastered(null, threshold = 3))
    }

    @Test fun deckMasteredRequiresAllItems() {
        val d = deck("d1", "a", "b")
        val partial = mapOf("a" to ItemProgress(3))
        val full = mapOf("a" to ItemProgress(3), "b" to ItemProgress(4))
        assertFalse(isDeckMastered(d, partial, 3))
        assertTrue(isDeckMastered(d, full, 3))
    }

    @Test fun unlockRules() {
        val d1 = deck("d1", "a")
        val d2 = deck("d2", "b", rule = UnlockRule.DecksMastered(listOf("d1")))
        val none = emptyMap<String, ItemProgress>()
        val d1Done = mapOf("a" to ItemProgress(3))
        assertTrue(isDeckUnlocked(d1, listOf(d1, d2), none, 3, emptySet()))
        assertFalse(isDeckUnlocked(d2, listOf(d1, d2), none, 3, emptySet()))
        assertTrue(isDeckUnlocked(d2, listOf(d1, d2), d1Done, 3, emptySet()))
        assertTrue(isDeckUnlocked(d2, listOf(d1, d2), none, 3, setOf("d2"))) // force-unlock
    }
}
```

- [ ] **Step 3: Run to verify failure**

Run: `./gradlew testDebugUnitTest --tests "us.jmresearch.abcflashcards.engine.MasteryTest"`
Expected: FAIL — unresolved references (`applyCorrect` etc.).

- [ ] **Step 4: Implement `Mastery.kt`**

```kotlin
package us.jmresearch.abcflashcards.engine

import us.jmresearch.abcflashcards.data.Deck
import us.jmresearch.abcflashcards.data.ItemProgress
import us.jmresearch.abcflashcards.data.UnlockRule

fun applyCorrect(p: ItemProgress, today: Long): ItemProgress =
    p.copy(correctCount = p.correctCount + 1, lastSeenEpochDay = today)

fun applyWrong(p: ItemProgress, today: Long): ItemProgress =
    p.copy(correctCount = (p.correctCount - 1).coerceAtLeast(0), lastSeenEpochDay = today)

fun isMastered(p: ItemProgress?, threshold: Int): Boolean =
    p != null && p.correctCount >= threshold

fun isDeckMastered(deck: Deck, progress: Map<String, ItemProgress>, threshold: Int): Boolean =
    deck.items.all { isMastered(progress[it.id], threshold) }

fun isDeckUnlocked(
    deck: Deck,
    allDecks: List<Deck>,
    progress: Map<String, ItemProgress>,
    threshold: Int,
    forceUnlocked: Set<String>,
): Boolean {
    if (deck.id in forceUnlocked) return true
    return when (val rule = deck.unlockRule) {
        is UnlockRule.None -> true
        is UnlockRule.DecksMastered -> rule.deckIds.all { id ->
            val dep = allDecks.firstOrNull { it.id == id } ?: return false
            isDeckMastered(dep, progress, threshold)
        }
    }
}
```

- [ ] **Step 5: Run tests — expect PASS**

Run: `./gradlew testDebugUnitTest --tests "us.jmresearch.abcflashcards.engine.MasteryTest"`
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/us/jmresearch/abcflashcards/data/Model.kt app/src/main/java/us/jmresearch/abcflashcards/engine/Mastery.kt app/src/test/java/us/jmresearch/abcflashcards/engine/MasteryTest.kt
git commit -m "feat: data model and mastery engine with tests"
```

---

### Task 3: Card picker

**Files:**
- Create: `app/src/main/java/us/jmresearch/abcflashcards/engine/CardPicker.kt`
- Test: `app/src/test/java/us/jmresearch/abcflashcards/engine/CardPickerTest.kt`

**Interfaces:**
- Consumes: `CardItem`, `ItemProgress`, `isMastered` from Task 2.
- Produces: `fun pickNext(items: List<CardItem>, progress: Map<String, ItemProgress>, threshold: Int, lastShownId: String?, random: kotlin.random.Random): CardItem?`

Behavior (from spec §5):
1. Returns `null` only for an empty item list.
2. Excludes `lastShownId` unless it is the only candidate.
3. If all items mastered → return mastered item with oldest `lastSeenEpochDay`.
4. Else with probability 1/8 (`random.nextInt(8) == 0`) AND mastered pool non-empty → oldest-seen mastered item (spaced review).
5. Else weighted random among unmastered: weight `(threshold - correctCount).coerceAtLeast(1)`.

- [ ] **Step 1: Write failing tests** — `CardPickerTest.kt`:

```kotlin
package us.jmresearch.abcflashcards.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.ItemProgress

class CardPickerTest {

    private val items = listOf(CardItem("a", "a"), CardItem("b", "b"), CardItem("c", "c"))

    @Test fun emptyDeckReturnsNull() {
        assertNull(pickNext(emptyList(), emptyMap(), 3, null, Random(1)))
    }

    @Test fun neverRepeatsLastShownWhenAlternativesExist() {
        repeat(50) { seed ->
            val pick = pickNext(items, emptyMap(), 3, lastShownId = "a", random = Random(seed))
            assertNotEquals("a", pick!!.id)
        }
    }

    @Test fun singleItemDeckMayRepeat() {
        val one = listOf(CardItem("a", "a"))
        assertEquals("a", pickNext(one, emptyMap(), 3, lastShownId = "a", random = Random(1))!!.id)
    }

    @Test fun allMasteredServesOldestSeen() {
        val progress = mapOf(
            "a" to ItemProgress(3, lastSeenEpochDay = 100),
            "b" to ItemProgress(3, lastSeenEpochDay = 50),
            "c" to ItemProgress(3, lastSeenEpochDay = 75),
        )
        assertEquals("b", pickNext(items, progress, 3, null, Random(1))!!.id)
    }

    @Test fun spacedReviewRollServesMasteredItem() {
        // Random(seed).nextInt(8) == 0 must hold for the chosen seed; seed 5 gives 0.
        val seed = generateSequence(0) { it + 1 }.first { Random(it).nextInt(8) == 0 }
        val progress = mapOf("a" to ItemProgress(3, lastSeenEpochDay = 10))
        val pick = pickNext(items, progress, 3, null, Random(seed))
        assertEquals("a", pick!!.id)
    }

    @Test fun unmasteredPicksFavorLowCounts() {
        // "a" has count 0 (weight 3), "b" count 2 (weight 1); over many draws "a" must dominate.
        val two = listOf(CardItem("a", "a"), CardItem("b", "b"))
        val progress = mapOf("b" to ItemProgress(2))
        var aWins = 0
        repeat(1000) { seed ->
            val r = Random(seed)
            if (r.nextInt(8) == 0) return@repeat // skip spaced-review draws (no mastered pool anyway, but keep draws comparable)
            if (pickNext(two, progress, 3, null, Random(seed))!!.id == "a") aWins++
        }
        assertTrue("expected a to win most draws, won $aWins", aWins > 600)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew testDebugUnitTest --tests "us.jmresearch.abcflashcards.engine.CardPickerTest"`
Expected: FAIL — `pickNext` unresolved.

- [ ] **Step 3: Implement `CardPicker.kt`**

```kotlin
package us.jmresearch.abcflashcards.engine

import kotlin.random.Random
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.ItemProgress

fun pickNext(
    items: List<CardItem>,
    progress: Map<String, ItemProgress>,
    threshold: Int,
    lastShownId: String?,
    random: Random,
): CardItem? {
    if (items.isEmpty()) return null
    val candidates = items.filter { it.id != lastShownId }.ifEmpty { items }
    val (mastered, unmastered) = candidates.partition { isMastered(progress[it.id], threshold) }

    fun oldestMastered() = mastered.minBy { progress[it.id]?.lastSeenEpochDay ?: 0 }

    if (unmastered.isEmpty()) return oldestMastered()
    if (mastered.isNotEmpty() && random.nextInt(8) == 0) return oldestMastered()

    val weights = unmastered.map { item ->
        val count = progress[item.id]?.correctCount ?: 0
        (threshold - count).coerceAtLeast(1)
    }
    var roll = random.nextInt(weights.sum())
    for ((i, w) in weights.withIndex()) {
        roll -= w
        if (roll < 0) return unmastered[i]
    }
    return unmastered.last()
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `./gradlew testDebugUnitTest --tests "us.jmresearch.abcflashcards.engine.CardPickerTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/us/jmresearch/abcflashcards/engine/CardPicker.kt app/src/test/java/us/jmresearch/abcflashcards/engine/CardPickerTest.kt
git commit -m "feat: weighted card picker with spaced review"
```

---

### Task 4: Curriculum content + decodability validation

**Files:**
- Create: `app/src/main/java/us/jmresearch/abcflashcards/data/Curriculum.kt`
- Test: `app/src/test/java/us/jmresearch/abcflashcards/data/CurriculumTest.kt`

**Interfaces:**
- Consumes: `Deck`, `CardItem`, `Subject`, `UnlockRule` from Task 2.
- Produces: `object Curriculum { val decks: List<Deck> }` plus deck id constants used by ViewModel/UI:
  deck ids: `letters_1`..`letters_5`, `letters_review`, `cvc_1`, `cvc_2`, `cvc_3`, `sight_1`, `phrases_1`, `phrases_2`, `numbers`, `counting`, `addition`, `subtraction`, `multiplication`, `division`.
- Item id scheme: `letter_<char>`, `word_<word>`, `sight_<word>`, `phrase_<words_joined_by_underscore>`, `num_<n>`, `count_<n>`, `add_<a>_<b>`, `sub_<a>_<b>`, `mul_<a>_<b>`, `div_<a>_<b>`.

- [ ] **Step 1: Write failing test** — `CurriculumTest.kt`:

```kotlin
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

    @Test fun mathFactsAreCorrectAndInRange() {
        deck("addition").items.forEach {
            val (a, b) = it.id.removePrefix("add_").split("_").map(String::toInt)
            assertTrue(a + b <= 10)
            assertEquals((a + b).toString(), it.back)
        }
        deck("subtraction").items.forEach {
            val (a, b) = it.id.removePrefix("sub_").split("_").map(String::toInt)
            assertTrue(a <= 10 && a - b >= 0)
            assertEquals((a - b).toString(), it.back)
        }
        deck("multiplication").items.forEach {
            val (a, b) = it.id.removePrefix("mul_").split("_").map(String::toInt)
            assertEquals((a * b).toString(), it.back)
        }
        deck("division").items.forEach {
            val (a, b) = it.id.removePrefix("div_").split("_").map(String::toInt)
            assertEquals(0, a % b)
            assertEquals((a / b).toString(), it.back)
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
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew testDebugUnitTest --tests "us.jmresearch.abcflashcards.data.CurriculumTest"`
Expected: FAIL — `Curriculum` unresolved.

- [ ] **Step 3: Implement `Curriculum.kt`**

```kotlin
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

    val decks: List<Deck> = buildList {
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
```

Note: `letters_review` items use ids `letterreview_<c>` (distinct from `letter_<c>`) so review mastery is tracked separately and the all-item-ids-unique test passes. The decodability test only reads `letter_`-prefixed decks via groups 1–3, unaffected.

Note: `cvc_2`/`cvc_3` unlock on the previous word set AND the letter group (stricter than spec's letter-group-only rule — matches intent: kid should also finish previous word set). CurriculumTest's `unlockChainMatchesSpec` covers `cvc_1` only, deliberately.

- [ ] **Step 4: Run tests — expect PASS**

Run: `./gradlew testDebugUnitTest --tests "us.jmresearch.abcflashcards.data.CurriculumTest"`
Expected: PASS (6 tests). If `cvcWordsAreDecodableFromPrerequisiteLetterGroups` fails, the word list contains a letter outside its groups — fix the word list, not the test.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/us/jmresearch/abcflashcards/data/Curriculum.kt app/src/test/java/us/jmresearch/abcflashcards/data/CurriculumTest.kt
git commit -m "feat: full curriculum content with decodability validation"
```

---

### Task 5: Progress persistence (codec + DataStore + settings)

**Files:**
- Create: `app/src/main/java/us/jmresearch/abcflashcards/data/ProgressCodec.kt`
- Create: `app/src/main/java/us/jmresearch/abcflashcards/data/ProgressStore.kt`
- Test: `app/src/test/java/us/jmresearch/abcflashcards/data/ProgressCodecTest.kt`

**Interfaces:**
- Consumes: `ItemProgress` from Task 2.
- Produces:
  - `fun encodeProgress(map: Map<String, ItemProgress>): String`
  - `fun decodeProgress(raw: String): Map<String, ItemProgress>` (corrupt entries silently dropped — spec §7)
  - `class ProgressStore(context: Context)` exposing:
    - `val progress: Flow<Map<String, ItemProgress>>`
    - `val threshold: Flow<Int>` (default 3)
    - `val forceUnlocked: Flow<Set<String>>`
    - `suspend fun updateItem(itemId: String, transform: (ItemProgress) -> ItemProgress)`
    - `suspend fun resetDeck(itemIds: List<String>)`
    - `suspend fun setThreshold(value: Int)`
    - `suspend fun setForceUnlocked(deckId: String, unlocked: Boolean)`

- [ ] **Step 1: Write failing codec tests** — `ProgressCodecTest.kt`:

```kotlin
package us.jmresearch.abcflashcards.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCodecTest {

    @Test fun roundTrip() {
        val map = mapOf(
            "letter_s" to ItemProgress(3, 19923),
            "add_2_3" to ItemProgress(1, 20000),
        )
        assertEquals(map, decodeProgress(encodeProgress(map)))
    }

    @Test fun emptyRoundTrip() {
        assertEquals(emptyMap<String, ItemProgress>(), decodeProgress(encodeProgress(emptyMap())))
        assertEquals(emptyMap<String, ItemProgress>(), decodeProgress(""))
    }

    @Test fun corruptEntriesDroppedNotCrashed() {
        val decoded = decodeProgress("letter_s:3:100;garbage;word_sat:x:5;word_pat:2:50")
        assertEquals(ItemProgress(3, 100), decoded["letter_s"])
        assertEquals(ItemProgress(2, 50), decoded["word_pat"])
        assertTrue("garbage entries must be dropped", decoded.size == 2)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew testDebugUnitTest --tests "us.jmresearch.abcflashcards.data.ProgressCodecTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement `ProgressCodec.kt`**

Format: `itemId:correctCount:lastSeenEpochDay` entries joined with `;`. Item ids never contain `:` or `;` (enforced by id scheme in Task 4).

```kotlin
package us.jmresearch.abcflashcards.data

fun encodeProgress(map: Map<String, ItemProgress>): String =
    map.entries.joinToString(";") { (id, p) -> "$id:${p.correctCount}:${p.lastSeenEpochDay}" }

fun decodeProgress(raw: String): Map<String, ItemProgress> =
    raw.split(";").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size != 3) return@mapNotNull null
        val count = parts[1].toIntOrNull() ?: return@mapNotNull null
        val day = parts[2].toLongOrNull() ?: return@mapNotNull null
        parts[0] to ItemProgress(count, day)
    }.toMap()
```

- [ ] **Step 4: Run codec tests — expect PASS**

Run: `./gradlew testDebugUnitTest --tests "us.jmresearch.abcflashcards.data.ProgressCodecTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Implement `ProgressStore.kt`** (thin DataStore wrapper — covered by UI smoke test, not unit tests)

```kotlin
package us.jmresearch.abcflashcards.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "progress")

class ProgressStore(private val context: Context) {

    private val progressKey = stringPreferencesKey("progress_v1")
    private val thresholdKey = intPreferencesKey("threshold_v1")
    private val forceUnlockedKey = stringSetPreferencesKey("force_unlocked_v1")

    val progress: Flow<Map<String, ItemProgress>> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> decodeProgress(prefs[progressKey] ?: "") }

    val threshold: Flow<Int> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[thresholdKey] ?: 3 }

    val forceUnlocked: Flow<Set<String>> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[forceUnlockedKey] ?: emptySet() }

    suspend fun updateItem(itemId: String, transform: (ItemProgress) -> ItemProgress) {
        context.dataStore.edit { prefs ->
            val map = decodeProgress(prefs[progressKey] ?: "").toMutableMap()
            map[itemId] = transform(map[itemId] ?: ItemProgress())
            prefs[progressKey] = encodeProgress(map)
        }
    }

    suspend fun resetDeck(itemIds: List<String>) {
        context.dataStore.edit { prefs ->
            val map = decodeProgress(prefs[progressKey] ?: "").toMutableMap()
            itemIds.forEach { map.remove(it) }
            prefs[progressKey] = encodeProgress(map)
        }
    }

    suspend fun setThreshold(value: Int) {
        context.dataStore.edit { it[thresholdKey] = value.coerceIn(1, 10) }
    }

    suspend fun setForceUnlocked(deckId: String, unlocked: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[forceUnlockedKey] ?: emptySet()
            prefs[forceUnlockedKey] = if (unlocked) current + deckId else current - deckId
        }
    }
}
```

- [ ] **Step 6: Verify full build + all tests**

Run: `./gradlew testDebugUnitTest`
Expected: all tests PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/us/jmresearch/abcflashcards/data/ProgressCodec.kt app/src/main/java/us/jmresearch/abcflashcards/data/ProgressStore.kt app/src/test/java/us/jmresearch/abcflashcards/data/ProgressCodecTest.kt
git commit -m "feat: progress persistence via DataStore with corrupt-safe codec"
```

---

### Task 6: AppViewModel

**Files:**
- Create: `app/src/main/java/us/jmresearch/abcflashcards/ui/AppViewModel.kt`

**Interfaces:**
- Consumes: `Curriculum.decks`, `ProgressStore`, engine functions, `pickNext`.
- Produces (UI relies on these exact names):

```kotlin
data class DeckStatus(val deck: Deck, val unlocked: Boolean, val masteredCount: Int, val total: Int)
data class AppState(
    val deckStatuses: List<DeckStatus> = emptyList(),
    val threshold: Int = 3,
    val forceUnlocked: Set<String> = emptySet(),
    val progress: Map<String, ItemProgress> = emptyMap(),
)
class AppViewModel(store: ProgressStore) : ViewModel() {
    val state: StateFlow<AppState>
    val currentCard: StateFlow<CardItem?>   // null when no deck open
    fun openDeck(deckId: String)
    fun closeDeck()
    fun markCorrect()
    fun markWrong()
    fun resetDeck(deckId: String)
    fun setThreshold(value: Int)
    fun toggleForceUnlock(deckId: String)
    companion object { fun factory(context: Context): ViewModelProvider.Factory }
}
```

- [ ] **Step 1: Implement `AppViewModel.kt`**

```kotlin
package us.jmresearch.abcflashcards.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import us.jmresearch.abcflashcards.data.CardItem
import us.jmresearch.abcflashcards.data.Curriculum
import us.jmresearch.abcflashcards.data.Deck
import us.jmresearch.abcflashcards.data.ItemProgress
import us.jmresearch.abcflashcards.data.ProgressStore
import us.jmresearch.abcflashcards.engine.applyCorrect
import us.jmresearch.abcflashcards.engine.applyWrong
import us.jmresearch.abcflashcards.engine.isDeckUnlocked
import us.jmresearch.abcflashcards.engine.isMastered
import us.jmresearch.abcflashcards.engine.pickNext

data class DeckStatus(val deck: Deck, val unlocked: Boolean, val masteredCount: Int, val total: Int)

data class AppState(
    val deckStatuses: List<DeckStatus> = emptyList(),
    val threshold: Int = 3,
    val forceUnlocked: Set<String> = emptySet(),
    val progress: Map<String, ItemProgress> = emptyMap(),
)

class AppViewModel(private val store: ProgressStore) : ViewModel() {

    private val random = Random(System.nanoTime())

    val state: StateFlow<AppState> =
        combine(store.progress, store.threshold, store.forceUnlocked) { progress, threshold, force ->
            val statuses = Curriculum.decks.map { deck ->
                DeckStatus(
                    deck = deck,
                    unlocked = isDeckUnlocked(deck, Curriculum.decks, progress, threshold, force),
                    masteredCount = deck.items.count { isMastered(progress[it.id], threshold) },
                    total = deck.items.size,
                )
            }
            AppState(statuses, threshold, force, progress)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    private val openDeckId = MutableStateFlow<String?>(null)
    private val _currentCard = MutableStateFlow<CardItem?>(null)
    val currentCard: StateFlow<CardItem?> = _currentCard

    private fun deckById(id: String?): Deck? = Curriculum.decks.firstOrNull { it.id == id }

    fun openDeck(deckId: String) {
        openDeckId.value = deckId
        advance(lastShownId = null)
    }

    fun closeDeck() {
        openDeckId.value = null
        _currentCard.value = null
    }

    private fun advance(lastShownId: String?) {
        val deck = deckById(openDeckId.value) ?: return
        val s = state.value
        _currentCard.value = pickNext(deck.items, s.progress, s.threshold, lastShownId, random)
    }

    fun markCorrect() = mark(::applyCorrect)
    fun markWrong() = mark(::applyWrong)

    private fun mark(transform: (ItemProgress, Long) -> ItemProgress) {
        val card = _currentCard.value ?: return
        val today = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            store.updateItem(card.id) { transform(it, today) }
            advance(lastShownId = card.id)
        }
    }

    fun resetDeck(deckId: String) {
        val deck = deckById(deckId) ?: return
        viewModelScope.launch { store.resetDeck(deck.items.map { it.id }) }
    }

    fun setThreshold(value: Int) {
        viewModelScope.launch { store.setThreshold(value) }
    }

    fun toggleForceUnlock(deckId: String) {
        viewModelScope.launch {
            store.setForceUnlocked(deckId, deckId !in state.value.forceUnlocked)
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(ProgressStore(context.applicationContext)) }
        }
    }
}
```

Known subtlety: `advance` inside `mark`'s coroutine runs after `store.updateItem` returns, but `state` updates asynchronously via the flow; the picked card may use just-stale progress for one draw. Acceptable — next draw corrects, and no-repeat rule still holds via `lastShownId`.

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/us/jmresearch/abcflashcards/ui/AppViewModel.kt
git commit -m "feat: AppViewModel wiring engine, curriculum, and persistence"
```

---

### Task 7: Compose UI — Home, Deck, Parent screens + MainActivity rewrite

**Files:**
- Create: `app/src/main/java/us/jmresearch/abcflashcards/ui/App.kt` (navigation + screens)
- Modify: `app/src/main/java/us/jmresearch/abcflashcards/MainActivity.kt` (full rewrite)
- Delete: `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/navigation/nav_graph.xml`
- Modify: `app/src/main/res/values/strings.xml` (remove nav-related strings if present; keep `app_name`)

**Interfaces:**
- Consumes: `AppViewModel` API from Task 6, `Subject` enum.
- Produces: `@Composable fun App(vm: AppViewModel)` — the single entry point used by MainActivity.

- [ ] **Step 1: Implement `App.kt`**

```kotlin
package us.jmresearch.abcflashcards.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.jmresearch.abcflashcards.data.Subject

private val subjectColors = mapOf(
    Subject.LETTERS to Color(0xFF7E57C2),
    Subject.WORDS to Color(0xFF26A69A),
    Subject.PHRASES to Color(0xFFEF6C00),
    Subject.MATH to Color(0xFF42A5F5),
)

private val subjectTitles = mapOf(
    Subject.LETTERS to "Letters",
    Subject.WORDS to "Words",
    Subject.PHRASES to "Phrases",
    Subject.MATH to "Math",
)

@Composable
fun App(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    var screen by remember { mutableStateOf<String>("home") } // "home" | "deck" | "parent"

    when (screen) {
        "home" -> HomeScreen(
            state = state,
            onDeckTap = { deckId -> vm.openDeck(deckId); screen = "deck" },
            onParentOpen = { screen = "parent" },
        )
        "deck" -> {
            BackHandler { vm.closeDeck(); screen = "home" }
            DeckScreen(vm = vm, onClose = { vm.closeDeck(); screen = "home" })
        }
        "parent" -> {
            BackHandler { screen = "home" }
            ParentScreen(vm = vm, state = state, onClose = { screen = "home" })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(state: AppState, onDeckTap: (String) -> Unit, onParentOpen: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Text(
                "Let's Learn!",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .combinedClickable(onClick = {}, onLongClick = onParentOpen),
            )
        }
        Subject.entries.forEach { subject ->
            val decks = state.deckStatuses.filter { it.deck.subject == subject }
            if (decks.isEmpty()) return@forEach
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Text(
                    subjectTitles.getValue(subject),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(decks, key = { it.deck.id }) { status ->
                DeckTile(status, subjectColors.getValue(subject), onDeckTap)
            }
        }
    }
}

@Composable
private fun DeckTile(status: DeckStatus, color: Color, onDeckTap: (String) -> Unit) {
    val enabled = status.unlocked
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(96.dp)
            .clickable(enabled = enabled) { onDeckTap(status.deck.id) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (enabled) color else Color(0xFFBDBDBD)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (enabled) status.deck.title else "🔒 ${status.deck.title}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${status.masteredCount}/${status.total}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun DeckScreen(vm: AppViewModel, onClose: () -> Unit) {
    val card by vm.currentCard.collectAsState()
    val state by vm.state.collectAsState()
    var showBack by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.Start)) {
            Text("← Back", fontSize = 18.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { if (card?.back != null) showBack = !showBack },
            contentAlignment = Alignment.Center,
        ) {
            val c = card
            if (c == null) {
                Text("All Done! 🎉", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (showBack && c.back != null) c.back!! else c.front,
                        fontSize = if (c.front.length > 8) 44.sp else 96.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 100.sp,
                    )
                    if (c.back != null) {
                        Text(
                            if (showBack) "tap to hide answer" else "tap to see answer",
                            fontSize = 14.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { showBack = false; vm.markWrong() },
                modifier = Modifier.weight(1f).height(64.dp),
                enabled = card != null,
            ) { Text("✗ Not yet", fontSize = 20.sp) }
            Button(
                onClick = { showBack = false; vm.markCorrect() },
                modifier = Modifier.weight(1f).height(64.dp),
                enabled = card != null,
            ) { Text("✓ Got it", fontSize = 20.sp) }
        }
    }
}

@Composable
private fun ParentScreen(vm: AppViewModel, state: AppState, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("← Back", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            Text("Parent Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text("Mastery threshold: ${state.threshold}", fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
        Slider(
            value = state.threshold.toFloat(),
            onValueChange = { vm.setThreshold(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
        )
        LazyVerticalGrid(columns = GridCells.Fixed(1), modifier = Modifier.weight(1f)) {
            items(state.deckStatuses, key = { it.deck.id }) { status ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(status.deck.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${status.masteredCount}/${status.total} mastered" +
                                if (!status.unlocked) " · locked" else "",
                            fontSize = 13.sp,
                            color = Color.Gray,
                        )
                    }
                    TextButton(onClick = { vm.resetDeck(status.deck.id) }) { Text("Reset") }
                    Switch(
                        checked = status.unlocked,
                        onCheckedChange = { vm.toggleForceUnlock(status.deck.id) },
                        enabled = status.deck.id in state.forceUnlocked ||
                            !status.unlocked || status.deck.id in state.forceUnlocked,
                    )
                }
            }
        }
    }
}
```

Note on the ParentScreen `Switch`: its `enabled` expression simplifies to "enabled when force-unlocked or naturally locked" — a naturally-unlocked deck's switch is disabled (nothing to toggle). Implementer may simplify to `enabled = status.deck.id in state.forceUnlocked || !status.unlocked`.

- [ ] **Step 2: Rewrite `MainActivity.kt`** (full replacement)

```kotlin
package us.jmresearch.abcflashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import us.jmresearch.abcflashcards.ui.App
import us.jmresearch.abcflashcards.ui.AppViewModel

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels { AppViewModel.factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { App(vm) }
            }
        }
    }
}
```

- [ ] **Step 3: Delete dead resources**

```bash
git rm app/src/main/res/layout/activity_main.xml app/src/main/res/navigation/nav_graph.xml
```

Check `app/src/main/res/values/strings.xml` — keep `app_name`, delete any `nav_*`/fragment strings if present.

- [ ] **Step 4: Verify build + all unit tests**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add -A app/src/main
git commit -m "feat: Compose UI with home grid, deck cards, and parent screen"
```

---

### Task 8: Instrumented smoke test + final verification

**Files:**
- Modify: `app/src/androidTest/java/us/jmresearch/abcflashcards/ExampleInstrumentedTest.kt` → rename content to smoke test (keep file or create `AppSmokeTest.kt` and delete example)
- Delete: `app/src/test/java/us/jmresearch/abcflashcards/ExampleUnitTest.kt`

**Interfaces:**
- Consumes: full app.

- [ ] **Step 1: Replace example instrumented test** with `AppSmokeTest.kt`:

```kotlin
package us.jmresearch.abcflashcards

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsSubjectsAndDeckOpens() {
        rule.onNodeWithText("Let's Learn!").assertIsDisplayed()
        rule.onNodeWithText("Letters").assertIsDisplayed()
        rule.onNodeWithText("Math").assertIsDisplayed()
        rule.onNodeWithText("Letters 1").performClick()
        rule.onNodeWithText("✓ Got it").assertIsDisplayed()
    }
}
```

Delete `ExampleInstrumentedTest.kt` and `ExampleUnitTest.kt` (both reference nothing real).

- [ ] **Step 2: Verify instrumented test compiles**

Run: `./gradlew assembleDebugAndroidTest`
Expected: `BUILD SUCCESSFUL`. (Running it needs a device/emulator — if one is connected, run `./gradlew connectedDebugAndroidTest`; otherwise compile-check is the gate and note this to the user.)

- [ ] **Step 3: Full final verification**

Run: `./gradlew clean assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, every unit test green.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test: Compose smoke test, remove template tests"
```
