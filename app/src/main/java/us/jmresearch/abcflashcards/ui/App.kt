package us.jmresearch.abcflashcards.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
    var screen by remember { mutableStateOf("home") } // "home" | "deck" | "parent"

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
        item(span = { GridItemSpan(2) }) {
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
            item(span = { GridItemSpan(2) }) {
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
                        enabled = status.deck.id in state.forceUnlocked || !status.unlocked,
                    )
                }
            }
        }
    }
}
