package us.jmresearch.abcflashcards.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import kotlinx.coroutines.launch
import us.jmresearch.abcflashcards.data.Subject

private val subjectColors = mapOf(
    Subject.COLORS to Color(0xFFEC407A),
    Subject.LETTERS to Color(0xFF7E57C2),
    Subject.WORDS to Color(0xFF26A69A),
    Subject.PHRASES to Color(0xFFEF6C00),
    Subject.LANGUAGE to Color(0xFF5C6BC0),
    Subject.MATH to Color(0xFF42A5F5),
)

private val rainbowColors = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFE53935),
)

/** Rounded-rect ring whose rainbow sweep spins while the shape stays put. */
private fun Modifier.rainbowRing(cornerRadius: androidx.compose.ui.unit.Dp, strokeWidth: androidx.compose.ui.unit.Dp, angle: Float): Modifier =
    drawWithContent {
        drawContent()
        val strokePx = strokeWidth.toPx()
        val cornerPx = cornerRadius.toPx()
        val ring = androidx.compose.ui.graphics.Path().apply {
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    0f, 0f, size.width, size.height,
                    androidx.compose.ui.geometry.CornerRadius(cornerPx),
                ),
            )
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    strokePx, strokePx, size.width - strokePx, size.height - strokePx,
                    androidx.compose.ui.geometry.CornerRadius((cornerPx - strokePx).coerceAtLeast(0f)),
                ),
            )
        }
        clipPath(ring) {
            rotate(angle) {
                val big = maxOf(size.width, size.height) * 2f
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(rainbowColors),
                    topLeft = Offset(size.width / 2 - big / 2, size.height / 2 - big / 2),
                    size = androidx.compose.ui.geometry.Size(big, big),
                )
            }
        }
    }

/** One shared spin angle for every rainbow ring on screen. */
@Composable
private fun rememberRainbowAngle(): Float {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "rainbow")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "rainbowAngle",
    )
    return angle
}

private data class Firework(
    val x0: Float,      // launch x as fraction of width
    val drift: Float,   // horizontal drift over the flight
    val height: Float,  // apex as fraction of height
    val emoji: String,
)

private data class TabSpec(val subject: Subject?, val title: String, val emoji: String)

private val tabs = listOf(
    TabSpec(Subject.COLORS, "Colors", "🎨"),
    TabSpec(Subject.LETTERS, "Letters", "🔤"),
    TabSpec(Subject.WORDS, "Words", "📖"),
    TabSpec(Subject.PHRASES, "Phrases", "💬"),
    TabSpec(Subject.LANGUAGE, "Language", "📚"),
    TabSpec(Subject.MATH, "Math", "🔢"),
    TabSpec(null, "Writing", "✍️"),
)

@Composable
fun App(vm: AppViewModel, audio: AudioBox, ink: InkBox, sound: SoundBox) {
    val state by vm.state.collectAsState()

    // Music starts once; both volumes track the kid-adjustable settings live.
    androidx.compose.runtime.LaunchedEffect(Unit) { sound.startMusic() }
    androidx.compose.runtime.LaunchedEffect(state.musicVolume) { sound.setMusicVolume(state.musicVolume) }
    androidx.compose.runtime.LaunchedEffect(state.sfxVolume) { sound.setSfxVolume(state.sfxVolume) }
    var screen by remember { mutableStateOf("home") } // "home" | "cards" | "quiz" | "parent"
    var showStarBurst by remember { mutableStateOf(false) }
    var burstAmount by remember { mutableStateOf(1) }
    var lastBank by remember { mutableStateOf(-1) }

    // Star celebration whenever the bank grows, whatever screen we're on.
    // Baseline updates FIRST: if this effect restarts mid-celebration, the next
    // burst must show only the new delta, not the accumulated one.
    androidx.compose.runtime.LaunchedEffect(state.starBank) {
        val previous = lastBank
        lastBank = state.starBank
        if (previous >= 0 && state.starBank > previous) {
            burstAmount = state.starBank - previous
            showStarBurst = true
            sound.fanfare()
            audio.play(
                "star_earned",
                if (burstAmount == 1) "You earned a star!" else "You earned $burstAmount stars!",
            )
            kotlinx.coroutines.delay(2200)
            showStarBurst = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            "home" -> HomeScreen(
                vm = vm,
                state = state,
                audio = audio,
                ink = ink,
                sound = sound,
                onDeckTap = { deckId, quiz ->
                    vm.openDeck(deckId)
                    screen = if (quiz) "quiz" else "cards"
                },
                onParentOpen = { screen = "parent" },
            )
            "cards" -> {
                BackHandler { vm.closeDeck(); screen = "home" }
                DeckScreen(vm = vm, state = state, audio = audio, onClose = { vm.closeDeck(); screen = "home" })
            }
            "quiz" -> {
                BackHandler { vm.closeDeck(); screen = "home" }
                QuizScreen(vm = vm, state = state, audio = audio, sound = sound, onClose = { vm.closeDeck(); screen = "home" })
            }
            "parent" -> {
                BackHandler { screen = "home" }
                ParentScreen(vm = vm, state = state, audio = audio, onClose = { screen = "home" })
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showStarBurst,
            enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0xEEFFF8E1), RoundedCornerShape(24.dp))
                    .padding(40.dp),
            ) {
                Text("⭐", fontSize = 110.sp)
                Text(
                    if (burstAmount == 1) "You earned a star!" else "+$burstAmount stars!",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF57F17),
                )
            }
        }
    }
}


@Composable
private fun PinDialog(title: String, onSubmit: (String) -> Boolean, onDismiss: () -> Unit) {
    var entry by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = entry,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) entry = it },
                    label = { Text("4-digit PIN") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { if (!onSubmit(entry)) { wrong = true; entry = "" } },
                    ),
                )
                if (wrong) Text("Wrong PIN", color = Color(0xFFD32F2F))
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSubmit(entry)) { wrong = true; entry = "" } }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SetPinDialog(onSet: (String) -> Unit, onDismiss: () -> Unit) {
    var entry by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a parent PIN") },
        text = {
            Column {
                Text("Locks the parent settings screen.", fontSize = 14.sp)
                OutlinedTextField(
                    value = entry,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) entry = it },
                    label = { Text("4-digit PIN") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { if (entry.length == 4) onSet(entry) },
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (entry.length == 4) onSet(entry) },
            ) { Text("Save & start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StarBar(state: AppState, onProfileTap: () -> Unit, onParentTap: () -> Unit, onSoundTap: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val name = state.profiles.firstOrNull { it.id == state.activeProfileId }?.name ?: ""
        AssistChip(
            onClick = onProfileTap,
            label = { Text("👦 $name", fontSize = 16.sp) },
        )
        Spacer(Modifier.padding(horizontal = 6.dp))
        Text("⭐ ${state.starBank}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(horizontal = 8.dp))
        LinearProgressIndicator(
            progress = { state.starProgress / CORRECTS_PER_STAR.toFloat() },
            modifier = Modifier.weight(1f).height(10.dp),
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(
            "${state.starProgress}/$CORRECTS_PER_STAR answers to next ⭐",
            fontSize = 13.sp,
            color = Color.Gray,
        )
        TextButton(onClick = onSoundTap) { Text("🎵", fontSize = 22.sp) }
        TextButton(onClick = onParentTap) { Text("⚙️", fontSize = 22.sp) }
    }
}

/** Kid-accessible sound settings — deliberately outside the PIN gate. */
@Composable
private fun SoundDialog(vm: AppViewModel, state: AppState, sound: SoundBox, onDismiss: () -> Unit) {
    val songName by sound.currentSong.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("🎵 Sounds") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Music — $songName", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { sound.nextSong() }) { Text("⏭ Next song") }
                }
                Slider(
                    value = state.musicVolume,
                    onValueChange = { vm.setMusicVolume(it) },
                )
                Text("Sound effects", fontSize = 16.sp)
                Slider(
                    value = state.sfxVolume,
                    onValueChange = { vm.setSfxVolume(it) },
                )
            }
        },
    )
}

@Composable
private fun QuizScreen(vm: AppViewModel, state: AppState, audio: AudioBox, sound: SoundBox, onClose: () -> Unit) {
    val quiz by vm.currentQuiz.collectAsState()
    val openDeckId by vm.openDeckId.collectAsState()
    val reviewMode by vm.reviewMode.collectAsState()
    var wrongPicked by remember { mutableStateOf<String?>(null) }
    var flashCorrect by remember { mutableStateOf(false) }

    val status = state.deckStatuses.firstOrNull { it.deck.id == openDeckId }
    val complete = status != null && status.quizMasteredCount == status.total && status.total > 0

    if (complete && !reviewMode) {
        CelebrationScreen(
            deckTitle = status!!.deck.title,
            onKeepPracticing = { vm.keepPracticing() },
            onClose = onClose,
        )
        return
    }

    val q = quiz
    val quizNonce by vm.quizNonce.collectAsState()
    // Firework emojis: launch from the bottom, arc up, fall off screen.
    var confetti by remember { mutableStateOf<List<Firework>>(emptyList()) }
    val burst = remember { androidx.compose.animation.core.Animatable(2f) }

    // Runs at screen level so it survives the answer buttons leaving composition.
    androidx.compose.runtime.LaunchedEffect(flashCorrect) {
        if (flashCorrect) {
            confetti = List(8) {
                Firework(
                    x0 = 0.05f + kotlin.random.Random.nextFloat() * 0.8f,
                    drift = (kotlin.random.Random.nextFloat() - 0.5f) * 0.4f,
                    height = 0.45f + kotlin.random.Random.nextFloat() * 0.45f,
                    emoji = listOf("🎉", "⭐", "🎊", "✨").random(),
                )
            }
            launch {
                burst.snapTo(0f)
                burst.animateTo(
                    1.4f,
                    androidx.compose.animation.core.tween(1600, easing = androidx.compose.animation.core.LinearEasing),
                )
            }
            kotlinx.coroutines.delay(900)
            vm.quizCorrect()
        }
    }

    // Wrong answer: correct one is shown green; move on after the kid has seen it.
    androidx.compose.runtime.LaunchedEffect(wrongPicked) {
        if (wrongPicked != null) {
            kotlinx.coroutines.delay(2000)
            vm.quizWrongAdvance()
        }
    }

    androidx.compose.runtime.LaunchedEffect(quizNonce) {
        wrongPicked = null
        flashCorrect = false
        q?.let {
            audio.play(
                us.jmresearch.abcflashcards.data.recordingKeyFor(it.item.id),
                it.spokenPrompt,
            )
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxW = maxWidth
        val maxH = maxHeight
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var showReset by remember { mutableStateOf(false) }
        if (showReset && status != null) {
            AlertDialog(
                onDismissRequest = { showReset = false },
                title = { Text("Reset ${status.deck.title} quiz?") },
                text = { Text("Quiz progress for this deck goes back to zero. Flashcard progress and earned stars are not touched.") },
                confirmButton = {
                    TextButton(onClick = { vm.resetDeckQuiz(status.deck.id); showReset = false }) { Text("Reset") }
                },
                dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onClose) { Text("← Back", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            Text("⭐ ${state.starBank}  ·  ${state.starProgress}/$CORRECTS_PER_STAR", fontSize = 16.sp)
            TextButton(onClick = { showReset = true }) { Text("↺ Reset", fontSize = 15.sp) }
        }
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                q == null -> Text("All Done! 🎉", fontSize = 40.sp, fontWeight = FontWeight.Bold)
                q.visualPrompt != null -> Text(
                    q.visualPrompt!!,
                    fontSize = if (q.visualPrompt!!.length > 8) 44.sp else 80.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 88.sp,
                )
                else -> Button(
                    onClick = {
                        audio.play(
                            us.jmresearch.abcflashcards.data.recordingKeyFor(q.item.id),
                            q.spokenPrompt,
                        )
                    },
                    modifier = Modifier.size(140.dp),
                    shape = CircleShape,
                ) { Text("🔊", fontSize = 48.sp) }
            }
        }
        if (q != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                q.choices.forEach { choice ->
                    val isAnswer = choice == q.answer
                    val revealColor = when {
                        wrongPicked == null -> null
                        isAnswer -> Color(0xFF43A047)
                        choice == wrongPicked -> Color(0xFFD32F2F)
                        else -> null
                    }
                    Button(
                        onClick = {
                            if (wrongPicked != null || flashCorrect) return@Button
                            if (isAnswer) {
                                flashCorrect = true
                                sound.correct()
                                audio.play("praise", listOf("Great job!", "You got it!", "Awesome!").random())
                            } else {
                                wrongPicked = choice
                                sound.wrong()
                                audio.play("reveal", "The answer is ${q.answer}")
                            }
                        },
                        enabled = wrongPicked == null || revealColor != null,
                        colors = when {
                            revealColor != null -> androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = revealColor,
                                disabledContainerColor = revealColor,
                                disabledContentColor = Color.White,
                            )
                            isShapeSpec(choice) -> androidx.compose.material3.ButtonDefaults.buttonColors(
                                // Light canvas behind drawn shapes so their color reads true.
                                containerColor = Color(0xFFFFFDF5),
                                disabledContainerColor = Color(0xFFFFFDF5),
                            )
                            else -> androidx.compose.material3.ButtonDefaults.buttonColors()
                        },
                        border = if (isShapeSpec(choice) && revealColor == null) {
                            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFBDBDBD))
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f).height(96.dp),
                    ) {
                        if (isShapeSpec(choice)) {
                            ShapeGlyph(choice, modifier = Modifier.size(64.dp))
                        } else {
                            Text(
                                choice,
                                fontSize = if (choice.length > 8) 20.sp else 32.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }

    // Firework emojis fly over everything without hiding the screen.
    if (burst.value < 1.4f) {
        val p = burst.value
        confetti.forEach { f ->
            val yFrac = 1f - f.height * (1f - (2 * p - 1f) * (2 * p - 1f))
            Text(
                f.emoji,
                fontSize = 48.sp,
                modifier = Modifier.offset(x = maxW * (f.x0 + f.drift * p), y = maxH * yFrac),
            )
        }
    }
    }
}

@Composable
private fun HomeScreen(
    vm: AppViewModel,
    state: AppState,
    audio: AudioBox,
    ink: InkBox,
    sound: SoundBox,
    onDeckTap: (String, Boolean) -> Unit,
    onParentOpen: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var showProfiles by remember { mutableStateOf(false) }
    var showSetPin by remember { mutableStateOf(false) }
    var showParentPin by remember { mutableStateOf(false) }
    var showSound by remember { mutableStateOf(false) }
    var chooseDeck by remember { mutableStateOf<DeckStatus?>(null) }

    if (showSound) {
        SoundDialog(vm, state, sound, onDismiss = { showSound = false })
    }

    chooseDeck?.let { chosen ->
        AlertDialog(
            onDismissRequest = { chooseDeck = null },
            title = { Text(chosen.deck.title, textAlign = TextAlign.Center) },
            text = {
                Column {
                    Button(
                        onClick = { onDeckTap(chosen.deck.id, true); chooseDeck = null },
                        enabled = chosen.quizUnlocked,
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                    ) {
                        val hw = chosen.deck.id in state.homework
                        Text(
                            when {
                                !chosen.quizUnlocked -> "🔒 Quiz — master earlier decks first"
                                hw -> "🎯 Quiz — homework! Earn ⭐ (${chosen.quizMasteredCount}/${chosen.total})"
                                else -> "🎯 Quiz — tap answers (${chosen.quizMasteredCount}/${chosen.total})"
                            },
                            fontSize = 18.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onDeckTap(chosen.deck.id, false); chooseDeck = null },
                        enabled = chosen.unlocked,
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                    ) {
                        Text(
                            if (chosen.unlocked) "🃏 Flashcards — practice together (${chosen.masteredCount}/${chosen.total})"
                            else "🔒 Flashcards — master earlier decks first",
                            fontSize = 18.sp,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooseDeck = null }) { Text("Cancel") } },
        )
    }

    if (showProfiles) {
        ProfileDialog(vm, state, onDismiss = { showProfiles = false }, allowAdd = false)
    }

    if (showSetPin) {
        SetPinDialog(
            onSet = { pin -> vm.setPin(pin); showSetPin = false; onParentOpen() },
            onDismiss = { showSetPin = false },
        )
    }

    if (showParentPin) {
        PinDialog(
            title = "Parents only!",
            onSubmit = { pin ->
                val ok = pin == state.parentPin
                if (ok) {
                    showParentPin = false
                    onParentOpen()
                }
                ok
            },
            onDismiss = { showParentPin = false },
        )
    }

    val ringAngle = rememberRainbowAngle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, spec ->
                    val hasHomework = if (spec.subject == null) {
                        WRITING_HOMEWORK_ID in state.homework
                    } else {
                        state.deckStatuses.any { it.deck.subject == spec.subject && it.deck.id in state.homework }
                    }
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(60.dp)
                                    .then(
                                        if (hasHomework) {
                                            Modifier.rainbowRing(cornerRadius = 30.dp, strokeWidth = 7.dp, angle = ringAngle)
                                        } else {
                                            Modifier
                                        },
                                    ),
                            ) { Text(spec.emoji, fontSize = 24.sp) }
                        },
                        label = { Text(if (hasHomework) "🌟 ${spec.title}" else spec.title, fontSize = 14.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StarBar(
                state = state,
                onProfileTap = { showProfiles = true },
                onParentTap = { if (state.parentPin == null) showSetPin = true else showParentPin = true },
                onSoundTap = { showSound = true },
            )
            val spec = tabs[tab]
            val subject = spec.subject
            if (subject == null) {
                if (WRITING_HOMEWORK_ID in state.homework) {
                    Text(
                        "🌟 Homework — stories earn stars!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57F17),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                WritingScreen(vm = vm, audio = audio, ink = ink)
            } else {
                val decks = state.deckStatuses.filter { it.deck.subject == subject }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(decks, key = { it.deck.id }) { status ->
                        DeckTile(
                            status,
                            subjectColors.getValue(subject),
                            onDeckTap = { chooseDeck = status },
                            isHomework = status.deck.id in state.homework,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckTile(
    status: DeckStatus,
    color: Color,
    onDeckTap: (String) -> Unit,
    isHomework: Boolean = false,
) {
    val enabled = status.unlocked || status.quizUnlocked
    val complete = status.masteredCount == status.total
    val ringAngle = if (isHomework) rememberRainbowAngle() else 0f
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(104.dp)
            .then(
                if (isHomework) {
                    Modifier.rainbowRing(cornerRadius = 16.dp, strokeWidth = 6.dp, angle = ringAngle)
                } else {
                    Modifier
                },
            )
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
                    text = (if (isHomework) "🌟 " else "") + when {
                        !enabled -> "🔒 ${status.deck.title}"
                        complete -> "⭐ ${status.deck.title}"
                        else -> status.deck.title
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "🃏 ${status.masteredCount}/${status.total}   🎯 ${status.quizMasteredCount}/${status.total}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                )
                LinearProgressIndicator(
                    progress = { if (status.total == 0) 0f else status.masteredCount.toFloat() / status.total },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(top = 6.dp).fillMaxWidth(0.6f),
                )
            }
        }
    }
}

@Composable
private fun DeckScreen(vm: AppViewModel, state: AppState, audio: AudioBox, onClose: () -> Unit) {
    val card by vm.currentCard.collectAsState()
    val openDeckId by vm.openDeckId.collectAsState()
    val reviewMode by vm.reviewMode.collectAsState()
    var showBack by remember { mutableStateOf(false) }

    val status = state.deckStatuses.firstOrNull { it.deck.id == openDeckId }
    val complete = status != null && status.masteredCount == status.total && status.total > 0

    if (complete && !reviewMode) {
        CelebrationScreen(
            deckTitle = status!!.deck.title,
            onKeepPracticing = { vm.keepPracticing() },
            onClose = onClose,
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var showReset by remember { mutableStateOf(false) }
        if (showReset && status != null) {
            AlertDialog(
                onDismissRequest = { showReset = false },
                title = { Text("Reset ${status.deck.title} flashcards?") },
                text = { Text("Flashcard progress for this deck goes back to zero. Quiz progress is not touched.") },
                confirmButton = {
                    TextButton(onClick = { vm.resetDeckLearning(status.deck.id); showReset = false }) { Text("Reset") }
                },
                dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onClose) { Text("← Back", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            if (status != null) {
                Text("${status.masteredCount}/${status.total} ⭐", fontSize = 16.sp)
                TextButton(onClick = { showReset = true }) { Text("↺ Reset", fontSize = 15.sp) }
            }
        }
        if (status != null) {
            LinearProgressIndicator(
                progress = { status.masteredCount.toFloat() / status.total },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
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
                    if (isShapeSpec(c.front) && !(showBack && c.back != null)) {
                        ShapeGlyph(c.front, modifier = Modifier.size(240.dp))
                    } else {
                        Text(
                            text = if (showBack && c.back != null) c.back!! else c.front,
                            fontSize = if (c.front.length > 8) 44.sp else 96.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 100.sp,
                        )
                    }
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
        val c = card
        if (c != null && status != null) {
            Button(
                onClick = {
                    audio.play(
                        us.jmresearch.abcflashcards.data.recordingKeyFor(c.id),
                        us.jmresearch.abcflashcards.data.utteranceFor(c, status.deck),
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp).height(56.dp),
            ) { Text("🔊 Hear it", fontSize = 18.sp) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                val count = state.progress[c.id]?.correctCount ?: 0
                repeat(state.threshold) { i ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                if (i < count) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                CircleShape,
                            ),
                    )
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
private fun CelebrationScreen(deckTitle: String, onKeepPracticing: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉⭐🎉", fontSize = 64.sp)
        Text(
            "$deckTitle Mastered!",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Text("Amazing job!", fontSize = 24.sp, color = Color.Gray)
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(0.7f).height(64.dp),
        ) { Text("Back to menu", fontSize = 20.sp) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onKeepPracticing) { Text("Keep practicing", fontSize = 18.sp) }
    }
}

/** Daily earn limit: 1..5 or endless (-1). Plus past 5 wraps to ∞. */
@Composable
private fun LimitStepper(limit: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { onChange(if (limit < 0) 5 else (limit - 1).coerceAtLeast(1)) },
            enabled = limit < 0 || limit > 1,
            modifier = Modifier.width(36.dp),
        ) { Text("−", fontSize = 16.sp) }
        Text(if (limit < 0) "∞/day" else "$limit/day", fontSize = 12.sp)
        TextButton(
            onClick = { onChange(if (limit >= 5 || limit < 0) -1 else limit + 1) },
            enabled = limit >= 0,
            modifier = Modifier.width(36.dp),
        ) { Text("+", fontSize = 16.sp) }
    }
}

@Composable
private fun RewardStepper(count: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { onChange(count - 1) },
            enabled = count > 1,
            modifier = Modifier.width(36.dp),
        ) { Text("−", fontSize = 18.sp) }
        Text("$count⭐", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        TextButton(
            onClick = { onChange(count + 1) },
            enabled = count < 10,
            modifier = Modifier.width(36.dp),
        ) { Text("+", fontSize = 18.sp) }
    }
}

@Composable
private fun LetterRecordings(audio: AudioBox) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var recordingLetter by remember { mutableStateOf<Char?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var hasMicPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> hasMicPermission = granted }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("Letter sounds — record your voice", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Say the SOUND, not the letter name. Tap ⏺, read the prompt out loud, tap ⏹.",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        if (!hasMicPermission) {
            Button(onClick = { permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }) {
                Text("Allow microphone to record")
            }
        } else {
            ('a'..'z').forEach { letter ->
                val itemId = "letter_$letter"
                val recorded = remember(refresh) { audio.hasRecording(itemId) }
                val isRecording = recordingLetter == letter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Text(
                        letter.uppercaseChar().toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        us.jmresearch.abcflashcards.data.letterRecordingPrompts[letter] ?: "",
                        fontSize = 14.sp,
                        color = if (isRecording) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (recorded && !isRecording) {
                        TextButton(onClick = { audio.play(itemId, "") }) { Text("▶") }
                        TextButton(onClick = { audio.deleteRecording(itemId); refresh++ }) { Text("🗑") }
                    }
                    TextButton(
                        onClick = {
                            if (isRecording) {
                                audio.stopRecording()
                                recordingLetter = null
                                refresh++
                            } else {
                                audio.stopRecording()
                                recordingLetter = if (audio.startRecording(itemId)) letter else null
                            }
                        },
                    ) { Text(if (isRecording) "⏹ Stop" else "⏺", fontSize = 18.sp) }
                }
            }
        }
    }
}

@Composable
private fun ProfileDialog(
    vm: AppViewModel,
    state: AppState,
    onDismiss: () -> Unit,
    allowAdd: Boolean = true,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Who is learning?") },
        text = {
            Column {
                state.profiles.forEach { profile ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.switchProfile(profile.id); onDismiss() }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            if (profile.id == state.activeProfileId) "✅ ${profile.name}" else "👦 ${profile.name}",
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (allowAdd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("New kid's name") },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    vm.addProfile(newName)
                                    newName = ""
                                    onDismiss()
                                }
                            },
                        ) { Text("Add") }
                    }
                }
            }
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ParentScreen(vm: AppViewModel, state: AppState, audio: AudioBox, onClose: () -> Unit) {
    var deleteTarget by remember { mutableStateOf<us.jmresearch.abcflashcards.data.Profile?>(null) }
    var renameTarget by remember { mutableStateOf<us.jmresearch.abcflashcards.data.Profile?>(null) }
    var showResetAll by remember { mutableStateOf(false) }

    if (showResetAll) {
        val activeName = state.profiles.firstOrNull { it.id == state.activeProfileId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { showResetAll = false },
            title = { Text("Reset ALL progress?") },
            text = { Text("Every deck for $activeName goes back to zero. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.resetAll(); showResetAll = false }) { Text("Reset everything") }
            },
            dismissButton = {
                TextButton(onClick = { showResetAll = false }) { Text("Cancel") }
            },
        )
    }

    renameTarget?.let { target ->
        var editedName by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename ${target.name}") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Name") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) vm.renameProfile(target.id, editedName)
                        renameTarget = null
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove ${target.name}?") },
            text = { Text("All of ${target.name}'s progress will be erased. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteProfile(target.id); deleteTarget = null }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }


    var parentTab by remember { mutableStateOf(0) }
    val activeName = state.profiles.firstOrNull { it.id == state.activeProfileId }?.name ?: ""
    var birthdayTarget by remember { mutableStateOf<us.jmresearch.abcflashcards.data.Profile?>(null) }

    birthdayTarget?.let { target ->
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = target.birthdayEpochDay?.let { it * 86_400_000L },
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()

                override fun isSelectableYear(year: Int): Boolean =
                    year <= java.time.LocalDate.now().year
            },
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { birthdayTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            vm.setBirthday(target.id, millis / 86_400_000L)
                        }
                        birthdayTarget = null
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { birthdayTarget = null }) { Text("Cancel") } },
        ) {
            androidx.compose.material3.DatePicker(
                state = pickerState,
                title = {
                    Text(
                        "${target.name}'s birthday",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    )
                },
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("← Back", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            Text("Parent Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Kid selector chips — everything below applies to the selected kid.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            state.profiles.forEach { profile ->
                val selected = profile.id == state.activeProfileId
                AssistChip(
                    onClick = { vm.switchProfile(profile.id) },
                    label = {
                        Text(
                            if (selected) "✅ ${profile.name}" else profile.name,
                            fontSize = 16.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        androidx.compose.material3.TabRow(selectedTabIndex = parentTab) {
            listOf("⭐ Summary", "📚 Learning", "🔧 Setup").forEachIndexed { i, title ->
                androidx.compose.material3.Tab(
                    selected = parentTab == i,
                    onClick = { parentTab = i },
                    text = { Text(title, fontSize = 15.sp) },
                )
            }
        }

        when (parentTab) {
            0 -> LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("⭐ ${state.starBank} stars", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "${state.starProgress}/$CORRECTS_PER_STAR toward the next star",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Button(
                                    onClick = { vm.redeemStars(1) },
                                    enabled = state.starBank > 0,
                                    modifier = Modifier.height(56.dp),
                                ) { Text("🎁 Redeem 1 star", fontSize = 16.sp) }
                                TextButton(
                                    onClick = { vm.resetStarProgress() },
                                    enabled = state.starProgress > 0,
                                ) { Text("Reset ${state.starProgress}/$CORRECTS_PER_STAR progress") }
                            }
                        }
                    }
                    Text(
                        "$activeName" + "'s progress",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(Subject.entries.toList(), key = { "sum_" + it.name }) { subject ->
                    val decks = state.deckStatuses.filter { it.deck.subject == subject }
                    if (decks.isNotEmpty()) {
                        val learn = decks.sumOf { it.masteredCount }
                        val quizzed = decks.sumOf { it.quizMasteredCount }
                        val total = decks.sumOf { it.total }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                tabs.firstOrNull { it.subject == subject }?.let { "${it.emoji} ${it.title}" } ?: subject.name,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text("🃏 $learn/$total   🎯 $quizzed/$total", fontSize = 15.sp, color = Color.Gray)
                        }
                    }
                }
                item {
                    val hw = state.homework
                    Text(
                        if (hw.isEmpty()) "No homework assigned — no stars can be earned."
                        else "Homework: " + hw.joinToString(", ") { id ->
                            if (id == WRITING_HOMEWORK_ID) "Writing" else state.deckStatuses.firstOrNull { it.deck.id == id }?.deck?.title ?: id
                        },
                        fontSize = 14.sp,
                        color = if (hw.isEmpty()) Color(0xFFD32F2F) else Color.Gray,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            1 -> LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text("Mastery threshold: ${state.threshold}", fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
                    Slider(
                        value = state.threshold.toFloat(),
                        onValueChange = { vm.setThreshold(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text("Decks", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showResetAll = true }) { Text("Reset all") }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        Text("Homework", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.width(100.dp))
                        Text("Lock", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
                    }
                }
                items(state.deckStatuses, key = { it.deck.id }) { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(status.deck.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "🃏 ${status.masteredCount}/${status.total}  🎯 ${status.quizMasteredCount}/${status.total}" +
                                    if (!status.unlocked) " · locked" else "",
                                fontSize = 13.sp,
                                color = Color.Gray,
                            )
                        }
                        val assigned = status.deck.id in state.homework
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp),
                        ) {
                            Switch(
                                checked = assigned,
                                onCheckedChange = { vm.toggleHomework(status.deck.id) },
                            )
                            if (assigned) {
                                RewardStepper(
                                    count = state.rewardFor(status.deck.id),
                                    onChange = { vm.setHomeworkReward(status.deck.id, it) },
                                )
                                LimitStepper(
                                    limit = state.limitFor(status.deck.id),
                                    onChange = { vm.setDailyLimit(status.deck.id, it) },
                                )
                                if (!state.canEarn(status.deck.id)) {
                                    Text("✅ done today", fontSize = 12.sp, color = Color(0xFF43A047))
                                }
                            }
                        }
                        Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
                            when {
                                status.deck.id in state.forceUnlocked ->
                                    TextButton(onClick = { vm.toggleForceUnlock(status.deck.id) }) { Text("Re-lock") }
                                !status.unlocked ->
                                    TextButton(onClick = { vm.toggleForceUnlock(status.deck.id) }) { Text("Unlock") }
                                else -> Text("—", color = Color.LightGray)
                            }
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("✍️ Writing stories", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        val assigned = WRITING_HOMEWORK_ID in state.homework
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp),
                        ) {
                            Switch(
                                checked = assigned,
                                onCheckedChange = { vm.toggleHomework(WRITING_HOMEWORK_ID) },
                            )
                            if (assigned) {
                                RewardStepper(
                                    count = state.rewardFor(WRITING_HOMEWORK_ID),
                                    onChange = { vm.setHomeworkReward(WRITING_HOMEWORK_ID, it) },
                                )
                                LimitStepper(
                                    limit = state.limitFor(WRITING_HOMEWORK_ID),
                                    onChange = { vm.setDailyLimit(WRITING_HOMEWORK_ID, it) },
                                )
                                if (!state.canEarn(WRITING_HOMEWORK_ID)) {
                                    Text("✅ done today", fontSize = 12.sp, color = Color(0xFF43A047))
                                }
                            }
                        }
                        Spacer(Modifier.width(90.dp))
                    }
                    Text(
                        "Rewards reset daily. The /day limit caps how many times each homework can pay per day.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                    )
                }
            }

            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text("Kids", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                }
                items(state.profiles, key = { "kid_" + it.id }) { profile ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        val age = us.jmresearch.abcflashcards.data.ageOf(
                            profile,
                            java.time.LocalDate.now().toEpochDay(),
                        )
                        Text(
                            (if (profile.id == state.activeProfileId) "✅ ${profile.name}" else "👦 ${profile.name}") +
                                (age?.let { "  ·  $it yrs" } ?: ""),
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { birthdayTarget = profile }) {
                            Text(if (profile.birthdayEpochDay == null) "🎂 Set birthday" else "🎂")
                        }
                        TextButton(onClick = { renameTarget = profile }) { Text("Rename") }
                        if (state.profiles.size > 1) {
                            TextButton(onClick = { deleteTarget = profile }) { Text("Remove") }
                        }
                    }
                }
                item {
                    var newKidName by remember { mutableStateOf("") }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newKidName,
                            onValueChange = { newKidName = it },
                            label = { Text("New kid's name") },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                if (newKidName.isNotBlank()) {
                                    vm.addProfile(newKidName)
                                    newKidName = ""
                                }
                            },
                        ) { Text("Add") }
                    }
                }
                item {
                    LetterRecordings(audio)
                }
            }
        }
    }
}
