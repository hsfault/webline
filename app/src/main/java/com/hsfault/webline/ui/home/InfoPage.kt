package com.hsfault.webline.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsfault.webline.data.AppEntry
import com.hsfault.webline.data.DeviceStats
import com.hsfault.webline.data.gb
import com.hsfault.webline.ui.components.HudTile
import com.hsfault.webline.ui.components.UiIcon
import com.hsfault.webline.ui.components.drawUiIcon
import com.hsfault.webline.ui.theme.GlowSeg
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import com.hsfault.webline.ui.theme.hudCard
import com.hsfault.webline.ui.theme.hudCutPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val QUOTES = listOf(
    "Keep going, great things take time.",
    "Small steps every day add up.",
    "Focus on progress, not perfection.",
    "Build it once, build it right.",
    "Stay curious. Keep shipping.",
    "Discipline beats motivation.",
    "Every expert was once a beginner.",
    "Make your future self proud.",
)

/** Skyline buildings: x, width, height (fractions of the greeting card). */
private val BUILDINGS = listOf(
    Triple(0.52f, 0.05f, 0.35f), Triple(0.57f, 0.04f, 0.55f), Triple(0.61f, 0.06f, 0.42f),
    Triple(0.67f, 0.035f, 0.72f), Triple(0.705f, 0.05f, 0.50f), Triple(0.755f, 0.045f, 0.62f),
    Triple(0.80f, 0.06f, 0.38f), Triple(0.86f, 0.04f, 0.58f), Triple(0.90f, 0.05f, 0.45f),
    Triple(0.95f, 0.06f, 0.30f),
)

private val GREETING_GLOWS = listOf(GlowSeg(1), GlowSeg(6, 0.2f, 0.8f))
private val RECENT_GLOWS = listOf(GlowSeg(3), GlowSeg(7))
private val STATS_GLOWS = listOf(GlowSeg(5), GlowSeg(1))

/** The second page: search, greeting, recently installed apps, storage and RAM. */
@Composable
fun InfoPage(
    geo: Geo,
    userName: String,
    recent: List<AppEntry>,
    visible: Boolean,
    onSearch: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onStorage: () -> Unit,
) {
    val k = geo.k
    val context = LocalContext.current
    var stats by remember { mutableStateOf<DeviceStats?>(null) }

    // Refresh stats every 5 s, but only while this page is on screen.
    LaunchedEffect(visible) {
        stats = withContext(Dispatchers.IO) { DeviceStats.read(context) }
        if (!visible) return@LaunchedEffect
        while (true) {
            delay(5_000)
            stats = withContext(Dispatchers.IO) { DeviceStats.read(context) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = geo.y(0.055f), start = geo.x(0.045f), end = geo.x(0.045f))
    ) {
        SearchPill(k, onSearch, Modifier.fillMaxWidth().height(geo.y(0.052f)))
        Spacer(Modifier.height(geo.y(0.018f)))
        GreetingCard(k, userName, Modifier.fillMaxWidth().height(geo.y(0.155f)))
        Spacer(Modifier.height(geo.y(0.018f)))
        RecentCard(k, geo, recent, onLaunch, onLongPress, Modifier.fillMaxWidth().height(geo.y(0.355f)))
        Spacer(Modifier.height(geo.y(0.018f)))
        StatsCard(k, stats, onStorage, Modifier.fillMaxWidth().height(geo.y(0.135f)))
    }
}

@Composable
private fun SearchPill(k: Float, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .hudCard(10.dp * k, 10.dp * k, 10.dp * k, 10.dp * k)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 16.dp * k),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(17.dp * k)) { drawUiIcon(UiIcon.SEARCH, Hud.Red) }
        Spacer(Modifier.width(12.dp * k))
        BasicText(
            text = "Search apps...",
            style = HudType.body.copy(color = Hud.Grey, fontSize = (14 * k).sp),
            modifier = Modifier.weight(1f),
        )
        Canvas(Modifier.size(18.dp * k)) { drawUiIcon(UiIcon.MIC, Hud.Red) }
    }
}

@Composable
private fun GreetingCard(k: Float, name: String, modifier: Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val greeting = when (cal.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
    val quote = QUOTES[cal.get(Calendar.DAY_OF_YEAR) % QUOTES.size]
    val tl = 14.dp * k
    val tr = 30.dp * k
    val br = 14.dp * k
    val bl = 14.dp * k

    Box(
        modifier
            .hudCard(tl, tr, br, bl, glows = GREETING_GLOWS)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val clip = hudCutPath(size, tl.toPx(), tr.toPx(), br.toPx(), bl.toPx())
                val windowR = 1.1.dp.toPx()
                val step = 7.dp.toPx()
                val edge = 1.dp.toPx()
                onDrawBehind {
                    clipPath(clip) {
                        val haze = Offset(w * 0.8f, h * 0.95f)
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Hud.Red.copy(alpha = 0.55f), Hud.DeepRed.copy(alpha = 0.25f), Color.Transparent),
                                center = haze,
                                radius = w * 0.45f,
                            ),
                            w * 0.45f,
                            haze,
                        )
                        BUILDINGS.forEachIndexed { i, (bx, bw, bh) ->
                            val left = bx * w
                            val top = h * (1f - bh)
                            val width = bw * w
                            drawRect(Color(0xFF0D0306), Offset(left, top), Size(width, h - top))
                            drawLine(Hud.Red.copy(alpha = 0.7f), Offset(left, top), Offset(left + width, top), edge)
                            var y = top + step
                            var row = 0
                            while (y < h - step * 0.6f) {
                                if ((i + row) % 3 != 0) {
                                    drawCircle(Hud.Glow.copy(alpha = 0.55f), windowR, Offset(left + width * 0.3f, y))
                                }
                                if ((i + row) % 2 == 0) {
                                    drawCircle(Hud.Glow.copy(alpha = 0.4f), windowR, Offset(left + width * 0.7f, y))
                                }
                                y += step
                                row++
                            }
                        }
                    }
                }
            }
            .padding(horizontal = 18.dp * k, vertical = 14.dp * k)
    ) {
        Column(Modifier.fillMaxWidth(0.6f)) {
            BasicText(
                text = greeting,
                style = HudType.cardTitle.copy(fontSize = (14 * k).sp, fontWeight = FontWeight.Medium, color = Hud.Soft),
            )
            BasicText(
                text = name,
                style = HudType.title.copy(fontSize = (22 * k).sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp * k))
            BasicText(
                text = quote,
                style = HudType.cardSub.copy(fontSize = (12 * k).sp, lineHeight = (16 * k).sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp * k))
            Box(Modifier.size(22.dp * k, 2.dp * k).background(Hud.Red))
        }
    }
}

@Composable
private fun RecentCard(
    k: Float,
    geo: Geo,
    recent: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier,
) {
    val rows = recent.take(12).chunked(4)
    Column(
        modifier
            .hudCard(14.dp * k, 14.dp * k, 30.dp * k, 14.dp * k, glows = RECENT_GLOWS)
            .padding(horizontal = 12.dp * k, vertical = 12.dp * k)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp * k), verticalAlignment = Alignment.CenterVertically) {
            BasicText("Recently Installed", style = HudType.cardTitle.copy(fontSize = (15 * k).sp))
            Spacer(Modifier.weight(1f))
            BasicText("${recent.size} APPS", style = HudType.header.copy(color = Hud.Grey, fontSize = (10 * k).sp))
        }
        Spacer(Modifier.height(6.dp * k))
        if (rows.isEmpty()) {
            BasicText(
                text = "Apps you install will show up here.",
                style = HudType.cardSub.copy(fontSize = (12 * k).sp),
                modifier = Modifier.padding(4.dp * k),
            )
        }
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                row.forEach { app ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HudTile(
                                glyph = app.glyph,
                                label = app.label,
                                size = geo.x(0.12f),
                                onClick = { onLaunch(app) },
                                onLongClick = { onLongPress(app) },
                                labelWidth = geo.x(0.2f),
                                labelSize = (10 * k).sp,
                            )
                            BasicText(
                                text = ago(app.installedAt),
                                style = HudType.cardSub.copy(fontSize = (9.5f * k).sp, textAlign = TextAlign.Center),
                            )
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        repeat(3 - rows.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun StatsCard(k: Float, stats: DeviceStats?, onStorage: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .hudCard(14.dp * k, 14.dp * k, 14.dp * k, 26.dp * k, glows = STATS_GLOWS)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onStorage)
            .padding(horizontal = 14.dp * k, vertical = 10.dp * k),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatBlock(k, "STORAGE", stats?.storageUsed ?: 0L, stats?.storageTotal ?: 0L, Modifier.weight(1f))
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight(0.7f)
                .background(Hud.Line)
        )
        Spacer(Modifier.width(12.dp * k))
        StatBlock(k, "RAM", stats?.ramUsed ?: 0L, stats?.ramTotal ?: 0L, Modifier.weight(1f))
    }
}

@Composable
private fun StatBlock(k: Float, label: String, used: Long, total: Long, modifier: Modifier) {
    val frac = if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp * k), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 5.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(Hud.Line, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(Hud.Glow.copy(alpha = 0.3f), -90f, 360f * frac, false, Offset(inset, inset), arcSize, style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
                drawArc(Hud.Red, -90f, 360f * frac, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            BasicText("${(frac * 100).roundToInt()}%", style = HudType.cardTitle.copy(fontSize = (12 * k).sp))
        }
        Spacer(Modifier.width(10.dp * k))
        Column {
            BasicText(label, style = HudType.header.copy(fontSize = (10 * k).sp))
            Spacer(Modifier.height(3.dp))
            BasicText(
                text = if (total > 0) gb(used) else "--",
                style = HudType.cardTitle.copy(fontSize = (13 * k).sp),
                maxLines = 1,
            )
            BasicText(
                text = if (total > 0) "of ${gb(total)}" else "",
                style = HudType.cardSub.copy(fontSize = (11 * k).sp),
                maxLines = 1,
            )
        }
    }
}

private fun ago(time: Long): String {
    val minutes = (System.currentTimeMillis() - time) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1_440 -> "${minutes / 60}h ago"
        minutes < 10_080 -> "${minutes / 1_440}d ago"
        minutes < 43_200 -> "${minutes / 10_080}w ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(time))
    }
}