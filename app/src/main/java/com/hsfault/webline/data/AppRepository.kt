package com.hsfault.webline.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

data class AppEntry(
    val key: String,
    val component: ComponentName,
    val label: String,
    val icon: ImageBitmap,
)

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val user: UserHandle = Process.myUserHandle()
    private val iconPx = (48 * context.resources.displayMetrics.density).toInt()
    private var callback: LauncherApps.Callback? = null

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set

    suspend fun reload() {
        apps = withContext(Dispatchers.IO) {
            launcherApps.getActivityList(null, user)
                .map { info ->
                    val cn = info.componentName
                    AppEntry(
                        key = cn.flattenToString(),
                        component = cn,
                        label = info.label.toString(),
                        icon = info.getIcon(0).toBitmap(iconPx, iconPx).asImageBitmap(),
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
    }

    fun startWatching(onChange: () -> Unit) {
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
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

/** First-run layout: a sensible dock and a few common apps on page one. */
object Defaults {
    private val HOME_PACKAGES = listOf(
        "com.google.android.youtube",
        "com.instagram.android",
        "com.android.vending",
        "com.google.android.gm",
        "com.google.android.apps.maps",
        "com.android.settings",
    )

    fun dock(context: Context, apps: List<AppEntry>): List<String> {
        val pm = context.packageManager
        fun resolve(intent: Intent): String? =
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
                ?.takeIf { it != "android" }

        val packages = listOfNotNull(
            resolve(Intent(Intent.ACTION_DIAL)),
            "com.whatsapp",
            resolve(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
            resolve(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))),
            resolve(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
        )
        return packages.mapNotNull { keyFor(apps, it) }.distinct().take(LayoutStore.DOCK_SIZE)
    }

    fun home(context: Context, apps: List<AppEntry>, exclude: Set<String>): List<String> {
        val own = keyFor(apps, context.packageName)
        return (HOME_PACKAGES.mapNotNull { keyFor(apps, it) } + listOfNotNull(own))
            .filter { it !in exclude }
            .distinct()
    }

    private fun keyFor(apps: List<AppEntry>, pkg: String): String? =
        apps.firstOrNull { it.component.packageName == pkg }?.key
}