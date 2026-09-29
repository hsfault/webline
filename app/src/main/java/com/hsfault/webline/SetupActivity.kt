package com.hsfault.webline

import android.app.role.RoleManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsfault.webline.ui.components.HudButton
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import com.hsfault.webline.ui.theme.hudFrame
import com.hsfault.webline.util.SystemActions
import com.hsfault.webline.wallpaper.WebWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : ComponentActivity() {

    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { SetupScreen(resumeTick) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
private fun SetupScreen(resumeTick: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isHome = remember(resumeTick) { SystemActions.isDefaultHome(context) }
    val batteryFree = remember(resumeTick) { SystemActions.isIgnoringBattery(context) }
    var wallpaperStatus by remember { mutableStateOf("NOT APPLIED") }
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!SystemActions.isDefaultHome(context)) SystemActions.openHomeSettings(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Hud.Void)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        BasicText("WEBLINE", style = HudType.title)
        Spacer(Modifier.height(4.dp))
        BasicText("SETUP // PHASE 1", style = HudType.label.copy(color = Hud.Crimson))
        Spacer(Modifier.height(24.dp))

        SetupCard(
            step = "01",
            title = "SET AS HOME",
            status = if (isHome) "ACTIVE" else "NOT SET",
            ok = isHome,
            body = "Makes WEBLINE your home screen. To undo, go to Settings → Apps → Default apps → Home app and pick the XOS launcher.",
        ) {
            if (isHome) {
                HudButton("OPEN HOME") { SystemActions.goHome(context) }
            } else {
                HudButton("SET DEFAULT") {
                    val roles = context.getSystemService(RoleManager::class.java)
                    if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                        roleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    } else {
                        SystemActions.openHomeSettings(context)
                    }
                }
            }
        }

        SetupCard(
            step = "02",
            title = "WEB WALLPAPER",
            status = wallpaperStatus,
            ok = wallpaperStatus == "APPLIED",
            body = "Draws the radial web, spider and glowing pod at your exact screen size and sets it on the home and lock screen.",
        ) {
            HudButton(
                text = if (wallpaperStatus == "APPLIED") "APPLY AGAIN" else "APPLY WALLPAPER",
                enabled = wallpaperStatus != "APPLYING",
            ) {
                wallpaperStatus = "APPLYING"
                scope.launch {
                    wallpaperStatus = try {
                        withContext(Dispatchers.IO) { WebWallpaper.apply(context) }
                        "APPLIED"
                    } catch (e: Exception) {
                        "FAILED"
                    }
                }
            }
        }

        SetupCard(
            step = "03",
            title = "KEEP ALIVE ON XOS",
            status = if (batteryFree) "UNRESTRICTED" else "RESTRICTED",
            ok = batteryFree,
            body = "XOS closes background apps aggressively. Turn off battery optimization here, then in Phone Master allow WEBLINE to auto-start. This matters most once the notification panel arrives in Phase 4.",
        ) {
            HudButton("BATTERY OPTIMIZATION", enabled = !batteryFree) {
                SystemActions.requestIgnoreBattery(context)
            }
            HudButton("OPEN PHONE MASTER") {
                if (!SystemActions.openPhoneMaster(context)) {
                    Toast.makeText(context, "Phone Master not found. Open it from your app list.", Toast.LENGTH_LONG).show()
                }
            }
            HudButton("APP INFO") { SystemActions.openOwnAppInfo(context) }
        }
    }
}

@Composable
private fun SetupCard(
    step: String,
    title: String,
    status: String,
    ok: Boolean,
    body: String,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .hudFrame(cut = 16.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(step, style = HudType.label.copy(color = Hud.Crimson))
            Spacer(Modifier.width(10.dp))
            BasicText(title, style = HudType.action, modifier = Modifier.weight(1f))
            BasicText(status, style = HudType.label.copy(color = if (ok) Hud.Glow else Hud.Muted))
        }
        BasicText(
            text = body,
            style = HudType.body.copy(fontSize = 13.sp, color = Hud.Muted, letterSpacing = 0.2.sp),
        )
        actions()
    }
}