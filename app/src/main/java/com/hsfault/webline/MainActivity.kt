package com.hsfault.webline

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.hsfault.webline.ui.home.HomeScreen
import com.hsfault.webline.util.SystemActions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repo: AppRepository
    private lateinit var layout: LayoutStore
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
        repo.startWatching { lifecycleScope.launch { refresh() } }
        lifecycleScope.launch { refresh() }

        setContent {
            HomeScreen(
                apps = repo.apps,
                layout = layout,
                drawerOpen = drawerOpen,
                homeSignal = homeSignal,
                onDrawerOpenChange = { drawerOpen = it },
                onLaunch = { app ->
                    repo.launch(app)
                    drawerOpen = false
                },
                onAppInfo = { app -> repo.openAppInfo(app) },
                onSwipeDown = { SystemActions.expandNotifications(this) },
            )
        }
    }

    private suspend fun refresh() {
        repo.reload()
        val installed = repo.apps.mapTo(HashSet()) { it.key }
        if (installed.isEmpty()) return
        if (!layout.initialized) {
            val dock = Defaults.dock(this, repo.apps)
            layout.seed(home = Defaults.home(this, repo.apps, exclude = dock.toSet()), dock = dock)
        }
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