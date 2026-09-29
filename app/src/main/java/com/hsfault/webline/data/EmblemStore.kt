package com.hsfault.webline.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Keeps a copy of the lock-cover emblem image (PNG, transparency preserved) inside the app. */
object EmblemStore {

    private const val FILE = "lock_emblem.png"
    private const val MAX = 1024

    private var cached: ImageBitmap? = null
    private var cachedStamp = -1L

    fun exists(context: Context): Boolean = File(context.filesDir, FILE).exists()

    fun save(context: Context, uri: Uri) {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX && bounds.outHeight / (sample * 2) >= MAX) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Can't read the image")

        val scale = min(1f, MAX.toFloat() / max(decoded.width, decoded.height))
        val out = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true,
            )
        } else {
            decoded
        }

        File(context.filesDir, FILE).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        cached = null
        cachedStamp = -1L
    }

    fun load(context: Context): ImageBitmap? {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) return null
        if (file.lastModified() == cachedStamp && cached != null) return cached
        val bitmap = BitmapFactory.decodeFile(file.path) ?: return null
        cached = bitmap.asImageBitmap()
        cachedStamp = file.lastModified()
        return cached
    }
}