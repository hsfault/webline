package com.hsfault.webline.panel

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsfault.webline.data.WeatherNow
import com.hsfault.webline.data.conditionText
import com.hsfault.webline.media.MediaRepository
import com.hsfault.webline.media.NowPlaying
import com.hsfault.webline.ui.components.UiIcon
import com.hsfault.webline.ui.components.drawUiIcon
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Everything the lock cover shows; owned by PanelService. */
class LockState {
    var visible by mutableStateOf(false)
    var splitting by mutableStateOf(false)
    var screenOn by mutableStateOf(true)
    var emblem by mutableStateOf<ImageBitmap?>(null)
    var battery by mutableIntStateOf(0)
    var charging by mutableStateOf(false)
    var weather by mutableStateOf<WeatherNow?>(null)
    var showToken by mutableIntStateOf(0)
}

private val SurfacePill = Color(0xD90E0E12)

/**
 * The cover. On unlock it draws itself twice: the left half clipped and sliding left,
 * the right half clipped and sliding right, each with a glowing red cut edge.
 */
@Composable
fun LockCoverUi(
    state: LockState,
    media: MediaRepository,
    statusBarPx: Int,
    navBarPx: Int,
    onSwipeUp: () -> Unit,
    onSplitDone: () -> Unit,
    onDoubleTap: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onTick: () -> Unit,
) {
    val density = LocalDensity.current
    val topPad = with(density) { statusBarPx.toDp() }
    val bottomPad = with(density) { navBarPx.toDp() }
    val split = remember { Animatable(0f) }
    val drag = remember { Animatable(0f) }
    val pulse = remember { Animatable(0.45f) }
    val scope = rememberCoroutineScope()
    val latestSwipe by rememberUpdatedState(onSwipeUp)
    val latestDone by rememberUpdatedState(onSplitDone)
    val latestDoubleTap by rememberUpdatedState(onDoubleTap)
    val latestTick by rememberUpdatedState(onTick)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Fresh cover each time it's shown.
    LaunchedEffect(state.showToken) {
        split.snapTo(0f)
        drag.snapTo(0f)
    }

    // Unlock: split apart, then tell the service to hide the window.
    LaunchedEffect(state.splitting) {
        if (state.splitting) {
            split.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
            latestDone()
        }
    }

    // Clock/battery refresh and the glow pulse only run while the cover is actually on screen.
    val live = state.visible && state.screenOn && !state.splitting
    LaunchedEffect(live) {
        if (!live) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            latestTick()
            delay(10_000)
        }
    }
    LaunchedEffect(live) {
        if (!live) return@LaunchedEffect
        while (true) {
            pulse.animateTo(0.85f, tween(1800))
            pulse.animateTo(0.45f, tween(1800))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { latestDoubleTap() })
            }
            .pointerInput(Unit) {
                val threshold = size.height * 0.16f
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (-drag.value > threshold) latestSwipe()
                        scope.launch { drag.animateTo(0f, spring()) }
                    },
                    onDragCancel = { scope.launch { drag.animateTo(0f) } },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        scope.launch { drag.snapTo((drag.value + dy).coerceAtMost(0f)) }
                    },
                )
            }
            .drawWithContent {
                val p = split.value
                if (p <= 0f) {
                    drawContent()
                    return@drawWithContent
                }
                val half = size.width / 2f
                val dx = (half + 24.dp.toPx()) * p
                val glowWidth = 10.dp.toPx()
                val coreWidth = 2.dp.toPx()

                translate(left = -dx) {
                    clipRect(0f, 0f, half, size.height) { this@drawWithContent.drawContent() }
                    drawLine(Hud.Glow.copy(alpha = 0.35f), Offset(half, 0f), Offset(half, size.height), glowWidth)
                    drawLine(Hud.Glow, Offset(half, 0f), Offset(half, size.height), coreWidth)
                }
                translate(left = dx) {
                    clipRect(half, 0f, size.width, size.height) { this@drawWithContent.drawContent() }
                    drawLine(Hud.Glow.copy(alpha = 0.35f), Offset(half, 0f), Offset(half, size.height), glowWidth)
                    drawLine(Hud.Glow, Offset(half, 0f), Offset(half, size.height), coreWidth)
                }
            }
    ) {
        // Painted into one cached layer, so the split just slides two copies of an image.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    translationY = drag.value * 0.35f
                }
        ) {
            CoverContent(
                state = state,
                nowPlaying = media.nowPlaying,
                notificationCount = NotificationStore.items.size,
                now = now,
                pulse = { pulse.value },
                topPad = topPad,
                bottomPad = bottomPad,
                onPlayPause = onPlayPause,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun CoverContent(
    state: LockState,
    nowPlaying: NowPlaying?,
    notificationCount: Int,
    now: Long,
    pulse: () -> Float,
    topPad: Dp,
    bottomPad: Dp,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val date = Date(now)
    fun fmt(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    val time = buildAnnotatedString {
        withStyle(SpanStyle(color = Hud.White)) { append(fmt(if (is24) "HH" else "hh") + ":") }
        withStyle(SpanStyle(color = Hud.Red)) { append(fmt("mm")) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val bg = Brush.verticalGradient(listOf(Color(0xFF050507), Color(0xFF09030A), Color(0xFF120207)))
                val haze = Brush.radialGradient(
                    listOf(Hud.Red.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(w / 2f, h * 0.97f),
                    radius = w * 0.9f,
                )
                val seam = Brush.verticalGradient(listOf(Color.Transparent, Hud.Red.copy(alpha = 0.35f), Color.Transparent))
                val stroke = 1.2.dp.toPx()
                val seamWidth = 1.5.dp.toPx()
                onDrawBehind {
                    drawRect(bg)
                    drawRect(haze)
                    // The split seam
                    drawRect(seam, topLeft = Offset(w / 2f - seamWidth / 2f, 0f), size = Size(seamWidth, h))
                    // Angular red light lines
                    val strong = Hud.Red.copy(alpha = 0.45f)
                    val soft = Hud.Red.copy(alpha = 0.22f)
                    drawLine(strong, Offset(0f, h * 0.70f), Offset(w / 2f, h * 0.86f), stroke)
                    drawLine(strong, Offset(w, h * 0.70f), Offset(w / 2f, h * 0.86f), stroke)
                    drawLine(soft, Offset(0f, h * 0.18f), Offset(w / 2f, h * 0.30f), stroke)
                    drawLine(soft, Offset(w, h * 0.18f), Offset(w / 2f, h * 0.30f), stroke)
                }
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = topPad + 34.dp, bottom = bottomPad + 22.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = fmt("EEEE · dd MMMM").uppercase(),
                style = HudType.date.copy(fontSize = 12.sp, letterSpacing = 4.sp, color = Hud.Soft),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText(time, style = HudType.clock.copy(fontSize = 76.sp, lineHeight = 80.sp))
                if (!is24) {
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        text = fmt("a").uppercase(),
                        style = HudType.clockSuffix.copy(fontSize = 16.sp),
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Emblem(state.emblem, pulse)
            Spacer(Modifier.weight(1f))

            if (nowPlaying != null) {
                MusicPill(nowPlaying, onPlayPause, onNext)
                Spacer(Modifier.height(16.dp))
            }

            InfoRow(state.weather, notificationCount, state.battery, state.charging)
            Spacer(Modifier.height(20.dp))
            UnlockHint(pulse)
        }
    }
}

@Composable
private fun Emblem(emblem: ImageBitmap?, pulse: () -> Float) {
    Box(
        Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(1f)
            .drawBehind {
                val a = pulse().coerceIn(0f, 1f)
                val r = size.minDimension * 0.62f
                drawCircle(
                    Brush.radialGradient(
                        listOf(Hud.Red.copy(alpha = 0.55f * a), Hud.DeepRed.copy(alpha = 0.25f * a), Color.Transparent),
                        center = center,
                        radius = r,
                    ),
                    r,
                    center,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (emblem != null) {
            Image(
                bitmap = emblem,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.86f),
            )
        } else {
            BasicText(
                text = "Pick your emblem in\nWEBLINE Setup → Lock Cover",
                style = HudType.cardSub.copy(fontSize = 13.sp, textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun MusicPill(np: NowPlaying, onPlayPause: () -> Unit, onNext: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(SurfacePill)
            .border(1.dp, Hud.Line, shape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val art = np.art
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Hud.Surface),
        ) {
            if (art != null) {
                Image(bitmap = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(np.title, style = HudType.cardTitle.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(np.artist, style = HudType.cardSub.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.6.dp, Hud.Red, CircleShape)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            if (np.playing) {
                Canvas(Modifier.size(13.dp)) {
                    val barW = size.width * 0.3f
                    drawRoundRect(Hud.Red, Offset(size.width * 0.1f, 0f), Size(barW, size.height), CornerRadius(barW / 3f))
                    drawRoundRect(Hud.Red, Offset(size.width * 0.6f, 0f), Size(barW, size.height), CornerRadius(barW / 3f))
                }
            } else {
                Canvas(Modifier.size(14.dp)) { drawUiIcon(UiIcon.PLAY, Hud.Red) }
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(14.dp)) { drawUiIcon(UiIcon.NEXT, Hud.White) }
        }
    }
}

@Composable
private fun InfoRow(weather: WeatherNow?, notificationCount: Int, battery: Int, charging: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (weather != null) {
                Column {
                    BasicText("${weather.tempC}°", style = HudType.temp.copy(fontSize = 22.sp))
                    BasicText(conditionText(weather.code), style = HudType.cardSub.copy(fontSize = 11.sp), maxLines = 1)
                }
            }
        }
        Box(Modifier.weight(1.3f), contentAlignment = Alignment.Center) {
            if (notificationCount > 0) {
                val shape = RoundedCornerShape(12.dp)
                Row(
                    Modifier
                        .clip(shape)
                        .background(SurfacePill)
                        .border(1.dp, Hud.Line, shape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Hud.Glow)
                    )
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        text = if (notificationCount == 1) "1 NOTIFICATION" else "$notificationCount NOTIFICATIONS",
                        style = HudType.header.copy(fontSize = 9.sp, letterSpacing = 1.5.sp, color = Hud.Soft),
                        maxLines = 1,
                    )
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BatteryIcon(battery, charging)
                    Spacer(Modifier.width(6.dp))
                    BasicText("$battery%", style = HudType.cardTitle.copy(fontSize = 14.sp))
                }
                if (charging) {
                    BasicText("CHARGING", style = HudType.header.copy(fontSize = 9.sp, letterSpacing = 1.5.sp))
                }
            }
        }
    }
}

@Composable
private fun BatteryIcon(level: Int, charging: Boolean) {
    Canvas(Modifier.size(width = 24.dp, height = 12.dp)) {
        val nub = 2.dp.toPx()
        val bodyW = size.width - nub - 1.dp.toPx()
        drawRoundRect(
            Hud.Soft,
            Offset.Zero,
            Size(bodyW, size.height),
            CornerRadius(3.dp.toPx()),
            style = Stroke(1.4.dp.toPx()),
        )
        drawRoundRect(
            Hud.Soft,
            Offset(bodyW + 1.dp.toPx(), size.height * 0.3f),
            Size(nub, size.height * 0.4f),
            CornerRadius(1.dp.toPx()),
        )
        val inset = 2.5.dp.toPx()
        val fillW = (bodyW - inset * 2) * (level.coerceIn(0, 100) / 100f)
        val fill = when {
            level <= 15 -> Hud.Glow
            charging -> Hud.Red
            else -> Hud.White
        }
        drawRoundRect(fill, Offset(inset, inset), Size(fillW, size.height - inset * 2), CornerRadius(1.5.dp.toPx()))
    }
}

@Composable
private fun UnlockHint(pulse: () -> Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(22.dp)
                .graphicsLayer { translationY = -(pulse() - 0.45f) * 18f }
        ) { drawToggleIcon(ToggleIcon.CHEVRON_UP, Hud.Red) }
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = "SWIPE UP OR USE FINGERPRINT",
            style = HudType.header.copy(fontSize = 10.sp, letterSpacing = 3.sp, color = Hud.Grey),
        )
    }
}