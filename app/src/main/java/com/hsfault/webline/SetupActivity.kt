package com.hsfault.webline

import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.hsfault.webline.data.UserPrefs
import com.hsfault.webline.data.WeatherRepository
import com.hsfault.webline.ui.components.HudButton
import com.hsfault.webline.ui.theme.ChakraPetch
import com.hsfault.webline.ui.theme.GlowSeg
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import com.hsfault.webline.ui.theme.hudCard
import com.hsfault.webline.util.SystemActions
import com.hsfault.webline.wallpaper.WallpaperSetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CARD_GLOWS = listOf(GlowSeg(7), GlowSeg(1))

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
    val weather = remember { WeatherRepository(context) }
    val user = remember { UserPrefs(context) }
    val isHome = remember(resumeTick) { SystemActions.isDefaultHome(context) }
    val batteryFree = remember(resumeTick) { SystemActions.isIgnoringBattery(context) }
    val musicAccess = remember(resumeTick) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    var wallpaperStatus by remember { mutableStateOf("NOT SET") }
    var name by remember { mutableStateOf(user.name) }
    var nameStatus by remember { mutableStateOf(user.name.uppercase()) }
    var city by remember { mutableStateOf(weather.city) }
    var cityStatus by remember { mutableStateOf(weather.city.uppercase()) }

    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (!SystemActions.isDefaultHome(context)) SystemActions.openHomeSettings(context)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            wallpaperStatus = "APPLYING"
            scope.launch {
                wallpaperStatus = try {
                    withContext(Dispatchers.IO) { WallpaperSetter.apply(context, uri) }
                    "APPLIED"
                } catch (e: Exception) {
                    "FAILED"
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Hud.Bg)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        BasicText(
            text = "WEBLINE",
            style = HudType.title.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 6.sp),
        )
        Spacer(Modifier.height(4.dp))
        BasicText("SETUP // V2", style = HudType.header)
        Spacer(Modifier.height(24.dp))

        SetupCard(
            step = "01",
            title = "SET AS HOME",
            status = if (isHome) "ACTIVE" else "NOT SET",
            ok = isHome,
            body = "Makes WEBLINE your home screen. To undo: Settings → Apps → Default apps → Home app → XOS launcher.",
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
            title = "WALLPAPER",
            status = wallpaperStatus,
            ok = wallpaperStatus == "APPLIED",
            body = "Pick your wallpaper image. It's cropped to fill your screen and set on the home and lock screen.",
        ) {
            HudButton(
                text = if (wallpaperStatus == "APPLYING") "APPLYING…" else "CHOOSE IMAGE",
                enabled = wallpaperStatus != "APPLYING",
            ) { imagePicker.launch("image/*") }
        }

        SetupCard(
            step = "03",
            title = "YOUR NAME",
            status = nameStatus,
            ok = true,
            body = "Shown on the greeting card on the second page.",
        ) {
            TextFieldBox(value = name, placeholder = "Your name") { name = it }
            HudButton("SAVE NAME") {
                user.name = name
                nameStatus = user.name.uppercase()
            }
        }

        SetupCard(
            step = "04",
            title = "WEATHER CITY",
            status = cityStatus,
            ok = true,
            body = "Weather comes from Open-Meteo (free, no account). Type your city and save.",
        ) {
            TextFieldBox(value = city, placeholder = "City name") { city = it }
            HudButton("SAVE CITY") {
                weather.setCity(city)
                cityStatus = city.trim().uppercase().ifEmpty { WeatherRepository.DEFAULT_CITY.uppercase() }
                scope.launch {
                    weather.refresh(force = true)
                    weather.now?.let { cityStatus = "${it.place.uppercase()} · ${it.tempC}°" }
                }
            }
        }

        SetupCard(
            step = "05",
            title = "MUSIC ACCESS",
            status = if (musicAccess) "GRANTED" else "OFF",
            ok = musicAccess,
            body = "Turn on WEBLINE in Notification access so the music card can show and control the song that's playing. The notification panel will use it too.",
        ) {
            HudButton(if (musicAccess) "MANAGE ACCESS" else "ALLOW ACCESS") {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Open Settings → Notifications → Notification access", Toast.LENGTH_LONG).show()
                }
            }
        }

        SetupCard(
            step = "06",
            title = "KEEP ALIVE ON XOS",
            status = if (batteryFree) "UNRESTRICTED" else "RESTRICTED",
            ok = batteryFree,
            body = "XOS closes background apps aggressively. Turn off battery optimization, then in Phone Master allow WEBLINE to auto-start.",
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
        Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .hudCard(12.dp, 26.dp, 12.dp, 12.dp, glows = CARD_GLOWS)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(step, style = HudType.header)
            Spacer(Modifier.width(10.dp))
            BasicText(title, style = HudType.menu, modifier = Modifier.weight(1f))
            BasicText(status, style = HudType.header.copy(color = if (ok) Hud.Glow else Hud.Grey), maxLines = 1)
        }
        BasicText(body, style = HudType.cardSub.copy(fontSize = 13.sp, lineHeight = 18.sp))
        actions()
    }
}

@Composable
private fun TextFieldBox(value: String, placeholder: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = HudType.body,
        cursorBrush = SolidColor(Hud.Red),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(shape)
                    .background(Hud.Surface)
                    .border(1.dp, Hud.Line, shape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) BasicText(placeholder, style = HudType.body.copy(color = Hud.Grey))
                inner()
            }
        },
    )
}