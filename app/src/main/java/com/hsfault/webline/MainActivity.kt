package com.hsfault.webline

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.hsfault.webline.data.AppRepository
import com.hsfault.webline.data.Defaults
import com.hsfault.webline.data.LayoutStore
import com.hsfault.webline.data.UserPrefs
import com.hsfault.webline.data.WeatherRepository
import com.hsfault.webline.media.MediaRepository
import com.hsfault.webline.panel.PanelService
import com.hsfault.webline.ui.home.HomeActions
import com.hsfault.webline.ui.home.HomeScreen
import com.hsfault.webline.util.SystemActions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repo: AppRepository
    private lateinit var layout: LayoutStore
    private lateinit var weather: WeatherRepository
    private lateinit var media: MediaRepository
    private lateinit var user: UserPrefs
    private var drawerOpen by mutableStateOf(false)
    private var homeSignal by mutableIntStateOf(0)
    private var userName by mutableStateOf(UserPrefs.DEFAULT_NAME)
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        repo = AppRepository(this)
        layout = LayoutStore(this)
        weather = WeatherRepository(this)
        media = MediaRepository(this)
        user = UserPrefs(this)
        userName = user.name

        repo.startWatching { scheduleRefresh() }
        lifecycleScope.launch { refresh() }

        val actions = HomeActions(
            launch = { app ->
                repo.launch(app)
                drawerOpen = false
            },
            appInfo = { app -> repo.openAppInfo(app) },
            // Our panel if the WEBLINE Panel service is on, otherwise the system shade.
            swipeDown = { PanelService.instance?.openPanel() ?: SystemActions.expandNotifications(this) },
            openClock = { SystemActions.openClock(this) },
            refreshWeather = { lifecycleScope.launch { weather.refresh(force = true) } },
            openMusic = {
                if (!media.openPlayer() && !SystemActions.openMusic(this)) {
                    Toast.makeText(this, "No music app found", Toast.LENGTH_SHORT).show()
                }
            },
            musicPrev = {
                if (!media.previous()) SystemActions.mediaKey(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            },
            musicPlayPause = {
                if (!media.playPause()) SystemActions.mediaKey(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            },
            musicNext = {
                if (!media.next()) SystemActions.mediaKey(this, KeyEvent.KEYCODE_MEDIA_NEXT)
            },
            openStorage = { openStorageSettings() },
            voiceSearch = { SystemActions.voiceSearch(this) },
        )

        setContent {
            HomeScreen(
                apps = repo.apps,
                layout = layout,
                weather = weather.now,
                nowPlaying = media.nowPlaying,
                hasMusicAccess = media.hasAccess,
                userName = userName,
                drawerOpen = drawerOpen,
                homeSignal = homeSignal,
                onDrawerOpenChange = { drawerOpen = it },
                actions = actions,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        userName = user.name
        media.start()
        lifecycleScope.launch { weather.refresh() }
    }

    /** Package events often arrive in bursts; wait for them to settle, then refresh once. */
    private fun scheduleRefresh() {
        refreshJob?.cancel()
        refreshJob = lifecycleScope.launch {
            delay(700)
            refresh()
        }
    }

    private suspend fun refresh() {
        repo.reload()
        val installed = repo.apps.mapTo(HashSet()) { it.key }
        if (installed.isEmpty()) return
        if (!layout.initialized) Defaults.seed(this, repo.apps, layout)
        layout.prune(installed)
    }

    private fun openStorageSettings() {
        try {
            startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (ignored: Exception) {
                // nothing to open
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        val alreadyHome = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        super.onNewIntent(intent)
        drawerOpen = false
        if (alreadyHome) homeSignal++
    }

    override fun onDestroy() {
        repo.stopWatching()
        media.stop()
        super.onDestroy()
    }
}