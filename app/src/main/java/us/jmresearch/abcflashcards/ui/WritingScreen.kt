package us.jmresearch.abcflashcards.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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
    var preview by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf<String?>(null) }
    var stylusOnly by remember { mutableStateOf(true) }
    var recognizing by remember { mutableStateOf(false) }
    var storyTold by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    val strokes = remember { mutableStateListOf<DrawnStroke>() }

    val storyDone = isStoryComplete(sentences)

    fun clearCanvas() {
        strokes.clear()
        preview = null
    }

    suspend fun recognizeCanvas(): String? {
        val builder = Ink.builder()
        strokes.forEach { s ->
            val sb = Ink.Stroke.builder()
            s.points.forEachIndexed { i, p ->
                sb.addPoint(Ink.Point.create(p.x, p.y, s.times.getOrElse(i) { 0L }))
            }
            builder.addStroke(sb.build())
        }
        return ink.recognize(builder.build(), canvasSize.x, canvasSize.y)?.lowercase()
    }

    fun hearIt() {
        if (strokes.isEmpty() || recognizing) return
        recognizing = true
        scope.launch {
            val text = recognizeCanvas()
            if (text != null) {
                preview = text
                warning = null
                audio.play("writing_preview", text)
            } else {
                warning = "Couldn't read that — try again!"
            }
            recognizing = false
        }
    }

    fun sentenceDone() {
        if (strokes.isEmpty() || recognizing) return
        recognizing = true
        scope.launch {
            val text = recognizeCanvas()
            when {
                text == null -> warning = "Couldn't read that — try again!"
                !isValidSentence(text) ->
                    warning = "A sentence needs at least $WORDS_PER_SENTENCE words! (I read: \"$text\" — ${wordCount(text)})"
                else -> {
                    sentences.add(text)
                    warning = null
                    clearCanvas()
                }
            }
            recognizing = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✍️ My Story", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "${sentences.size}/$SENTENCES_PER_STORY sentences",
                fontSize = 14.sp,
                color = Color.Gray,
            )
        }

        sentences.forEachIndexed { i, s ->
            Text("${i + 1}. $s", fontSize = 16.sp, modifier = Modifier.padding(vertical = 1.dp))
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
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 8.dp),
            ) { Text("📖 Tell my story!", fontSize = 22.sp) }
            TextButton(onClick = { sentences.clear(); storyTold = false; clearCanvas() }) {
                Text("Start a new story")
            }
            return@Column
        }

        preview?.let {
            Text("I read: \"$it\"", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        warning?.let {
            Text(it, color = Color(0xFFD32F2F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (!modelReady) {
            Text(
                "✍️ Pen writing is downloading… one moment!",
                fontSize = 15.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 12.dp),
            )
            return@Column
        }

        // Controls ABOVE the paper so a resting palm can't hit them.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Button(
                onClick = { hearIt() },
                enabled = strokes.isNotEmpty() && !recognizing,
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text(if (recognizing) "…" else "🔊 Hear it", fontSize = 16.sp) }
            Button(
                onClick = { sentenceDone() },
                enabled = strokes.isNotEmpty() && !recognizing,
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text("✅ Sentence done", fontSize = 16.sp) }
            TextButton(onClick = { clearCanvas() }) { Text("Clear") }
            TextButton(onClick = { stylusOnly = !stylusOnly }) {
                Text(if (stylusOnly) "🖊️ Pen" else "👆 Finger", fontSize = 14.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFFFFDF5), RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(stylusOnly) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            // Palm rejection: in pen mode only stylus input draws.
                            if (stylusOnly && down.type != PointerType.Stylus) return@awaitEachGesture
                            down.consume()
                            val stroke = DrawnStroke()
                            stroke.points.add(down.position)
                            stroke.times.add(System.currentTimeMillis())
                            strokes.add(stroke)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.positionChanged()) {
                                    stroke.points.add(change.position)
                                    stroke.times.add(System.currentTimeMillis())
                                    change.consume()
                                }
                                if (!change.pressed) break
                            }
                        }
                    },
            ) {
                canvasSize = Offset(size.width, size.height)
                // Kid handwriting-guide paper: ruled rows with solid blue top/base
                // lines and a dashed pink midline.
                val rows = 3
                val rowGap = 28f
                val rowHeight = (size.height - rowGap * (rows + 1)) / rows
                val blue = Color(0xFF90CAF9)
                val pink = Color(0xFFF48FB1)
                val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
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
                    "Sentence ${sentences.size + 1}: write it on the lines ✏️",
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
