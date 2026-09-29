package com.hsfault.webline.data

import android.content.ComponentName
import androidx.compose.ui.graphics.ImageBitmap

/** Generic app types that get a hand-drawn red glyph. */
enum class GlyphKind {
    PHONE, MESSAGES, CAMERA, SETTINGS, GALLERY, CALCULATOR, CALENDAR, CLOCK,
    CONTACTS, FILES, NOTES, RECORDER, COMPASS, WEATHER, MUSIC, TOOLS, SOCIAL,
}

/** What gets drawn inside a tile. */
sealed interface GlyphSource {
    data class Drawn(val kind: GlyphKind) : GlyphSource
    class Mask(val bitmap: ImageBitmap) : GlyphSource
    class Original(val bitmap: ImageBitmap) : GlyphSource
}

data class AppEntry(
    val key: String,
    val component: ComponentName,
    val label: String,
    val kind: GlyphKind?,
    val glyph: GlyphSource,
)

enum class Folder(val token: String, val title: String, val glyph: GlyphKind) {
    TOOLS("folder:tools", "Tools", GlyphKind.TOOLS),
    SOCIAL("folder:social", "Social", GlyphKind.SOCIAL);

    companion object {
        fun fromToken(token: String): Folder? = entries.firstOrNull { it.token == token }
    }
}