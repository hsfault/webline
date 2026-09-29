package com.hsfault.webline.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hsfault.webline.data.Sky
import com.hsfault.webline.data.WeatherNow
import com.hsfault.webline.data.conditionText
import com.hsfault.webline.data.skyOf
import com.hsfault.webline.ui.components.UiIcon
import com.hsfault.webline.ui.components.drawUiIcon
import com.hsfault.webline.ui.theme.GlowSeg
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import com.hsfault.webline.ui.theme.hudCard
import com.hsfault.webline.ui.theme.hudCutPath
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val CLOCK_GLOWS = listOf(GlowSeg(5), GlowSeg(6, 0f, 0.55f), GlowSeg(7))
private val WEATHER_GLOWS = listOf(GlowSeg(1), GlowSeg(0, 0.8f, 1f), GlowSeg(2, 0f, 0.3f))
private val MUSIC_GLOWS = listOf(GlowSeg(2, 0.15f, 0.8f), GlowSeg(3))
private val SMOKE = listOf(
    Triple(0.80f, 0.86f, 0.34f),
    Triple(0.95f, 0.66f, 0.22f),
    Triple(0.60f, 1.00f, 0.26f),
    Triple(0.90f, 1.00f, 0.30f),
    Triple(0.72f, 0.70f, 0.14f),
)

// ---------- Clock card ----------

@Composable
fun ClockCard(k: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val is24 = DateFormat.is24HourFormat(context)
    val date = Date(now)
    fun fmt(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(date)

    val time = buildAnnotatedString {
        withStyle(SpanStyle(color = Hud.White)) { append(fmt(if (is24) "HH" else "hh") + ":") }
        withStyle(SpanStyle(color = Hud.Red)) { append(fmt("mm")) }
    }
    val dateStyle = HudType.date.copy(
        fontSize = (11 * k).sp,
        lineHeight = (15 * k).sp,
        letterSpacing = (3 * k).sp,
    )

    Box(
        modifier
            .hudCard(10.dp * k, 44.dp * k, 10.dp * k, 22.dp * k, glows = CLOCK_GLOWS)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 16.dp * k, vertical = 13.dp * k)
    ) {
        Column {
            BasicText(fmt("EEE").uppercase(), style = dateStyle)
            BasicText(fmt("MMM dd").uppercase(), style = dateStyle)
            BasicText(fmt("yyyy"), style = dateStyle)
            Spacer(Modifier.height(7.dp * k))
            Box(Modifier.size(22.dp * k, 2.dp * k).background(Hud.Red))
            Spacer(Modifier.height(4.dp * k))
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText(time, style = HudType.clock.copy(fontSize = (38 * k).sp, lineHeight = (42 * k).sp))
                if (!is24) {
                    Spacer(Modifier.width(3.dp * k))
                    BasicText(
                        text = fmt("a").uppercase(),
                        style = HudType.clockSuffix.copy(fontSize = (12 * k).sp),
                        modifier = Modifier.padding(bottom = 7.dp * k),
                    )
                }
            }
            Spacer(Modifier.height(5.dp * k))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = "BETTER DAYS AHEAD",
                    style = HudType.tagline.copy(fontSize = (7.5f * k).sp, letterSpacing = (3 * k).sp),
                )
                Spacer(Modifier.width(6.dp * k))
                Box(Modifier.size(16.dp * k, 1.5.dp * k).background(Hud.Red))
            }
        }
    }
}

// ---------- Weather card ----------

@Composable
fun WeatherCard(k: Float, weather: WeatherNow?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sky = weather?.let { skyOf(it.code) } ?: Sky.PARTLY
    val isDay = weather?.isDay ?: true

    Box(
        modifier
            .hudCard(20.dp * k, 34.dp * k, 12.dp * k, 12.dp * k, glows = WEATHER_GLOWS)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val clip = hudCutPath(size, (20.dp * k).toPx(), (34.dp * k).toPx(), (12.dp * k).toPx(), (12.dp * k).toPx())
                onDrawBehind {
                    clipPath(clip) {
                        SMOKE.forEach { (fx, fy, fr) ->
                            val c = Offset(fx * w, fy * h)
                            val r = fr * w
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(Hud.Red.copy(alpha = 0.42f), Hud.DeepRed.copy(alpha = 0.22f), Color.Transparent),
                                    center = c,
                                    radius = r,
                                ),
                                r,
                                c,
                            )
                        }
                    }
                }
            }
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 16.dp * k, vertical = 14.dp * k)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    Modifier
                        .size(42.dp * k)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                ) { drawWeatherIcon(sky, isDay) }
                Spacer(Modifier.width(8.dp * k))
                BasicText(
                    text = weather?.let { "${it.tempC}°" } ?: "--°",
                    style = HudType.temp.copy(fontSize = (30 * k).sp),
                )
            }
            Spacer(Modifier.height(4.dp * k))
            BasicText(
                text = weather?.let { conditionText(it.code) } ?: "Tap to refresh",
                style = HudType.cardTitle.copy(fontSize = (14 * k).sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Spacer(Modifier.height(5.dp * k))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(13.dp * k)) { drawUiIcon(UiIcon.PIN, Hud.Soft) }
                Spacer(Modifier.width(5.dp * k))
                BasicText(
                    text = weather?.place?.ifEmpty { null } ?: "Set city in Setup",
                    style = HudType.cardSub.copy(fontSize = (12 * k).sp, color = Hud.Soft),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun DrawScope.drawWeatherIcon(sky: Sky, isDay: Boolean) {
    val u = size.minDimension / 24f
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    val red = Color(0xFFE3202B)
    val cloudColor = Color(0xFFE8E8EC)

    if (sky == Sky.CLEAR || sky == Sky.PARTLY) {
        val big = sky == Sky.CLEAR
        val cx = if (big) 12f else 9.5f
        val cy = if (big) 12f else 9f
        val r = if (big) 5.2f else 4.3f
        drawCircle(red, r * u, o(cx, cy))
        if (isDay) {
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val ca = cos(a).toFloat()
                val sa = sin(a).toFloat()
                drawLine(
                    red,
                    o(cx + ca * (r + 1.5f), cy + sa * (r + 1.5f)),
                    o(cx + ca * (r + 3f), cy + sa * (r + 3f)),
                    1.6f * u,
                    cap = StrokeCap.Round,
                )
            }
        } else {
            // Crescent moon: cut a circle out of the red disc.
            drawCircle(Color.Black, r * 0.85f * u, o(cx + r * 0.55f, cy - r * 0.4f), blendMode = BlendMode.Clear)
        }
    }

    if (sky != Sky.CLEAR) {
        val dy = if (sky == Sky.RAIN || sky == Sky.STORM || sky == Sky.SNOW) -3f else 0f
        fun cloud(color: Color, grow: Float, mode: BlendMode) {
            drawRoundRect(
                color,
                o(5.5f - grow, 12.5f + dy - grow),
                Size((15.5f + grow * 2) * u, (7.5f + grow * 2) * u),
                CornerRadius((3.75f + grow) * u),
                blendMode = mode,
            )
            drawCircle(color, (4.6f + grow) * u, o(12.5f, 12.5f + dy), blendMode = mode)
            drawCircle(color, (3.4f + grow) * u, o(16.8f, 14f + dy), blendMode = mode)
        }
        cloud(Color.Black, 1.2f, BlendMode.Clear)
        cloud(cloudColor, 0f, BlendMode.SrcOver)

        when (sky) {
            Sky.RAIN -> for (x in listOf(9f, 13f, 17f)) {
                drawLine(red, o(x, 19.5f), o(x - 1.2f, 22.5f), 1.6f * u, cap = StrokeCap.Round)
            }
            Sky.STORM -> drawPath(
                Path().apply {
                    moveTo(13.5f * u, 17.5f * u)
                    lineTo(10.5f * u, 21f * u)
                    lineTo(12.8f * u, 21f * u)
                    lineTo(11.5f * u, 24f * u)
                    lineTo(15.5f * u, 19.8f * u)
                    lineTo(13f * u, 19.8f * u)
                    close()
                },
                red,
            )
            Sky.SNOW -> for (x in listOf(9f, 13f, 17f)) drawCircle(cloudColor, 1.1f * u, o(x, 21f))
            Sky.FOG -> {
                drawLine(Hud.Grey, o(6f, 21.5f), o(19f, 21.5f), 1.4f * u, cap = StrokeCap.Round)
                drawLine(Hud.Grey, o(8f, 23f), o(17f, 23f), 1.4f * u, cap = StrokeCap.Round)
            }
            else -> Unit
        }
    }
}

// ---------- Music card ----------

@Composable
fun MusicCard(k: Float, onOpen: () -> Unit, onMediaKey: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .hudCard(16.dp * k, 12.dp * k, 28.dp * k, 12.dp * k, glows = MUSIC_GLOWS)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onOpen)
            .padding(horizontal = 11.dp * k, vertical = 7.dp * k),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp * k)
                    .clip(RoundedCornerShape(10.dp * k))
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        onDrawBehind {
                            drawRect(Color(0xFF0B0B0E))
                            val c1 = Offset(w * 0.35f, h * 0.6f)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(Hud.Red.copy(alpha = 0.9f), Hud.DeepRed.copy(alpha = 0.5f), Color.Transparent),
                                    center = c1,
                                    radius = w * 0.6f,
                                ),
                                w * 0.6f,
                                c1,
                            )
                            val c2 = Offset(w * 0.7f, h * 0.3f)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFF6A6A).copy(alpha = 0.6f), Color.Transparent),
                                    center = c2,
                                    radius = w * 0.3f,
                                ),
                                w * 0.3f,
                                c2,
                            )
                            repeat(5) { i ->
                                drawLine(
                                    Hud.Glow.copy(alpha = 0.35f),
                                    Offset(w * (0.1f + i * 0.18f), h),
                                    Offset(w * (0.5f + i * 0.18f), 0f),
                                    1.dp.toPx(),
                                )
                            }
                        }
                    }
            )
            Spacer(Modifier.width(14.dp * k))
            Column {
                BasicText("Not Playing", style = HudType.cardTitle.copy(fontSize = (14 * k).sp))
                Spacer(Modifier.height(1.dp))
                BasicText("Tap to open music", style = HudType.cardSub.copy(fontSize = (11 * k).sp))
                Spacer(Modifier.height(4.dp * k))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MediaButton(UiIcon.PREV, 30.dp * k, 14.dp * k) { onMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
                    Spacer(Modifier.width(10.dp * k))
                    PlayButton(34.dp * k) { onMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
                    Spacer(Modifier.width(10.dp * k))
                    MediaButton(UiIcon.NEXT, 30.dp * k, 14.dp * k) { onMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) }
                }
            }
        }
    }
}

@Composable
private fun MediaButton(icon: UiIcon, box: Dp, glyph: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(box)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(glyph)) { drawUiIcon(icon, Hud.White) }
    }
}

@Composable
private fun PlayButton(box: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(box)
            .drawBehind {
                val r = size.minDimension / 2f - 2.dp.toPx()
                drawCircle(Hud.Glow.copy(alpha = 0.25f), r + 2.dp.toPx())
                drawCircle(Color(0xFF0B0B0E), r)
                drawCircle(Hud.Red, r, style = Stroke(1.6.dp.toPx()))
            }
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(box * 0.38f).offset(x = 1.dp)) { drawUiIcon(UiIcon.PLAY, Hud.Red) }
    }
}

// ---------- Quote ----------

@Composable
fun QuoteBlock(k: Float, lines: List<String>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Column(
            Modifier
                .drawBehind { drawRect(Hud.Red, size = Size(2.dp.toPx(), size.height)) }
                .padding(start = 18.dp * k, top = 4.dp * k, bottom = 4.dp * k)
        ) {
            lines.forEach { line ->
                BasicText(
                    text = line,
                    style = HudType.quote.copy(
                        fontSize = (11 * k).sp,
                        letterSpacing = (4.5f * k).sp,
                        lineHeight = (21 * k).sp,
                    ),
                )
            }
        }
        Spacer(Modifier.height(10.dp * k))
        Box(
            Modifier
                .padding(start = 18.dp * k)
                .size(22.dp * k, 2.dp * k)
                .background(Hud.Red)
        )
    }
}