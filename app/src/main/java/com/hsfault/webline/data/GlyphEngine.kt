package com.hsfault.webline.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Turns an installed app icon into a white mask (tinted red later).
 * Every pixel is compared with the icon's own background colours, so the logo
 * survives and the background plate disappears. Returns null when the result
 * would look bad, and the tile falls back to the original icon.
 */
object GlyphEngine {

    private const val N = 128
    private val SAMPLES = listOf(
        0.5f to 0.1f, 0.1f to 0.5f, 0.9f to 0.5f, 0.5f to 0.9f,
        0.23f to 0.23f, 0.77f to 0.23f, 0.23f to 0.77f, 0.77f to 0.77f,
    )

    fun extract(drawable: Drawable): Bitmap? {
        val src = drawable.toBitmap(N, N, Bitmap.Config.ARGB_8888)
        val px = IntArray(N * N)
        src.getPixels(px, 0, N, 0, 0, N, N)

        val background = SAMPLES
            .map { (fx, fy) -> px[(fy * N).toInt() * N + (fx * N).toInt()] }
            .filter { Color.alpha(it) > 200 }

        val mask = IntArray(N * N)
        var total = 0f
        var minX = N
        var minY = N
        var maxX = -1
        var maxY = -1

        for (y in 0 until N) {
            for (x in 0 until N) {
                val c = px[y * N + x]
                val a = Color.alpha(c) / 255f
                if (a < 0.03f) continue
                val m = if (background.isEmpty()) {
                    a
                } else {
                    var nearest = Float.MAX_VALUE
                    for (b in background) nearest = min(nearest, distance(c, b))
                    a * smoothstep(60f, 140f, nearest)
                }
                if (m < 0.02f) continue
                total += m
                mask[y * N + x] = Color.argb((m * 255f).roundToInt().coerceIn(0, 255), 255, 255, 255)
                if (m > 0.3f) {
                    minX = min(minX, x); maxX = max(maxX, x)
                    minY = min(minY, y); maxY = max(maxY, y)
                }
            }
        }

        val coverage = total / (N * N)
        if (maxX < 0 || coverage < 0.02f || coverage > 0.6f) return null

        // Crop to a square around the glyph so every glyph fills its tile evenly.
        val full = Bitmap.createBitmap(mask, N, N, Bitmap.Config.ARGB_8888)
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val half = max(maxX - minX, maxY - minY) / 2f * 1.06f + 1f
        val left = cx - half
        val top = cy - half
        val span = half * 2f
        val crop = Rect(
            left.toInt().coerceAtLeast(0),
            top.toInt().coerceAtLeast(0),
            (cx + half).toInt().coerceAtMost(N),
            (cy + half).toInt().coerceAtMost(N),
        )
        val dst = RectF(
            (crop.left - left) / span * N,
            (crop.top - top) / span * N,
            (crop.right - left) / span * N,
            (crop.bottom - top) / span * N,
        )
        val out = Bitmap.createBitmap(N, N, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(full, crop, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }

    private fun distance(a: Int, b: Int): Float {
        val dr = (Color.red(a) - Color.red(b)).toFloat()
        val dg = (Color.green(a) - Color.green(b)).toFloat()
        val db = (Color.blue(a) - Color.blue(b)).toFloat()
        return sqrt(dr * dr + dg * dg + db * db)
    }

    private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}