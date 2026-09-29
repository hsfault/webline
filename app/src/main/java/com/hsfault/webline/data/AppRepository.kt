package com.hsfault.webline.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/** Packages of the default phone, SMS, camera and browser apps. */
data class Roles(val dialer: String?, val sms: String?, val camera: String?, val browser: String?) {
    companion object {
        fun resolve(context: Context): Roles {
            val pm = context.packageManager
            fun r(intent: Intent): String? =
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName
                    ?.takeIf { it != "android" }
            return Roles(
                dialer = r(Intent(Intent.ACTION_DIAL)),
                sms = r(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
                camera = r(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
                browser = r(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))),
            )
        }
    }
}

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val pm = context.packageManager
    private val user: UserHandle = Process.myUserHandle()
    private val iconPx = (48 * context.resources.displayMetrics.density).toInt()
    private var callback: LauncherApps.Callback? = null

    /** Icons and install times are decoded once and reused until that package changes. */
    private class Cached(val icon: ImageBitmap, val installedAt: Long)
    private val cache = ConcurrentHashMap<String, Cached>()
    private val dirty: MutableSet<String> = Collections.synchronizedSet(HashSet())

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set

    suspend fun reload() {
        apps = withContext(Dispatchers.Default) {
            val stale = synchronized(dirty) { dirty.toSet().also { dirty.clear() } }
            if (stale.isNotEmpty()) {
                cache.keys.removeAll { key -> stale.any { pkg -> key.startsWith("$pkg/") } }
            }
            val roles = Roles.resolve(context)
            launcherApps.getActivityList(null, user)
                .map { build(it, roles) }
                .sortedBy { it.label.lowercase() }
        }
    }

    private fun build(info: LauncherActivityInfo, roles: Roles): AppEntry {
        val cn = info.componentName
        val key = cn.flattenToString()
        val label = info.label.toString()
        val cached = cache.getOrPut(key) {
            Cached(
                icon = info.getIcon(0).toBitmap(iconPx, iconPx).asImageBitmap(),
                installedAt = installTime(cn.packageName),
            )
        }
        return AppEntry(
            key = key,
            component = cn,
            label = label,
            kind = classify(cn.packageName, label, roles),
            glyph = GlyphSource.Original(cached.icon),
            installedAt = cached.installedAt,
        )
    }

    private fun installTime(pkg: String): Long = try {
        pm.getPackageInfo(pkg, 0).firstInstallTime
    } catch (e: Exception) {
        0L
    }

    private fun markDirty(pkg: String?) {
        if (pkg != null) dirty.add(pkg)
    }

    fun startWatching(onChange: () -> Unit) {
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                markDirty(packageName); onChange()
            }
            override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                markDirty(packageName); onChange()
            }
            override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                markDirty(packageName); onChange()
            }
            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                packageNames?.forEach { markDirty(it) }; onChange()
            }
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                packageNames?.forEach { markDirty(it) }; onChange()
            }
        }
        callback = cb
        launcherApps.registerCallback(cb, Handler(Looper.getMainLooper()))
    }

    fun stopWatching() {
        callback?.let { launcherApps.unregisterCallback(it) }
        callback = null
    }

    fun launch(app: AppEntry) {
        try {
            launcherApps.startMainActivity(app.component, user, null, null)
        } catch (e: Exception) {
            Toast.makeText(context, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(app: AppEntry) {
        try {
            launcherApps.startAppDetailsActivity(app.component, user, null, null)
        } catch (e: Exception) {
            Toast.makeText(context, "Can't open app info", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Recognises generic app types (used to pick the default layout). */
fun classify(pkg: String, label: String, roles: Roles): GlyphKind? {
    val p = pkg.lowercase()
    val l = label.lowercase()
    return when {
        pkg == roles.dialer || p.contains("dialer") -> GlyphKind.PHONE
        pkg == roles.sms || p.contains("mms") || p.contains("messaging") || l == "messages" -> GlyphKind.MESSAGES
        pkg == roles.camera || p.contains("camera") || l == "camera" -> GlyphKind.CAMERA
        pkg == "com.android.settings" || l == "settings" -> GlyphKind.SETTINGS
        p.contains("gallery") || l.contains("gallery") -> GlyphKind.GALLERY
        p.contains("calculator") || l.contains("calculator") -> GlyphKind.CALCULATOR
        p.contains("calendar") || l.contains("calendar") -> GlyphKind.CALENDAR
        p.contains("deskclock") || l == "clock" -> GlyphKind.CLOCK
        p.contains("contacts") || l == "contacts" -> GlyphKind.CONTACTS
        p.contains("filemanager") || p.contains("documentsui") || l == "files" || l.contains("file manager") -> GlyphKind.FILES
        p.contains("notes") || l.contains("notes") -> GlyphKind.NOTES
        p.contains("recorder") || l.contains("recorder") -> GlyphKind.RECORDER
        p.contains("compass") || l.contains("compass") -> GlyphKind.COMPASS
        p.contains("weather") || l.contains("weather") -> GlyphKind.WEATHER
        p.contains("music") || l == "music" -> GlyphKind.MUSIC
        else -> null
    }
}