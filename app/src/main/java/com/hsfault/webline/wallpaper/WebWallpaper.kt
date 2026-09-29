package com.hsfault.webline.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.WindowManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Draws the Web HUD wallpaper at the exact screen size and applies it. */
object WebWallpaper {

    private val VOID = 0xFF050203.toInt()
    private val NIGHT = 0xFF0C0407.toInt()
    private val DEEP = 0xFF1C0210.toInt()
    private val MAROON = 0xFF2A0412.toInt()
    private val CRIMSON = 0xFFD81E4F.toInt()
    private val GLOW = 0xFFFF3B6B.toInt()
    private val SILVER = 0xFF8A8A92.toInt()
    private val GEM = 0xFFFFD6E0.toInt()

    fun apply(context: Context) {
        val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        val bitmap = render(bounds.width(), bounds.height())
        WallpaperManager.getInstance(context).setBitmap(
            bitmap, null, true,
            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
        )
        bitmap.recycle()
    }

    fun render(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()
        val u = w / 360f
        val cx = w / 2f
        val cy = h * 0.30f

        drawBackground(canvas, w, h, cx, cy)
        drawWeb(canvas, w, h, cx, cy, u)
        drawSpider(canvas, cx, cy, u)
        drawPod(canvas, w, h, cx, u)
        return bitmap
    }

    private fun drawBackground(c: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        c.drawRect(0f, 0f, w, h, Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(VOID, NIGHT, DEEP), floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
        })
        val hazeR = w * 0.8f
        c.drawCircle(cx, cy, hazeR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, hazeR,
                intArrayOf(withAlpha(CRIMSON, 50), withAlpha(CRIMSON, 0)), null,
                Shader.TileMode.CLAMP,
            )
        })
    }

    private fun drawWeb(c: Canvas, w: Float, h: Float, cx: Float, cy: Float, u: Float) {
        val spokes = 16
        val step = (2.0 * PI / spokes).toFloat()
        fun angle(i: Int): Float = i * step + if (i % 2 == 1) 0.035f else 0f
        val reach = hypot(w, h)

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.1f * u
            color = SILVER
            alpha = 70
        }
        for (i in 0 until spokes) {
            val a = angle(i)
            c.drawLine(cx, cy, cx + cos(a) * reach, cy + sin(a) * reach, line)
        }

        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f * u
            color = CRIMSON
            alpha = 150
        }

        var r = 24f * u
        var ring = 0
        while (r < reach) {
            val path = Path()
            for (i in 0..spokes) {
                val a = angle(i)
                val x = cx + cos(a) * r
                val y = cy + sin(a) * r
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    val mid = (angle(i - 1) + a) / 2f
                    val rc = r * 0.9f
                    path.quadTo(cx + cos(mid) * rc, cy + sin(mid) * rc, x, y)
                }
            }
            path.close()
            line.alpha = (86 - ring * 5).coerceAtLeast(28)
            c.drawPath(path, line)
            if (ring == 2 || ring == 5) c.drawPath(path, accent)
            if (ring in 1..7) {
                for (i in (ring % 3) until spokes step 3) {
                    val a = angle(i)
                    drawNode(c, cx + cos(a) * r, cy + sin(a) * r, u)
                }
            }
            r *= 1.33f
            ring++
        }
    }

    private fun drawSpider(c: Canvas, cx: Float, cy: Float, u: Float) {
        val s = 22f * u

        c.drawLine(cx, 0f, cx, cy - s * 0.65f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SILVER
            alpha = 130
            strokeWidth = 1f * u
        })

        val legs = Path()
        for (side in intArrayOf(-1, 1)) {
            for (i in 0 until 4) {
                val sx = cx + side * s * 0.16f
                val sy = cy - s * 0.3f + i * s * 0.2f
                val kx = cx + side * s * (0.72f + i * 0.1f)
                val ky = sy - s * (0.6f - i * 0.34f)
                val fx = cx + side * s * (1.12f + i * 0.06f)
                val fy = ky + s * (0.8f + i * 0.12f)
                legs.moveTo(sx, sy)
                legs.lineTo(kx, ky)
                legs.lineTo(fx, fy)
            }
        }

        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = GLOW
            alpha = 55
            strokeWidth = 6f * u
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val limb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = CRIMSON
            strokeWidth = 1.7f * u
            strokeJoin = Paint.Join.MITER
        }
        c.drawPath(legs, halo)
        c.drawPath(legs, limb)

        val head = Path().apply {
            moveTo(cx, cy - s * 0.66f)
            lineTo(cx + s * 0.2f, cy - s * 0.4f)
            lineTo(cx, cy - s * 0.14f)
            lineTo(cx - s * 0.2f, cy - s * 0.4f)
            close()
        }
        val body = Path().apply {
            moveTo(cx, cy - s * 0.08f)
            lineTo(cx + s * 0.32f, cy + s * 0.42f)
            lineTo(cx, cy + s * 1.15f)
            lineTo(cx - s * 0.32f, cy + s * 0.42f)
            close()
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, cy - s, cx, cy + s * 1.2f,
                intArrayOf(GLOW, CRIMSON, MAROON), null,
                Shader.TileMode.CLAMP,
            )
        }
        c.drawPath(head, halo)
        c.drawPath(body, halo)
        c.drawPath(head, fill)
        c.drawPath(body, fill)

        val gy = cy + s * 0.3f
        val g = s * 0.11f
        drawNode(c, cx, gy, u)
        c.drawPath(Path().apply {
            moveTo(cx, gy - g)
            lineTo(cx + g * 0.7f, gy)
            lineTo(cx, gy + g)
            lineTo(cx - g * 0.7f, gy)
            close()
        }, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GEM })
    }

    private fun drawPod(c: Canvas, w: Float, h: Float, cx: Float, u: Float) {
        val py = h * 0.885f
        val glowR = w * 0.55f

        c.save()
        c.scale(1f, 0.26f, cx, py)
        c.drawCircle(cx, py, glowR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, py, glowR,
                intArrayOf(withAlpha(GLOW, 175), withAlpha(CRIMSON, 70), withAlpha(CRIMSON, 0)),
                floatArrayOf(0f, 0.42f, 1f),
                Shader.TileMode.CLAMP,
            )
        })
        c.restore()

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = GLOW
            alpha = 200
            strokeWidth = 1.6f * u
        }
        val rx = w * 0.34f
        val ry = w * 0.065f
        c.drawOval(RectF(cx - rx, py - ry, cx + rx, py + ry), ring)

        ring.alpha = 70
        ring.strokeWidth = 1f * u
        c.drawOval(RectF(cx - w * 0.85f, py - w * 0.2f, cx + w * 0.85f, py + w * 0.2f), ring)
    }

    private fun drawNode(c: Canvas, x: Float, y: Float, u: Float) {
        val glowR = 8f * u
        c.drawCircle(x, y, glowR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                x, y, glowR,
                intArrayOf(withAlpha(GLOW, 120), withAlpha(GLOW, 0)), null,
                Shader.TileMode.CLAMP,
            )
        })
        c.drawCircle(x, y, 1.8f * u, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GLOW })
    }

    private fun withAlpha(color: Int, a: Int): Int = (color and 0x00FFFFFF) or (a shl 24)
}