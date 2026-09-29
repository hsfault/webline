package com.hsfault.webline.data

import android.content.ComponentName
import androidx.compose.ui.graphics.ImageBitmap

/** Generic app types. Used for default layout picks now, and as a base for the icon pack later. */
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
    val installedAt: Long,
)

enum class Folder(val token: String, val title: String, val glyph: GlyphKind) {
    TOOLS("folder:tools", "Tools", GlyphKind.TOOLS),
    SOCIAL("folder:social", "Social", GlyphKind.SOCIAL);

    companion object {
        fun fromToken(token: String): Folder? = entries.firstOrNull { it.token == token }
    }
}