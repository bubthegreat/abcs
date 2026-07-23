package us.jmresearch.abcflashcards.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    Subject.LETTERS to Color(0xFF7E57C2),
    Subject.WORDS to Color(0xFF26A69A),
    Subject.PHRASES to Color(0xFFEF6C00),
    Subject.MATH to Color(0xFF42A5F5),
)

private data class TabSpec(val subject: Subject?, val title: String, val emoji: String)

private val tabs = listOf(
    TabSpec(Subject.LETTERS, "Letters", "🔤"),
    TabSpec(Subject.WORDS, "Words", "📖"),
    TabSpec(Subject.PHRASES, "Phrases", "💬"),
    TabSpec(Subject.MATH, "Math", "🔢"),
    TabSpec(null, "Writing", "✍️"),
)

@Composable
fun App(vm: AppViewModel, audio: AudioBox, ink: InkBox) {
    val state by vm.state.collectAsState()
    var screen by remember { mutableStateOf("home") } // "home" | "deck" | "parent"

    if (state.kidMode) {
        KidMode(vm = vm, state = state, audio = audio, ink = ink)
        return
    }

    when (screen) {
        "home" -> HomeScreen(
            vm = vm,
            state = state,
            audio = audio,
            ink = ink,
            onDeckTap = { deckId -> vm.openDeck(deckId); screen = "deck" },
            onParentOpen = { screen = "parent" },
        )
        "deck" -> {
            BackHandler { vm.closeDeck(); screen = "home" }
            DeckScreen(vm = vm, state = state, audio = audio, onClose = { vm.closeDeck(); screen = "home" })
        }
        "parent" -> {
            BackHandler { screen = "home" }
            ParentScreen(vm = vm, state = state, audio = audio, onClose = { screen = "home" })
        }
    }
}

@Composable
private fun KidMode(vm: AppViewModel, state: AppState, audio: AudioBox, ink: InkBox) {
    val openDeckId by vm.openDeckId.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        PinDialog(
            title = "Parents only!",
            onSubmit = { pin ->
                if (vm.exitKidMode(pin)) showExitDialog = false
                vm.exitKidMode(pin)
            },
            onDismiss = { showExitDialog = false },
        )
    }

    if (openDeckId == null) {
        BackHandler { /* stay in kid mode; exit only via PIN */ }
        KidHome(
            vm = vm,
            state = state,
            audio = audio,
            ink = ink,
            onDeckTap = { vm.openDeck(it) },
            onExitTap = { showExitDialog = true },
        )
    } else {
        BackHandler { vm.closeDeck() }
        QuizScreen(vm = vm, state = state, audio = audio, onClose = { vm.closeDeck() })
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
                Text("Needed to leave kid mode later.", fontSize = 14.sp)
                OutlinedTextField(
                    value = entry,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) entry = it },
                    label = { Text("4-digit PIN") },
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
private fun StarBar(state: AppState, onExitTap: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text("⭐ ${state.starBank}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(horizontal = 8.dp))
        LinearProgressIndicator(
            progress = { state.starProgress / CORRECTS_PER_STAR.toFloat() },
            modifier = Modifier.weight(1f).height(10.dp),
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text("${state.starProgress}/$CORRECTS_PER_STAR", fontSize = 14.sp, color = Color.Gray)
        if (onExitTap != null) {
            TextButton(onClick = onExitTap) { Text("👨‍👩‍👧", fontSize = 20.sp) }
        }
    }
}

@Composable
private fun KidHome(
    vm: AppViewModel,
    state: AppState,
    audio: AudioBox,
    ink: InkBox,
    onDeckTap: (String) -> Unit,
    onExitTap: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, spec ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(spec.emoji, fontSize = 24.sp) },
                        label = { Text(spec.title, fontSize = 14.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StarBar(state, onExitTap)
            val spec = tabs[tab]
            val subject = spec.subject
            if (subject == null) {
                WritingScreen(audio = audio, ink = ink, onStoryFinished = { vm.awardStoryStar() })
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
                            onDeckTap,
                            isHomework = status.deck.id in state.homework,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizScreen(vm: AppViewModel, state: AppState, audio: AudioBox, onClose: () -> Unit) {
    val quiz by vm.currentQuiz.collectAsState()
    val openDeckId by vm.openDeckId.collectAsState()
    val reviewMode by vm.reviewMode.collectAsState()
    var wrongChoices by remember { mutableStateOf(setOf<String>()) }
    var flashCorrect by remember { mutableStateOf(false) }

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

    val q = quiz
    val quizNonce by vm.quizNonce.collectAsState()

    // Runs at screen level so it survives the answer buttons leaving composition.
    androidx.compose.runtime.LaunchedEffect(flashCorrect) {
        if (flashCorrect) {
            kotlinx.coroutines.delay(900)
            vm.quizCorrect()
        }
    }

    androidx.compose.runtime.LaunchedEffect(quizNonce) {
        wrongChoices = emptySet()
        flashCorrect = false
        q?.let {
            audio.play(
                us.jmresearch.abcflashcards.data.recordingKeyFor(it.item.id),
                it.spokenPrompt,
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onClose) { Text("← Back", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            Text("⭐ ${state.starBank}  ·  ${state.starProgress}/$CORRECTS_PER_STAR", fontSize = 16.sp)
        }
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                q == null -> Text("All Done! 🎉", fontSize = 40.sp, fontWeight = FontWeight.Bold)
                flashCorrect -> Text("🎉", fontSize = 96.sp)
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
        if (q != null && !flashCorrect) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                q.choices.forEach { choice ->
                    val disabled = choice in wrongChoices
                    Button(
                        onClick = {
                            if (choice == q.answer) {
                                flashCorrect = true
                                audio.play("praise", listOf("Great job!", "You got it!", "Awesome!").random())
                            } else {
                                wrongChoices = wrongChoices + choice
                                vm.quizWrong()
                                audio.play(
                                    us.jmresearch.abcflashcards.data.recordingKeyFor(q.item.id),
                                    q.spokenPrompt,
                                )
                            }
                        },
                        enabled = !disabled,
                        modifier = Modifier.weight(1f).height(96.dp),
                    ) {
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

@Composable
private fun HomeScreen(
    vm: AppViewModel,
    state: AppState,
    audio: AudioBox,
    ink: InkBox,
    onDeckTap: (String) -> Unit,
    onParentOpen: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var showProfiles by remember { mutableStateOf(false) }
    var showSetPin by remember { mutableStateOf(false) }
    val activeProfile = state.profiles.firstOrNull { it.id == state.activeProfileId }

    if (showProfiles) {
        ProfileDialog(vm, state, onDismiss = { showProfiles = false })
    }

    if (showSetPin) {
        SetPinDialog(
            onSet = { pin -> vm.setPin(pin); vm.enterKidMode(); showSetPin = false },
            onDismiss = { showSetPin = false },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, spec ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(spec.emoji, fontSize = 24.sp) },
                        label = { Text(spec.title, fontSize = 14.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                AssistChip(
                    onClick = { showProfiles = true },
                    label = { Text("👦 ${activeProfile?.name ?: "Kid 1"}", fontSize = 16.sp) },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Let's Learn!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { if (state.parentPin == null) showSetPin = true else vm.enterKidMode() },
                ) { Text("▶ Play!", fontSize = 16.sp) }
                TextButton(onClick = onParentOpen) { Text("⚙️", fontSize = 22.sp) }
            }
            val spec = tabs[tab]
            val subject = spec.subject
            if (subject == null) {
                WritingScreen(audio = audio, ink = ink, onStoryFinished = {})
            } else {
                val decks = state.deckStatuses.filter { it.deck.subject == subject }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(decks, key = { it.deck.id }) { status ->
                        DeckTile(status, subjectColors.getValue(subject), onDeckTap)
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
    val enabled = status.unlocked
    val complete = status.masteredCount == status.total
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(104.dp)
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
                    text = (if (isHomework) "📌 " else "") + when {
                        !enabled -> "🔒 ${status.deck.title}"
                        complete -> "⭐ ${status.deck.title}"
                        else -> status.deck.title
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${status.masteredCount}/${status.total}",
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onClose) { Text("← Back", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            if (status != null) {
                Text("${status.masteredCount}/${status.total} ⭐", fontSize = 16.sp)
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
private fun ProfileDialog(vm: AppViewModel, state: AppState, onDismiss: () -> Unit) {
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
        },
    )
}

@Composable
private fun ParentScreen(vm: AppViewModel, state: AppState, audio: AudioBox, onClose: () -> Unit) {
    var resetTarget by remember { mutableStateOf<DeckStatus?>(null) }
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

    resetTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { resetTarget = null },
            title = { Text("Reset ${target.deck.title}?") },
            text = { Text("All progress in this deck will be erased for the current kid. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.resetDeck(target.deck.id); resetTarget = null }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { resetTarget = null }) { Text("Cancel") }
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("← Back", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            Text("Parent Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                val activeName = state.profiles.firstOrNull { it.id == state.activeProfileId }?.name ?: ""
                Text(
                    "Settings for: $activeName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(
                        "⭐ Stars: ${state.starBank} (${state.starProgress}/$CORRECTS_PER_STAR to next)",
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { vm.redeemStars(1) },
                        enabled = state.starBank > 0,
                    ) { Text("Redeem 1") }
                }
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
            }
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
                    val assigned = status.deck.id in state.homework
                    TextButton(onClick = { vm.toggleHomework(status.deck.id) }) {
                        Text(
                            if (assigned) "📌 HW" else "📌",
                            color = if (assigned) MaterialTheme.colorScheme.primary else Color.LightGray,
                        )
                    }
                    TextButton(onClick = { resetTarget = status }) { Text("Reset") }
                    when {
                        status.deck.id in state.forceUnlocked ->
                            TextButton(onClick = { vm.toggleForceUnlock(status.deck.id) }) { Text("Re-lock") }
                        !status.unlocked ->
                            TextButton(onClick = { vm.toggleForceUnlock(status.deck.id) }) { Text("Unlock") }
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
                        Text("5-sentence story = 1 star", fontSize = 13.sp, color = Color.Gray)
                    }
                    val assigned = WRITING_HOMEWORK_ID in state.homework
                    TextButton(onClick = { vm.toggleHomework(WRITING_HOMEWORK_ID) }) {
                        Text(
                            if (assigned) "📌 HW" else "📌",
                            color = if (assigned) MaterialTheme.colorScheme.primary else Color.LightGray,
                        )
                    }
                }
                Text(
                    "📌 = homework. When any 📌 is set, stars only come from pinned activities.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
            }
            item {
                LetterRecordings(audio)
            }
            item {
                Text("Kids", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
            }
            items(state.profiles, key = { it.id }) { profile ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Text(
                        if (profile.id == state.activeProfileId) "✅ ${profile.name}" else profile.name,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { renameTarget = profile }) { Text("Rename") }
                    if (state.profiles.size > 1) {
                        TextButton(onClick = { deleteTarget = profile }) { Text("Remove") }
                    }
                }
            }
        }
    }
}
