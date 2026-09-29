package com.hsfault.webline.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.view.WindowManager
import kotlin.math.max

/** Center-crops the chosen image to the exact screen size and sets it for home and lock screen. */
object WallpaperSetter {

    fun apply(context: Context, uri: Uri) {
        val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        val tw = bounds.width()
        val th = bounds.height()

        val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: error("Can't read the image")

        val scale = max(tw / source.width.toFloat(), th / source.height.toFloat())
        val sw = source.width * scale
        val sh = source.height * scale

        val out = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            source,
            null,
            RectF((tw - sw) / 2f, (th - sh) / 2f, (tw + sw) / 2f, (th + sh) / 2f),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        source.recycle()

        WallpaperManager.getInstance(context).setBitmap(
            out, null, true,
            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
        )
        out.recycle()
    }
}