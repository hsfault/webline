package com.hsfault.webline.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val playing: Boolean,
    val packageName: String,
)

/** Follows the active media session (Spotify, YouTube Music, any player) and exposes it to the UI. */
class MediaRepository(private val context: Context) {

    private val sessions = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, WeblineNotificationListener::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var started = false
    private var artSource: Bitmap? = null
    private var artImage: ImageBitmap? = null

    var nowPlaying by mutableStateOf<NowPlaying?>(null)
        private set
    var hasAccess by mutableStateOf(false)
        private set

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publish()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish()
        }

        override fun onSessionDestroyed() {
            pick(activeSessions())
        }
    }

    /** Safe to call on every resume: picks up access granted in Settings. */
    fun start() {
        hasAccess = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        if (!hasAccess || started) return
        try {
            sessions.addOnActiveSessionsChangedListener(sessionsListener, component, main)
            started = true
            pick(activeSessions())
        } catch (e: SecurityException) {
            hasAccess = false
        }
    }

    fun stop() {
        if (started) {
            try {
                sessions.removeOnActiveSessionsChangedListener(sessionsListener)
            } catch (e: Exception) {
                // already removed
            }
        }
        started = false
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun activeSessions(): List<MediaController> = try {
        sessions.getActiveSessions(component)
    } catch (e: SecurityException) {
        emptyList()
    }

    private fun pick(list: List<MediaController>?) {
        val next = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = next
            next?.registerCallback(callback, main)
        }
        publish()
    }

    private fun publish() {
        val c = controller ?: run { nowPlaying = null; return }
        val md = c.metadata ?: run { nowPlaying = null; return }
        val title = (md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE))
            ?.takeIf { it.isNotBlank() }
            ?: run { nowPlaying = null; return }
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: appLabel(c.packageName)
        val source = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (source !== artSource) {
            artSource = source
            artImage = source?.let { scaleArt(it).asImageBitmap() }
        }
        nowPlaying = NowPlaying(
            title = title,
            artist = artist,
            art = artImage,
            playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = c.packageName,
        )
    }

    private fun scaleArt(src: Bitmap): Bitmap {
        val max = 256
        if (src.width <= max && src.height <= max) {
            return src.copy(Bitmap.Config.ARGB_8888, false) ?: src
        }
        val s = max.toFloat() / maxOf(src.width, src.height)
        return Bitmap.createScaledBitmap(
            src,
            (src.width * s).toInt().coerceAtLeast(1),
            (src.height * s).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun appLabel(pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        ""
    }

    fun playPause(): Boolean {
        val c = controller ?: return false
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
        return true
    }

    fun next(): Boolean {
        val c = controller ?: return false
        c.transportControls.skipToNext()
        return true
    }

    fun previous(): Boolean {
        val c = controller ?: return false
        c.transportControls.skipToPrevious()
        return true
    }

    /** Opens the app that's playing. Returns false if nothing is playing. */
    fun openPlayer(): Boolean {
        val c = controller ?: return false
        c.sessionActivity?.let { pi ->
            try {
                pi.send()
                return true
            } catch (e: Exception) {
                // fall through to the launch intent
            }
        }
        val launch = context.packageManager.getLaunchIntentForPackage(c.packageName) ?: return false
        return try {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }
}