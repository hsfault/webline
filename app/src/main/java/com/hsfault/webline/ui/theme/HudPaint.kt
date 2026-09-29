package com.hsfault.webline.ui.theme

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Path as ComposePath

/**
 * A glowing segment along one polygon edge. Edge i runs from point i to point i+1.
 * For cut-corner cards: 0 top, 1 top-right cut, 2 right, 3 bottom-right cut,
 * 4 bottom, 5 bottom-left cut, 6 left, 7 top-left cut.
 */
data class GlowSeg(val edge: Int, val from: Float = 0f, val to: Float = 1f)

/** Renders the 3D slabs, cards and tiles into bitmaps once, with real blur glows, and caches them. */
object HudPaint {

    private val GLOW = 0xFFFF2A2A.toInt()
    private val HOT = 0xFFFF9C9C.toInt()
    private val tileCache = HashMap<String, ImageBitmap>()

    /** Card/plate bitmaps, reused across recompositions and page swipes (48 MB budget). */
    private val slabCache = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun cachedSlab(key: String, build: () -> Bitmap): ImageBitmap {
        slabCache.get(key)?.let { return it }
        val image = build().asImageBitmap()
        slabCache.put(key, image)
        return image
    }

    fun cutPoints(w: Float, h: Float, tl: Float, tr: Float, br: Float, bl: Float): List<PointF> = listOf(
        PointF(tl, 0f), PointF(w - tr, 0f), PointF(w, tr), PointF(w, h - br),
        PointF(w - br, h), PointF(bl, h), PointF(0f, h - bl), PointF(0f, tl),
    )

    private fun pathOf(points: List<PointF>, dx: Float, dy: Float): Path = Path().apply {
        points.forEachIndexed { i, p ->
            if (i == 0) moveTo(p.x + dx, p.y + dy) else lineTo(p.x + dx, p.y + dy)
        }
        close()
    }

    fun renderSlab(
        width: Int,
        height: Int,
        margin: Int,
        points: List<PointF>,
        glows: List<GlowSeg>,
        d: Float,
        plate: Boolean,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width + margin * 2, height + margin * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val m = margin.toFloat()
        val h = height.toFloat()
        val shape = pathOf(points, m, m)
        val corner = CornerPathEffect(3f * d)

        // Drop shadow
        c.save()
        c.translate(0f, 7f * d)
        c.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            alpha = if (plate) 210 else 175
            maskFilter = BlurMaskFilter(14f * d, BlurMaskFilter.Blur.NORMAL)
            pathEffect = corner
        })
        c.restore()

        // Slab thickness: the 3D edge under the face
        c.save()
        c.translate(0f, (if (plate) 5f else 2.5f) * d)
        c.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF020203.toInt()
            pathEffect = corner
        })
        c.restore()

        // Face
        c.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            pathEffect = corner
            shader = LinearGradient(
                0f, m, 0f, m + h,
                (if (plate) 0xF2131318 else 0xF61B1B21).toInt(),
                (if (plate) 0xF2050507 else 0xF6060608).toInt(),
                Shader.TileMode.CLAMP,
            )
        })

        // Glassy sheen on the upper half
        c.save()
        c.clipPath(shape)
        c.drawRect(m, m, m + width, m + h * 0.45f, Paint().apply {
            shader = LinearGradient(0f, m, 0f, m + h * 0.45f, 0x18FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        c.restore()

        // Border
        c.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1f * d
            color = 0xFF2E2E35.toInt()
            pathEffect = corner
        })

        // Top highlight
        if (points.size > 1) {
            val a = points[0]
            val b = points[1]
            c.drawLine(a.x + m, a.y + m, b.x + m, b.y + m, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                alpha = 45
                strokeWidth = 1f * d
            })
        }

        glows.forEach { g ->
            if (g.edge in points.indices) {
                glow(c, points[g.edge], points[(g.edge + 1) % points.size], g.from, g.to, m, d)
            }
        }
        return bmp
    }

    private fun glow(c: Canvas, a: PointF, b: PointF, from: Float, to: Float, off: Float, d: Float) {
        val x1 = off + a.x + (b.x - a.x) * from
        val y1 = off + a.y + (b.y - a.y) * from
        val x2 = off + a.x + (b.x - a.x) * to
        val y2 = off + a.y + (b.y - a.y) * to
        c.drawLine(x1, y1, x2, y2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GLOW
            alpha = 190
            strokeWidth = 6f * d
            strokeCap = Paint.Cap.ROUND
            maskFilter = BlurMaskFilter(7f * d, BlurMaskFilter.Blur.NORMAL)
        })
        c.drawLine(x1, y1, x2, y2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GLOW
            strokeWidth = 1.8f * d
            strokeCap = Paint.Cap.ROUND
        })
        c.drawLine(x1, y1, x2, y2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = HOT
            alpha = 200
            strokeWidth = 0.7f * d
            strokeCap = Paint.Cap.ROUND
        })
    }

    fun tileMargin(d: Float): Int = (12f * d).toInt()

    /** Beveled 3D tile (octagon, or rounded square for the dock). Cached per size. */
    fun tile(sizePx: Int, rounded: Boolean, d: Float): ImageBitmap =
        tileCache.getOrPut("$sizePx-$rounded") { renderTile(sizePx, rounded, d).asImageBitmap() }

    private fun tileShape(s: Float, inset: Float, rounded: Boolean, off: Float): Path {
        val size = s - inset * 2f
        return if (rounded) {
            Path().apply {
                addRoundRect(
                    RectF(off + inset, off + inset, off + inset + size, off + inset + size),
                    size * 0.27f, size * 0.27f, Path.Direction.CW,
                )
            }
        } else {
            val cut = size * 0.27f
            pathOf(cutPoints(size, size, cut, cut, cut, cut), off + inset, off + inset)
        }
    }

    private fun renderTile(sizePx: Int, rounded: Boolean, d: Float): Bitmap {
        val margin = tileMargin(d)
        val s = sizePx.toFloat()
        val off = margin.toFloat()
        val bmp = Bitmap.createBitmap(sizePx + margin * 2, sizePx + margin * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val roundOuter = if (rounded) null else CornerPathEffect(s * 0.1f)
        val roundInner = if (rounded) null else CornerPathEffect(s * 0.08f)
        val outer = tileShape(s, 0f, rounded, off)
        val inner = tileShape(s, s * 0.07f, rounded, off)

        // Shadow
        c.save()
        c.translate(0f, 3.5f * d)
        c.drawPath(outer, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF000000.toInt()
            alpha = 210
            maskFilter = BlurMaskFilter(6f * d, BlurMaskFilter.Blur.NORMAL)
            pathEffect = roundOuter
        })
        c.restore()

        // Red rim glow, bottom-right
        c.drawPath(outer, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 3f * d
            pathEffect = roundOuter
            maskFilter = BlurMaskFilter(5f * d, BlurMaskFilter.Blur.NORMAL)
            shader = LinearGradient(
                off, off, off + s, off + s,
                intArrayOf(0x00FF2A2A, 0x00FF2A2A, 0xDDFF2A2A.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        })

        // Outer body
        c.drawPath(outer, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            pathEffect = roundOuter
            shader = LinearGradient(off, off, off, off + s, 0xFF25252C.toInt(), 0xFF09090C.toInt(), Shader.TileMode.CLAMP)
        })

        // Rim: white highlight top-left fading to red bottom-right
        c.drawPath(outer, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1.2f * d
            pathEffect = roundOuter
            shader = LinearGradient(
                off, off, off + s, off + s,
                intArrayOf(0x70FFFFFF, 0x12FFFFFF, 0xFFD01422.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        })

        // Inner raised face (the bevel)
        c.drawPath(inner, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            pathEffect = roundInner
            shader = LinearGradient(off, off, off, off + s, 0xFF1B1B21.toInt(), 0xFF0A0A0D.toInt(), Shader.TileMode.CLAMP)
        })
        c.drawPath(inner, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 0.8f * d
            pathEffect = roundInner
            shader = LinearGradient(off, off, off, off + s, 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        return bmp
    }
}

/** Compose path of a cut-corner card, for clipping. */
fun hudCutPath(size: Size, tl: Float, tr: Float, br: Float, bl: Float): ComposePath = ComposePath().apply {
    val p = HudPaint.cutPoints(size.width, size.height, tl, tr, br, bl)
    moveTo(p[0].x, p[0].y)
    for (i in 1 until p.size) lineTo(p[i].x, p[i].y)
    close()
}

/** Angled glass card with shadow, 3D edge, sheen and glowing edges. Rendered once per size, then cached. */
fun Modifier.hudCard(tl: Dp, tr: Dp, br: Dp, bl: Dp, glows: List<GlowSeg> = emptyList()): Modifier = drawWithCache {
    val w = size.width.toInt()
    val h = size.height.toInt()
    if (w < 2 || h < 2) return@drawWithCache onDrawBehind { }
    val margin = 28.dp.toPx().toInt()
    val key = "card:$w:$h:${tl.value}:${tr.value}:${br.value}:${bl.value}:$density:$glows"
    val image = HudPaint.cachedSlab(key) {
        val pts = HudPaint.cutPoints(size.width, size.height, tl.toPx(), tr.toPx(), br.toPx(), bl.toPx())
        HudPaint.renderSlab(w, h, margin, pts, glows, density, plate = false)
    }
    onDrawBehind { drawImage(image, topLeft = Offset(-margin.toFloat(), -margin.toFloat())) }
}

/** Tilted slab under an icon cluster. Points are fractions of this element's size. */
fun Modifier.hudPlate(points: List<Offset>, glows: List<GlowSeg>): Modifier = drawWithCache {
    val w = size.width.toInt()
    val h = size.height.toInt()
    if (w < 2 || h < 2) return@drawWithCache onDrawBehind { }
    val margin = 28.dp.toPx().toInt()
    val key = "plate:$w:$h:$density:$points:$glows"
    val image = HudPaint.cachedSlab(key) {
        val pts = points.map { PointF(it.x * size.width, it.y * size.height) }
        HudPaint.renderSlab(w, h, margin, pts, glows, density, plate = true)
    }
    onDrawBehind { drawImage(image, topLeft = Offset(-margin.toFloat(), -margin.toFloat())) }
}