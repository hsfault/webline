package com.hsfault.webline.data

import android.content.Context

/** First-run layout that mirrors the reference design. */
object Defaults {

    private val SOCIAL_PACKAGES = listOf(
        "com.whatsapp", "com.instagram.android", "com.facebook.katana", "com.facebook.orca",
        "com.snapchat.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.twitter.android", "org.telegram.messenger", "com.discord",
    )

    private val TOOL_KINDS = listOf(
        GlyphKind.CLOCK, GlyphKind.FILES, GlyphKind.CONTACTS, GlyphKind.NOTES,
        GlyphKind.RECORDER, GlyphKind.COMPASS, GlyphKind.WEATHER,
    )

    fun seed(context: Context, apps: List<AppEntry>, layout: LayoutStore) {
        val roles = Roles.resolve(context)
        fun pkg(name: String?): String =
            name?.let { n -> apps.firstOrNull { it.component.packageName == n }?.key }.orEmpty()
        fun kind(k: GlyphKind): String = apps.firstOrNull { it.kind == k }?.key.orEmpty()

        val slots = listOf(
            // left staircase
            pkg("com.google.android.youtube"), pkg("com.instagram.android"), pkg("com.google.android.apps.maps"),
            // right staircase
            pkg("com.android.vending"), pkg("com.google.android.gm"), kind(GlyphKind.CALCULATOR),
            // bottom-left row
            kind(GlyphKind.SETTINGS), Folder.TOOLS.token, kind(GlyphKind.GALLERY),
            // bottom-right row
            kind(GlyphKind.CAMERA), kind(GlyphKind.CALENDAR), Folder.SOCIAL.token,
        )

        val browser = pkg(roles.browser).ifEmpty { pkg("com.android.chrome") }
        val dock = listOf(
            kind(GlyphKind.PHONE), pkg("com.whatsapp"), kind(GlyphKind.MESSAGES), browser, kind(GlyphKind.CAMERA),
        )

        val tools = TOOL_KINDS.map { kind(it) }.filter { it.isNotEmpty() }
        val social = SOCIAL_PACKAGES.map { pkg(it) }.filter { it.isNotEmpty() }

        layout.seed(slots, dock, tools, social)
    }
}