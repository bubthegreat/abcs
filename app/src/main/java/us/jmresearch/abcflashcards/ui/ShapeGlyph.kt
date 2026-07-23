package us.jmresearch.abcflashcards.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** True when a card id should be rendered as a drawn shape instead of text. */
fun isShapeSpec(id: String): Boolean =
    id.startsWith("color_") || id.startsWith("shape_") || id.startsWith("combo_")

private val colorValues = mapOf(
    "red" to Color(0xFFE53935),
    "blue" to Color(0xFF1E88E5),
    "green" to Color(0xFF43A047),
    "yellow" to Color(0xFFFDD835),
    "orange" to Color(0xFFFB8C00),
    "purple" to Color(0xFF8E24AA),
    "pink" to Color(0xFFF06292),
    "brown" to Color(0xFF6D4C41),
    "black" to Color(0xFF212121),
    "white" to Color(0xFFFFFFFF),
)

private val neutralShapeColor = Color(0xFF455A64)

/**
 * Draws a card's shape/color from its id:
 * `color_<name>` = filled circle of that color,
 * `shape_<name>` = that shape in a neutral slate,
 * `combo_<color>_<shape>` = that shape in that color.
 */
@Composable
fun ShapeGlyph(spec: String, modifier: Modifier = Modifier) {
    val (shape, color) = when {
        spec.startsWith("color_") -> "circle" to (colorValues[spec.removePrefix("color_")] ?: neutralShapeColor)
        spec.startsWith("shape_") -> spec.removePrefix("shape_") to neutralShapeColor
        spec.startsWith("combo_") -> {
            val parts = spec.removePrefix("combo_").split("_")
            (parts.getOrElse(1) { "circle" }) to (colorValues[parts.getOrElse(0) { "" }] ?: neutralShapeColor)
        }
        else -> "circle" to neutralShapeColor
    }
    val needsOutline = color == Color(0xFFFFFFFF)
    Canvas(modifier = modifier) {
        drawShape(shape, color)
        if (needsOutline) drawShape(shape, Color(0xFF616161), stroke = true)
    }
}

private fun DrawScope.drawShape(shape: String, color: Color, stroke: Boolean = false) {
    val style = if (stroke) Stroke(width = size.minDimension * 0.04f) else androidx.compose.ui.graphics.drawscope.Fill
    val w = size.width
    val h = size.height
    val s = min(w, h)
    val cx = w / 2
    val cy = h / 2

    fun polygon(n: Int, rotationDeg: Float): Path {
        val r = s * 0.46f
        val path = Path()
        for (i in 0 until n) {
            val a = Math.toRadians(rotationDeg + i * 360.0 / n)
            val x = cx + (r * cos(a)).toFloat()
            val y = cy + (r * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    when (shape) {
        "circle" -> drawCircle(color, radius = s * 0.46f, center = Offset(cx, cy), style = style)
        "square" -> drawRect(
            color,
            topLeft = Offset(cx - s * 0.42f, cy - s * 0.42f),
            size = Size(s * 0.84f, s * 0.84f),
            style = style,
        )
        "rectangle" -> drawRect(
            color,
            topLeft = Offset(cx - s * 0.48f, cy - s * 0.28f),
            size = Size(s * 0.96f, s * 0.56f),
            style = style,
        )
        "oval" -> drawOval(
            color,
            topLeft = Offset(cx - s * 0.48f, cy - s * 0.3f),
            size = Size(s * 0.96f, s * 0.6f),
            style = style,
        )
        "triangle" -> drawPath(polygon(3, -90f), color, style = style)
        "diamond" -> drawPath(polygon(4, -90f), color, style = style)
        "pentagon" -> drawPath(polygon(5, -90f), color, style = style)
        "hexagon" -> drawPath(polygon(6, 0f), color, style = style)
        "octagon" -> drawPath(polygon(8, 22.5f), color, style = style)
        "star" -> {
            val outer = s * 0.48f
            val inner = s * 0.20f
            val path = Path()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outer else inner
                val a = Math.toRadians(-90.0 + i * 36.0)
                val x = cx + (r * cos(a)).toFloat()
                val y = cy + (r * sin(a)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color, style = style)
        }
        "heart" -> {
            val path = Path().apply {
                val rect = Rect(cx - s * 0.46f, cy - s * 0.44f, cx + s * 0.46f, cy + s * 0.48f)
                moveTo(rect.center.x, rect.top + rect.height * 0.28f)
                cubicTo(
                    rect.center.x - rect.width * 0.06f, rect.top + rect.height * 0.08f,
                    rect.left + rect.width * 0.12f, rect.top,
                    rect.left + rect.width * 0.06f, rect.top + rect.height * 0.3f,
                )
                cubicTo(
                    rect.left, rect.top + rect.height * 0.55f,
                    rect.center.x - rect.width * 0.2f, rect.top + rect.height * 0.75f,
                    rect.center.x, rect.bottom,
                )
                cubicTo(
                    rect.center.x + rect.width * 0.2f, rect.top + rect.height * 0.75f,
                    rect.right, rect.top + rect.height * 0.55f,
                    rect.right - rect.width * 0.06f, rect.top + rect.height * 0.3f,
                )
                cubicTo(
                    rect.right - rect.width * 0.12f, rect.top,
                    rect.center.x + rect.width * 0.06f, rect.top + rect.height * 0.08f,
                    rect.center.x, rect.top + rect.height * 0.28f,
                )
                close()
            }
            drawPath(path, color, style = style)
        }
        "cross" -> {
            val arm = s * 0.3f
            val len = s * 0.92f
            drawRect(color, topLeft = Offset(cx - arm / 2, cy - len / 2), size = Size(arm, len), style = style)
            drawRect(color, topLeft = Offset(cx - len / 2, cy - arm / 2), size = Size(len, arm), style = style)
        }
        else -> drawCircle(color, radius = s * 0.46f, center = Offset(cx, cy), style = style)
    }
}
