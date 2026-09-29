package com.hsfault.webline

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.hsfault.webline.data.WeatherRepository
import com.hsfault.webline.ui.home.HomeActions
import com.hsfault.webline.ui.home.HomeScreen
import com.hsfault.webline.util.SystemActions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repo: AppRepository
    private lateinit var layout: LayoutStore
    private lateinit var weather: WeatherRepository
    private var drawerOpen by mutableStateOf(false)
    private var homeSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        repo = AppRepository(this)
        layout = LayoutStore(this)
        weather = WeatherRepository(this)
        repo.startWatching { lifecycleScope.launch { refresh() } }
        lifecycleScope.launch { refresh() }

        val actions = HomeActions(
            launch = { app ->
                repo.launch(app)
                drawerOpen = false
            },
            appInfo = { app -> repo.openAppInfo(app) },
            swipeDown = { SystemActions.expandNotifications(this) },
            openClock = { SystemActions.openClock(this) },
            refreshWeather = { lifecycleScope.launch { weather.refresh(force = true) } },
            openMusic = {
                if (!SystemActions.openMusic(this)) {
                    Toast.makeText(this, "No music app found", Toast.LENGTH_SHORT).show()
                }
            },
            mediaKey = { code -> SystemActions.mediaKey(this, code) },
            voiceSearch = { SystemActions.voiceSearch(this) },
        )

        setContent {
            HomeScreen(
                apps = repo.apps,
                layout = layout,
                weather = weather.now,
                drawerOpen = drawerOpen,
                homeSignal = homeSignal,
                onDrawerOpenChange = { drawerOpen = it },
                actions = actions,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { weather.refresh() }
    }

    private suspend fun refresh() {
        repo.reload()
        val installed = repo.apps.mapTo(HashSet()) { it.key }
        if (installed.isEmpty()) return
        if (!layout.initialized) Defaults.seed(this, repo.apps, layout)
        layout.prune(installed)
    }

    override fun onNewIntent(intent: Intent) {
        val alreadyHome = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        super.onNewIntent(intent)
        drawerOpen = false
        if (alreadyHome) homeSignal++
    }

    override fun onDestroy() {
        repo.stopWatching()
        super.onDestroy()
    }
}