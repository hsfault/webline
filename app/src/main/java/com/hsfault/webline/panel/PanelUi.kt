package com.hsfault.webline.panel

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsfault.webline.data.GlyphKind
import com.hsfault.webline.media.MediaRepository
import com.hsfault.webline.ui.components.drawGlyph
import com.hsfault.webline.ui.home.MusicCard
import com.hsfault.webline.ui.theme.ChakraPetch
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

enum class ToggleIcon { WIFI, DATA, BLUETOOTH, TORCH, VIBRATE, AIRPLANE, SUN, CHEVRON_UP, CHEVRON_RIGHT, SYSTEM }

private class ToggleSpec(
    val icon: ToggleIcon,
    val title: String,
    val on: Boolean,
    val state: String,
    val onClick: () -> Unit,
)

private val PanelShape = RoundedCornerShape(14.dp)
private val PanelFill = Brush.verticalGradient(listOf(Color(0xFF16161B), Color(0xFF0A0A0D)))
private val SheetFill = Brush.verticalGradient(listOf(Color(0xF7060609), Color(0xFA0A0A0D)))

/** Cheap rounded card (no bitmap rendering), used for everything in the panel. */
private fun Modifier.panelCard(accent: Boolean = false): Modifier =
    this.clip(PanelShape)
        .background(PanelFill)
        .border(1.dp, if (accent) Hud.Red.copy(alpha = 0.85f) else Hud.Line, PanelShape)

@Composable
fun PanelRoot(
    open: Boolean,
    media: MediaRepository,
    toggles: QuickToggles,
    statusBarPx: Int,
    actions: PanelActions,
    onHidden: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val latestHidden by rememberUpdatedState(onHidden)
    val scope = rememberCoroutineScope()
    var panelHeight by remember { mutableFloatStateOf(1f) }
    val statusBar = with(LocalDensity.current) { statusBarPx.toDp() }

    LaunchedEffect(open) {
        if (open) {
            progress.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 420f))
        } else {
            progress.animateTo(0f, tween(200))
            latestHidden()
        }
    }

    // Drag the panel up to close it (used on the header and the bottom handle).
    val dragToClose = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragEnd = {
                if (progress.value < 0.8f) actions.close()
                else scope.launch { progress.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 420f)) }
            },
            onDragCancel = { scope.launch { progress.animateTo(1f) } },
            onVerticalDrag = { change, dy ->
                change.consume()
                scope.launch { progress.snapTo((progress.value + dy / panelHeight).coerceIn(0f, 1f)) }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        // Dim the screen behind the panel (drawn directly, no offscreen layer).
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black.copy(alpha = 0.55f * progress.value)) }
                .pointerInput(Unit) { detectTapGestures { actions.close() } }
        )

        Column(
            Modifier
                .fillMaxSize()
                .onSizeChanged { panelHeight = it.height.toFloat().coerceAtLeast(1f) }
                .graphicsLayer { translationY = -(1f - progress.value) * size.height }
                .background(SheetFill)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(top = statusBar + 8.dp, start = 14.dp, end = 14.dp, bottom = 4.dp)
        ) {
            PanelHeader(open = open, actions = actions, modifier = Modifier.fillMaxWidth().then(dragToClose))
            Spacer(Modifier.height(12.dp))
            MusicCard(
                k = 1f,
                nowPlaying = media.nowPlaying,
                hasAccess = media.hasAccess,
                onOpen = actions.openPlayer,
                onPrev = actions.prev,
                onPlayPause = actions.playPause,
                onNext = actions.next,
                modifier = Modifier.fillMaxWidth().height(88.dp),
            )
            Spacer(Modifier.height(12.dp))
            ToggleGrid(toggles, actions)
            Spacer(Modifier.height(10.dp))
            BrightnessSlider(toggles.brightness, actions.brightness)
            Spacer(Modifier.height(14.dp))
            NotificationsHeader(count = NotificationStore.items.size, onClear = actions.clearAll)
            Spacer(Modifier.height(6.dp))
            NotificationList(
                notifications = NotificationStore.items,
                connected = NotificationStore.connected,
                actions = actions,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .then(dragToClose)
                    .clickable(remember { MutableInteractionSource() }, null) { actions.close() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(22.dp)) { drawToggleIcon(ToggleIcon.CHEVRON_UP, Hud.Red) }
            }
        }
    }
}

@Composable
private fun PanelHeader(open: Boolean, actions: PanelActions, modifier: Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(open) {
        while (open) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val is24 = DateFormat.is24HourFormat(context)
    val date = Date(now)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText(
                    text = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(date),
                    style = HudType.title.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.Bold, fontSize = 34.sp),
                )
                if (!is24) {
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        text = SimpleDateFormat("a", Locale.getDefault()).format(date).uppercase(),
                        style = HudType.cardSub.copy(fontSize = 13.sp, color = Hud.White),
                        modifier = Modifier.padding(bottom = 7.dp),
                    )
                }
            }
            BasicText(
                text = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(date),
                style = HudType.cardSub.copy(fontSize = 13.sp),
            )
        }
        RoundIconButton(onClick = actions.openSystemPanel) { drawToggleIcon(ToggleIcon.SYSTEM, Hud.Grey) }
        Spacer(Modifier.width(8.dp))
        RoundIconButton(onClick = actions.openSettings, offscreen = true) { drawGlyph(GlyphKind.SETTINGS, Hud.Grey) }
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, offscreen: Boolean = false, draw: DrawScope.() -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Hud.Surface)
            .border(1.dp, Hud.Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val layer = if (offscreen) Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen) else Modifier
        Canvas(modifier = Modifier.size(20.dp).then(layer), onDraw = draw)
    }
}

@Composable
private fun ToggleGrid(t: QuickToggles, a: PanelActions) {
    fun onOff(on: Boolean) = if (on) "On" else "Off"
    val specs = listOf(
        ToggleSpec(ToggleIcon.WIFI, "Wi-Fi", t.wifi, onOff(t.wifi), a.wifi),
        ToggleSpec(ToggleIcon.DATA, "Mobile Data", t.mobileData, if (t.mobileData) "In use" else "Tap to manage", a.mobileData),
        ToggleSpec(ToggleIcon.BLUETOOTH, "Bluetooth", t.bluetoothOn, onOff(t.bluetoothOn), a.bluetooth),
        ToggleSpec(ToggleIcon.TORCH, "Flashlight", t.torch, onOff(t.torch), a.torch),
        ToggleSpec(ToggleIcon.VIBRATE, "Vibrate", t.vibrate, onOff(t.vibrate), a.vibrate),
        ToggleSpec(ToggleIcon.AIRPLANE, "Airplane Mode", t.airplane, onOff(t.airplane), a.airplane),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        specs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { spec -> ToggleCard(spec, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ToggleCard(spec: ToggleSpec, modifier: Modifier) {
    Row(
        modifier
            .height(60.dp)
            .panelCard(accent = spec.on)
            .clickable(onClick = spec.onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (spec.on) Hud.Red else Hud.Surface)
                .border(1.dp, if (spec.on) Hud.Glow else Hud.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(18.dp)) { drawToggleIcon(spec.icon, if (spec.on) Hud.White else Hud.Grey) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(spec.title, style = HudType.cardTitle.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(spec.state, style = HudType.cardSub.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BrightnessSlider(value: Float, onChange: (Float) -> Unit) {
    val latest by rememberUpdatedState(onChange)
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .panelCard()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp)) { drawToggleIcon(ToggleIcon.SUN, Hud.Grey) }
        Spacer(Modifier.width(12.dp))
        Canvas(
            Modifier
                .weight(1f)
                .height(30.dp)
                .pointerInput(Unit) { detectTapGestures { latest((it.x / size.width).coerceIn(0f, 1f)) } }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        latest((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
        ) {
            val h = 10.dp.toPx()
            val top = (size.height - h) / 2f
            drawRoundRect(Hud.Line, Offset(0f, top), Size(size.width, h), CornerRadius(h / 2f))
            val w = (size.width * value).coerceAtLeast(h)
            drawRoundRect(Hud.Glow.copy(alpha = 0.25f), Offset(0f, top - 3.dp.toPx()), Size(w, h + 6.dp.toPx()), CornerRadius(h))
            drawRoundRect(Hud.Red, Offset(0f, top), Size(w, h), CornerRadius(h / 2f))
        }
        Spacer(Modifier.width(12.dp))
        Canvas(Modifier.size(22.dp)) { drawToggleIcon(ToggleIcon.SUN, Hud.White) }
    }
}

@Composable
private fun NotificationsHeader(count: Int, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText("Notifications", style = HudType.cardTitle.copy(fontSize = 15.sp))
        Spacer(Modifier.weight(1f))
        if (count > 0) {
            BasicText(
                text = "Clear all",
                style = HudType.cardSub.copy(fontSize = 13.sp, color = Hud.Red),
                modifier = Modifier
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun NotificationList(
    notifications: List<PanelNotification>,
    connected: Boolean,
    actions: PanelActions,
    modifier: Modifier,
) {
    if (notifications.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            BasicText(
                text = if (connected) "No notifications" else "Turn on Music Access in WEBLINE Setup to see notifications here",
                style = HudType.cardSub.copy(fontSize = 13.sp, textAlign = TextAlign.Center),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        items(notifications, key = { it.key }) { n ->
            NotificationCard(
                n = n,
                onOpen = { actions.openNotification(n) },
                onDismiss = { actions.dismissNotification(n) },
            )
        }
    }
}

@Composable
private fun NotificationCard(n: PanelNotification, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val offset = remember(n.key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width by remember { mutableFloatStateOf(1f) }
    val latestDismiss by rememberUpdatedState(onDismiss)

    Row(
        Modifier
            .fillMaxWidth()
            .height(74.dp)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationX = offset.value
                alpha = 1f - (abs(offset.value) / width).coerceIn(0f, 1f) * 0.7f
            }
            .pointerInput(n.key, n.clearable) {
                if (!n.clearable) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offset.value) > width * 0.35f) {
                                offset.animateTo(sign(offset.value) * width, tween(160))
                                latestDismiss()
                            } else {
                                offset.animateTo(0f, spring())
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + dx) }
                    },
                )
            }
            .panelCard()
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Hud.Surface),
            contentAlignment = Alignment.Center,
        ) {
            val icon = n.icon
            if (icon != null) Image(bitmap = icon, contentDescription = n.appName, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = "${n.appName} · ${panelAgo(n.postTime)}",
                style = HudType.cardSub.copy(fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(n.title, style = HudType.cardTitle.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (n.text.isNotBlank()) {
                BasicText(
                    text = n.text,
                    style = HudType.cardSub.copy(fontSize = 12.sp, color = Hud.Soft),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Canvas(Modifier.size(16.dp)) { drawToggleIcon(ToggleIcon.CHEVRON_RIGHT, Hud.Grey) }
    }
}

private fun panelAgo(time: Long): String {
    val minutes = (System.currentTimeMillis() - time) / 60_000
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 1_440 -> "${minutes / 60}h"
        else -> "${minutes / 1_440}d"
    }
}

// ---------- Icons (24-unit grid) ----------

private const val PLANE =
    "M12 2 C13 2 13.5 3 13.5 4 L13.5 9 L21 13.5 L21 15.5 L13.5 13 L13.5 18 L16 20 L16 21.5 " +
        "L12 20.5 L8 21.5 L8 20 L10.5 18 L10.5 13 L3 15.5 L3 13.5 L10.5 9 L10.5 4 C10.5 3 11 2 12 2 Z"

private val iconPaths = HashMap<String, Path>()
private fun iconPath(d: String): Path = iconPaths.getOrPut(d) { PathParser().parsePathString(d).toPath() }

fun DrawScope.drawToggleIcon(icon: ToggleIcon, color: Color) {
    val u = size.minDimension / 24f
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun svg(d: String, fill: Boolean) {
        val shape = iconPath(d)
        scale(u, u, Offset.Zero) {
            if (fill) {
                drawPath(shape, color)
            } else {
                drawPath(shape, color, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }

    when (icon) {
        ToggleIcon.WIFI -> {
            for (r in listOf(4f, 8f, 12f)) {
                drawArc(color, 225f, 90f, false, o(12f - r, 19f - r), Size(2f * r * u, 2f * r * u), style = stroke)
            }
            drawCircle(color, 1.6f * u, o(12f, 19f))
        }
        ToggleIcon.DATA -> {
            listOf(5f, 9f, 13f, 17f).forEachIndexed { i, h ->
                drawRoundRect(color, o(3.5f + i * 4.8f, 20f - h), Size(3f * u, h * u), CornerRadius(1f * u))
            }
        }
        ToggleIcon.BLUETOOTH -> svg("M7 7 L17 17 L12 21.5 L12 2.5 L17 7 L7 17", fill = false)
        ToggleIcon.TORCH -> svg("M13.5 2 L5.5 13 L11 13 L10 22 L18.5 10.5 L13 10.5 Z", fill = true)
        ToggleIcon.VIBRATE -> {
            drawRoundRect(color, o(8f, 3.5f), Size(8f * u, 17f * u), CornerRadius(2f * u), style = stroke)
            drawLine(color, o(4f, 9f), o(4f, 15f), 2f * u, StrokeCap.Round)
            drawLine(color, o(20f, 9f), o(20f, 15f), 2f * u, StrokeCap.Round)
        }
        ToggleIcon.AIRPLANE -> svg(PLANE, fill = true)
        ToggleIcon.SUN -> {
            drawCircle(color, 4f * u, o(12f, 12f))
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val c = cos(a).toFloat()
                val s = sin(a).toFloat()
                drawLine(color, o(12f + c * 6.5f, 12f + s * 6.5f), o(12f + c * 9f, 12f + s * 9f), 2f * u, StrokeCap.Round)
            }
        }
        ToggleIcon.CHEVRON_UP -> svg("M6 15 L12 9 L18 15", fill = false)
        ToggleIcon.CHEVRON_RIGHT -> svg("M9 6 L15 12 L9 18", fill = false)
        ToggleIcon.SYSTEM -> {
            for (r in 0..1) {
                for (c in 0..1) {
                    drawRoundRect(
                        color,
                        o(4f + c * 9f, 4f + r * 9f),
                        Size(7f * u, 7f * u),
                        CornerRadius(1.8f * u),
                        style = Stroke(1.8f * u),
                    )
                }
            }
        }
    }
}