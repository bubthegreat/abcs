package us.jmresearch.abcflashcards.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.launch
import us.jmresearch.abcflashcards.data.SENTENCES_PER_STORY
import us.jmresearch.abcflashcards.data.WORDS_PER_SENTENCE
import us.jmresearch.abcflashcards.data.isStoryComplete
import us.jmresearch.abcflashcards.data.isValidSentence
import us.jmresearch.abcflashcards.data.wordCount

private class DrawnStroke {
    val points = mutableStateListOf<Offset>()
    val times = mutableListOf<Long>()
}

@Composable
fun WritingScreen(audio: AudioBox, ink: InkBox, onStoryFinished: () -> Unit) {
    val modelReady by ink.modelReady.collectAsState()
    val scope = rememberCoroutineScope()

    val sentences = remember { mutableStateListOf<String>() }
    var current by remember { mutableStateOf("") }
    var warning by remember { mutableStateOf<String?>(null) }
    var usePen by remember { mutableStateOf(true) }
    var recognizing by remember { mutableStateOf(false) }
    var storyTold by remember { mutableStateOf(false) }
    val strokes = remember { mutableStateListOf<DrawnStroke>() }

    val storyDone = isStoryComplete(sentences)

    fun clearCanvas() = strokes.clear()

    var canvasSize by remember { mutableStateOf(Offset.Zero) }

    fun addRecognizedText() {
        if (strokes.isEmpty() || recognizing) return
        recognizing = true
        scope.launch {
            val builder = Ink.builder()
            strokes.forEach { s ->
                val sb = Ink.Stroke.builder()
                s.points.forEachIndexed { i, p ->
                    sb.addPoint(Ink.Point.create(p.x, p.y, s.times.getOrElse(i) { 0L }))
                }
                builder.addStroke(sb.build())
            }
            val text = ink.recognize(
                builder.build(),
                areaWidth = canvasSize.x,
                areaHeight = canvasSize.y,
                preContext = current,
            )
            if (text != null) {
                current = (current.trim() + " " + text.lowercase()).trim()
                warning = null
            } else {
                warning = "Couldn't read that — try again!"
            }
            clearCanvas()
            recognizing = false
        }
    }

    fun submitSentence() {
        if (!isValidSentence(current)) {
            warning = "A sentence needs at least $WORDS_PER_SENTENCE words! (has ${wordCount(current)})"
            return
        }
        sentences.add(current.trim())
        current = ""
        warning = null
        clearCanvas()
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("✍️ My Story", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Write $SENTENCES_PER_STORY sentences. Each needs $WORDS_PER_SENTENCE words or more.",
            fontSize = 14.sp,
            color = Color.Gray,
        )

        sentences.forEachIndexed { i, s ->
            Text("${i + 1}. $s", fontSize = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
        }

        if (storyDone) {
            Button(
                onClick = {
                    audio.speakStory(sentences.toList())
                    if (!storyTold) {
                        storyTold = true
                        onStoryFinished()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 8.dp),
            ) { Text("📖 Tell my story!", fontSize = 20.sp) }
            TextButton(onClick = { sentences.clear(); current = ""; storyTold = false; clearCanvas() }) {
                Text("Start a new story")
            }
            return@Column
        }

        Text(
            "Sentence ${sentences.size + 1}:  ${current.ifBlank { "…" }}",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        warning?.let {
            Text(it, color = Color(0xFFD32F2F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (usePen && modelReady) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(Color(0xFFFFFDF5), RoundedCornerShape(12.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { pos ->
                                    val s = DrawnStroke()
                                    s.points.add(pos)
                                    s.times.add(System.currentTimeMillis())
                                    strokes.add(s)
                                },
                                onDrag = { change, _ ->
                                    strokes.lastOrNull()?.let {
                                        it.points.add(change.position)
                                        it.times.add(System.currentTimeMillis())
                                    }
                                },
                            )
                        },
                ) {
                    canvasSize = Offset(size.width, size.height)
                    // Kid handwriting-guide paper: 3 ruled rows, each with a solid
                    // sky-blue top line, dashed pink midline, and solid blue baseline.
                    val rows = 3
                    val rowGap = 24f
                    val rowHeight = (size.height - rowGap * (rows + 1)) / rows
                    val blue = Color(0xFF90CAF9)
                    val pink = Color(0xFFF48FB1)
                    val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
                    repeat(rows) { r ->
                        val top = rowGap + r * (rowHeight + rowGap)
                        val mid = top + rowHeight / 2
                        val base = top + rowHeight
                        drawLine(blue, Offset(24f, top), Offset(size.width - 24f, top), strokeWidth = 3f)
                        drawLine(pink, Offset(24f, mid), Offset(size.width - 24f, mid), strokeWidth = 3f, pathEffect = dash)
                        drawLine(blue, Offset(24f, base), Offset(size.width - 24f, base), strokeWidth = 4f)
                    }
                    strokes.forEach { s ->
                        if (s.points.size > 1) {
                            val path = Path().apply {
                                moveTo(s.points[0].x, s.points[0].y)
                                s.points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, Color(0xFF37474F), style = Stroke(width = 6f))
                        }
                    }
                }
                if (strokes.isEmpty()) {
                    Text(
                        "Write your sentence on the lines ✏️",
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Button(
                    onClick = { addRecognizedText() },
                    enabled = strokes.isNotEmpty() && !recognizing,
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Text(if (recognizing) "…" else "✓ Read my writing", fontSize = 16.sp) }
                TextButton(onClick = { clearCanvas() }) { Text("Clear") }
                TextButton(
                    onClick = {
                        current = current.trim().substringBeforeLast(" ", "").trim()
                    },
                ) { Text("⌫ Word") }
            }
        } else {
            if (usePen && !modelReady) {
                Text(
                    "✍️ Pen writing is downloading… keyboard for now!",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
            }
            LetterKeyboard(
                onKey = { current += it; warning = null },
                onSpace = { if (!current.endsWith(" ")) current += " " },
                onBackspace = { current = current.dropLast(1) },
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Button(
                onClick = { submitSentence() },
                modifier = Modifier.weight(1f).height(56.dp),
            ) { Text("✅ Sentence done", fontSize = 16.sp) }
            TextButton(onClick = { audio.play("current_sentence", current) }, enabled = current.isNotBlank()) {
                Text("🔊 Hear it")
            }
            if (modelReady) {
                TextButton(onClick = { usePen = !usePen }) { Text(if (usePen) "⌨️" else "✍️", fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun LetterKeyboard(onKey: (String) -> Unit, onSpace: () -> Unit, onBackspace: () -> Unit) {
    val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                row.forEach { c ->
                    Button(
                        onClick = { onKey(c.toString()) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) { Text(c.toString(), fontSize = 20.sp) }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        ) {
            Button(onClick = onSpace, modifier = Modifier.weight(3f).height(52.dp)) { Text("space") }
            Button(onClick = onBackspace, modifier = Modifier.weight(1f).height(52.dp)) { Text("⌫") }
        }
    }
}
