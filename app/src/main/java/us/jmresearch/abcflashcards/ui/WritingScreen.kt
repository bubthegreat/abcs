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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import us.jmresearch.abcflashcards.data.sentenceProblem

private class DrawnStroke {
    val points = mutableStateListOf<Offset>()
    val times = mutableListOf<Long>()
}

private const val PAPER_ROWS = 3
private const val ROW_GAP = 28f

/** Which ruled row a stroke belongs to, from its average y. */
private fun rowIndexOf(stroke: DrawnStroke, canvasHeight: Float): Int {
    if (stroke.points.isEmpty()) return 0
    val rowHeight = (canvasHeight - ROW_GAP * (PAPER_ROWS + 1)) / PAPER_ROWS
    val avgY = stroke.points.sumOf { it.y.toDouble() }.toFloat() / stroke.points.size
    return (((avgY - ROW_GAP) / (rowHeight + ROW_GAP)).toInt()).coerceIn(0, PAPER_ROWS - 1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(vm: AppViewModel, audio: AudioBox, ink: InkBox) {
    val modelReady by ink.modelReady.collectAsState()
    val scope = rememberCoroutineScope()

    val sentences by vm.storySentences.collectAsState()
    var preview by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf<String?>(null) }
    var stylusOnly by remember { mutableStateOf(true) }
    var eraserMode by remember { mutableStateOf(false) }
    var recognizing by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    val strokes = remember { mutableStateListOf<DrawnStroke>() }

    val storyDone = isStoryComplete(sentences)

    fun clearCanvas() {
        strokes.clear()
        preview = null
    }

    suspend fun recognizeCanvas(): String? {
        // Recognize each ruled row separately — one long multi-row blob confuses
        // the recognizer and drops words; rows are known geometry, so split there.
        val rowHeight = (canvasSize.y - ROW_GAP * (PAPER_ROWS + 1)) / PAPER_ROWS
        val byRow = strokes.groupBy { rowIndexOf(it, canvasSize.y) }.toSortedMap()
        val parts = mutableListOf<String>()
        for ((_, rowStrokes) in byRow) {
            val builder = Ink.builder()
            rowStrokes.forEach { s ->
                val sb = Ink.Stroke.builder()
                s.points.forEachIndexed { i, p ->
                    sb.addPoint(Ink.Point.create(p.x, p.y, s.times.getOrElse(i) { 0L }))
                }
                builder.addStroke(sb.build())
            }
            val text = ink.recognize(
                builder.build(),
                areaWidth = canvasSize.x,
                areaHeight = rowHeight + ROW_GAP,
                preContext = parts.joinToString(" "),
            )
            if (text != null) parts.add(text)
        }
        return parts.joinToString(" ").trim().takeIf { it.isNotBlank() }
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
            if (text == null) {
                warning = "Couldn't read that — try again!"
            } else {
                val problem = sentenceProblem(text, existing = sentences)
                if (problem != null) {
                    preview = text
                    warning = problem
                } else {
                    vm.addStorySentence(text)
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
                onClick = { audio.speakStory(sentences) },
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 8.dp),
            ) { Text("📖 Tell my story!", fontSize = 22.sp) }
            TextButton(onClick = { vm.clearStory(); clearCanvas() }) {
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

        var showClearConfirm by remember { mutableStateOf(false) }
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Erase the whole page?") },
                text = { Text("All your writing on the page will be gone!") },
                confirmButton = {
                    TextButton(onClick = { clearCanvas(); showClearConfirm = false }) { Text("🗑 Erase it all") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("Keep writing") }
                },
            )
        }

        // Controls ABOVE the paper so a resting palm can't hit them.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
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
            TextButton(
                onClick = { showClearConfirm = true },
                enabled = strokes.isNotEmpty(),
            ) { Text("🗑 Clear page") }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                SegmentedButton(
                    selected = !eraserMode,
                    onClick = { eraserMode = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("✏️ Write") }
                SegmentedButton(
                    selected = eraserMode,
                    onClick = { eraserMode = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("🧹 Erase") }
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                SegmentedButton(
                    selected = stylusOnly,
                    onClick = { stylusOnly = true },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("🖊️ Pen") }
                SegmentedButton(
                    selected = !stylusOnly,
                    onClick = { stylusOnly = false },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("👆 Finger") }
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
                    .pointerInput(stylusOnly, eraserMode) {
                        fun eraseAt(pos: Offset) {
                            strokes.removeAll { s ->
                                s.points.any { p ->
                                    val dx = p.x - pos.x
                                    val dy = p.y - pos.y
                                    dx * dx + dy * dy < 48f * 48f
                                }
                            }
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            // Palm rejection: in pen mode only stylus input draws.
                            if (stylusOnly && down.type != PointerType.Stylus) return@awaitEachGesture
                            down.consume()
                            val stroke = if (eraserMode) null else DrawnStroke().also {
                                it.points.add(down.position)
                                it.times.add(System.currentTimeMillis())
                                strokes.add(it)
                            }
                            if (eraserMode) eraseAt(down.position)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.positionChanged()) {
                                    if (eraserMode) {
                                        eraseAt(change.position)
                                    } else {
                                        stroke?.points?.add(change.position)
                                        stroke?.times?.add(System.currentTimeMillis())
                                    }
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
                val rows = PAPER_ROWS
                val rowGap = ROW_GAP
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
                    } else if (s.points.size == 1) {
                        // Single tap = a dot (period, i-dot) — make it visible.
                        drawCircle(Color(0xFF37474F), radius = 5f, center = s.points[0])
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
