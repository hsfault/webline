package com.hsfault.webline.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import com.hsfault.webline.data.GlyphKind

enum class UiIcon { SEARCH, MIC, PIN, PLAY, PREV, NEXT, PLUS }

private const val PHONE =
    "M7.4 2.6 L9.8 2.3 C10.3 2.2 10.7 2.5 10.9 3 L12 6.4 C12.1 6.9 12 7.3 11.6 7.6 L9.9 8.9 " +
        "C11 11.3 12.8 13.1 15.2 14.2 L16.5 12.5 C16.8 12.1 17.3 12 17.7 12.1 L21.1 13.2 " +
        "C21.6 13.4 21.9 13.8 21.8 14.3 L21.5 16.7 C21.3 18.9 19.4 20.5 17.2 20.4 " +
        "C9.9 19.9 4.1 14.1 3.6 6.8 C3.5 4.6 5.1 2.7 7.4 2.6 Z"

private const val PIN =
    "M12 2.5 C8.1 2.5 5 5.6 5 9.4 C5 14.2 12 21.5 12 21.5 C12 21.5 19 14.2 19 9.4 C19 5.6 15.9 2.5 12 2.5 Z"

private val pathCache = HashMap<String, Path>()
private fun svg(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/** Draws on a 24-unit grid scaled to the canvas. "clear" punches holes (needs an offscreen layer). */
private class Pen(private val s: DrawScope, private val color: Color) {
    private val u = s.size.minDimension / 24f
    private fun mode(clear: Boolean) = if (clear) BlendMode.Clear else BlendMode.SrcOver

    fun rect(l: Float, t: Float, r: Float, b: Float, radius: Float, clear: Boolean = false) =
        s.drawRoundRect(color, Offset(l * u, t * u), Size((r - l) * u, (b - t) * u), CornerRadius(radius * u), blendMode = mode(clear))

    fun circle(x: Float, y: Float, r: Float, clear: Boolean = false) =
        s.drawCircle(color, r * u, Offset(x * u, y * u), blendMode = mode(clear))

    fun ring(x: Float, y: Float, r: Float, w: Float) =
        s.drawCircle(color, r * u, Offset(x * u, y * u), style = Stroke(w * u))

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 2f, clear: Boolean = false) =
        s.drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), w * u, cap = StrokeCap.Round, blendMode = mode(clear))

    fun arc(l: Float, t: Float, r: Float, b: Float, start: Float, sweep: Float, w: Float) =
        s.drawArc(color, start, sweep, false, Offset(l * u, t * u), Size((r - l) * u, (b - t) * u), style = Stroke(w * u, cap = StrokeCap.Round))

    fun path(d: String, clear: Boolean = false) {
        val p = svg(d)
        s.scale(u, u, Offset.Zero) { drawPath(p, color, blendMode = mode(clear)) }
    }

    fun outline(d: String, w: Float) {
        val p = svg(d)
        s.scale(u, u, Offset.Zero) { drawPath(p, color, style = Stroke(w)) }
    }

    fun gearTeeth() {
        for (i in 0 until 8) {
            s.rotate(i * 45f, Offset(12f * u, 12f * u)) {
                drawRoundRect(color, Offset(10.3f * u, 1.6f * u), Size(3.4f * u, 5.2f * u), CornerRadius(1f * u))
            }
        }
    }
}

/** Original glyphs for generic apps. Drawn white, then tinted red by the tile. */
fun DrawScope.drawGlyph(kind: GlyphKind, color: Color = Color.White) {
    val p = Pen(this, color)
    when (kind) {
        GlyphKind.PHONE -> p.path(PHONE)
        GlyphKind.MESSAGES -> {
            p.rect(3f, 4f, 21f, 17f, 4.5f)
            p.path("M6.5 15.5 L6.5 21 L12 16.5 Z")
            p.circle(8.4f, 10.5f, 1.3f, clear = true)
            p.circle(12f, 10.5f, 1.3f, clear = true)
            p.circle(15.6f, 10.5f, 1.3f, clear = true)
        }
        GlyphKind.CAMERA -> {
            p.path("M8.2 7 L9.6 4.6 C9.8 4.2 10.2 4 10.6 4 L13.4 4 C13.8 4 14.2 4.2 14.4 4.6 L15.8 7 Z")
            p.rect(2.5f, 6.5f, 21.5f, 19.5f, 3f)
            p.circle(12f, 13f, 4.6f, clear = true)
            p.circle(12f, 13f, 2.6f)
            p.circle(18.2f, 9.4f, 0.9f, clear = true)
        }
        GlyphKind.SETTINGS -> {
            p.gearTeeth()
            p.circle(12f, 12f, 7.2f)
            p.circle(12f, 12f, 3.1f, clear = true)
        }
        GlyphKind.GALLERY -> {
            p.rect(3f, 4.5f, 21f, 19.5f, 3f)
            p.rect(5f, 6.5f, 19f, 17.5f, 1.5f, clear = true)
            p.path("M5.8 16.8 L10 11.2 L13.1 15 L15.2 12.6 L18.2 16.8 Z")
            p.circle(15.6f, 9.6f, 1.6f)
        }
        GlyphKind.CALCULATOR -> {
            p.rect(5f, 2.5f, 19f, 21.5f, 3f)
            p.rect(7f, 4.6f, 17f, 8.6f, 1.2f, clear = true)
            for (row in listOf(12.3f, 15.7f, 19.1f)) {
                for (col in listOf(8.8f, 12f, 15.2f)) p.circle(col, row, 1.2f, clear = true)
            }
        }
        GlyphKind.CALENDAR -> {
            p.rect(3f, 5f, 21f, 21f, 3f)
            p.rect(5f, 10f, 19f, 19f, 1.2f, clear = true)
            p.rect(7f, 2.6f, 9.4f, 7.4f, 1.2f)
            p.rect(14.6f, 2.6f, 17f, 7.4f, 1.2f)
            p.rect(7f, 11.8f, 9.6f, 14.2f, 0.5f)
            p.rect(10.7f, 11.8f, 13.3f, 14.2f, 0.5f)
            p.rect(14.4f, 11.8f, 17f, 14.2f, 0.5f)
            p.rect(7f, 15.3f, 9.6f, 17.7f, 0.5f)
            p.rect(10.7f, 15.3f, 13.3f, 17.7f, 0.5f)
        }
        GlyphKind.CLOCK -> {
            p.circle(12f, 12f, 9.5f)
            p.circle(12f, 12f, 7.6f, clear = true)
            p.line(12f, 12f, 12f, 7.2f)
            p.line(12f, 12f, 15.4f, 14f)
            p.circle(12f, 12f, 1.4f)
        }
        GlyphKind.CONTACTS -> {
            p.circle(12f, 8f, 4f)
            p.path("M4 20.5 C4 16 7.6 13.6 12 13.6 C16.4 13.6 20 16 20 20.5 Z")
        }
        GlyphKind.FILES -> {
            p.path(
                "M2.5 6.6 C2.5 5.4 3.4 4.5 4.6 4.5 L9.4 4.5 L11.6 7 L19.4 7 C20.6 7 21.5 7.9 21.5 9.1 " +
                    "L21.5 17.4 C21.5 18.6 20.6 19.5 19.4 19.5 L4.6 19.5 C3.4 19.5 2.5 18.6 2.5 17.4 Z"
            )
            p.rect(2.5f, 9f, 21.5f, 10.2f, 0f, clear = true)
        }
        GlyphKind.NOTES -> {
            p.rect(5f, 2.5f, 19f, 21.5f, 2.5f)
            p.rect(8f, 7.5f, 16f, 9.1f, 0.8f, clear = true)
            p.rect(8f, 11.2f, 16f, 12.8f, 0.8f, clear = true)
            p.rect(8f, 14.9f, 13f, 16.5f, 0.8f, clear = true)
        }
        GlyphKind.RECORDER -> {
            p.rect(9f, 2.5f, 15f, 14.5f, 3f)
            p.arc(5.8f, 6.5f, 18.2f, 17.8f, 0f, 180f, 2f)
            p.line(12f, 17.8f, 12f, 21f)
            p.line(8.6f, 21f, 15.4f, 21f)
        }
        GlyphKind.COMPASS -> {
            p.circle(12f, 12f, 9.5f)
            p.circle(12f, 12f, 7.7f, clear = true)
            p.path("M12 5.6 L14.6 12 L12 18.4 L9.4 12 Z")
            p.circle(12f, 12f, 1.1f, clear = true)
        }
        GlyphKind.WEATHER -> {
            p.circle(9f, 9f, 3.8f)
            p.rect(4.5f, 11f, 21.5f, 20.5f, 4.75f, clear = true)
            p.circle(13f, 12.2f, 5.2f, clear = true)
            p.rect(5.5f, 12f, 20.5f, 19.5f, 3.75f)
            p.circle(13f, 12.2f, 4.2f)
        }
        GlyphKind.MUSIC -> {
            p.rect(14.2f, 3.5f, 16.4f, 16.5f, 1f)
            p.path("M16.4 3.5 L21 5 L21 8.4 L16.4 7 Z")
            p.circle(12f, 16.8f, 3.8f)
        }
        GlyphKind.TOOLS -> {
            p.rect(3.5f, 3.5f, 10.5f, 10.5f, 2.2f)
            p.rect(13.5f, 3.5f, 20.5f, 10.5f, 2.2f)
            p.rect(3.5f, 13.5f, 10.5f, 20.5f, 2.2f)
            p.rect(13.5f, 13.5f, 20.5f, 20.5f, 2.2f)
        }
        GlyphKind.SOCIAL -> {
            p.circle(16.4f, 8.2f, 3f)
            p.path("M13 19.5 C13 15.9 14.6 13.7 17 13.7 C19.8 13.7 21.6 15.9 21.6 19.5 Z")
            p.circle(9f, 8.4f, 4.6f, clear = true)
            p.circle(9f, 8.4f, 3.6f)
            p.path("M2.4 20.5 C2.4 16.2 5.4 13.8 9 13.8 C12.6 13.8 15.6 16.2 15.6 20.5 Z")
        }
    }
}

/** Small interface icons (search, mic, pin, media controls). Drawn directly in a colour. */
fun DrawScope.drawUiIcon(icon: UiIcon, color: Color) {
    val p = Pen(this, color)
    when (icon) {
        UiIcon.SEARCH -> {
            p.ring(10.5f, 10.5f, 6.3f, 2.2f)
            p.line(15.3f, 15.3f, 20.6f, 20.6f, 2.4f)
        }
        UiIcon.MIC -> {
            p.rect(9.3f, 2.5f, 14.7f, 13.8f, 2.7f)
            p.arc(5.8f, 6.8f, 18.2f, 17f, 0f, 180f, 2f)
            p.line(12f, 17f, 12f, 21f)
        }
        UiIcon.PIN -> {
            p.outline(PIN, 2f)
            p.ring(12f, 9.4f, 2.4f, 2f)
        }
        UiIcon.PLAY -> p.path("M8 5 L19.5 12 L8 19 Z")
        UiIcon.PREV -> {
            p.rect(5f, 5f, 7.4f, 19f, 1f)
            p.path("M19.5 5 L8.6 12 L19.5 19 Z")
        }
        UiIcon.NEXT -> {
            p.path("M4.5 5 L15.4 12 L4.5 19 Z")
            p.rect(16.6f, 5f, 19f, 19f, 1f)
        }
        UiIcon.PLUS -> {
            p.line(12f, 5f, 12f, 19f)
            p.line(5f, 12f, 19f, 12f)
        }
    }
}