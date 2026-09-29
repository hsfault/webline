package com.hsfault.webline.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsfault.webline.data.AppEntry
import com.hsfault.webline.data.GlyphSource
import com.hsfault.webline.data.WeatherNow
import com.hsfault.webline.media.NowPlaying
import com.hsfault.webline.ui.components.HudTile
import com.hsfault.webline.ui.theme.GlowSeg
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.hudCard
import com.hsfault.webline.ui.theme.hudPlate

/** Screen geometry: every position in the design is a fraction of screen width/height. */
class Geo(val w: Dp, val h: Dp) {
    val k: Float = w / 360.dp
    fun x(f: Float): Dp = w * f
    fun y(f: Float): Dp = h * f
}

private const val TILE = 0.108f
private const val PAD_X = 0.10f
private const val PAD_Y = 0.045f

private class ClusterSpec(
    val plate: List<Offset>,
    val glows: List<GlowSeg>,
    val tiles: List<Pair<Int, Offset>>,
    val tiltY: Float,
)

/** Positions measured from the reference design (fractions of screen width, height). */
private val CLUSTERS = listOf(
    // Left staircase
    ClusterSpec(
        plate = listOf(Offset(0.035f, 0.290f), Offset(0.230f, 0.292f), Offset(0.320f, 0.500f), Offset(0.075f, 0.512f)),
        glows = listOf(GlowSeg(3, 0.55f, 1f), GlowSeg(0, 0f, 0.3f), GlowSeg(2, 0f, 0.35f)),
        tiles = listOf(0 to Offset(0.113f, 0.334f), 1 to Offset(0.230f, 0.393f), 2 to Offset(0.150f, 0.470f)),
        tiltY = 8f,
    ),
    // Right staircase
    ClusterSpec(
        plate = listOf(Offset(0.800f, 0.325f), Offset(0.965f, 0.335f), Offset(0.905f, 0.585f), Offset(0.690f, 0.575f)),
        glows = listOf(GlowSeg(1), GlowSeg(2, 0f, 0.3f)),
        tiles = listOf(3 to Offset(0.880f, 0.369f), 4 to Offset(0.832f, 0.448f), 5 to Offset(0.797f, 0.527f)),
        tiltY = -8f,
    ),
    // Bottom-left row
    ClusterSpec(
        plate = listOf(Offset(0.075f, 0.662f), Offset(0.495f, 0.688f), Offset(0.490f, 0.772f), Offset(0.060f, 0.748f)),
        glows = listOf(GlowSeg(3), GlowSeg(0, 0f, 0.25f)),
        tiles = listOf(6 to Offset(0.172f, 0.695f), 7 to Offset(0.312f, 0.709f), 8 to Offset(0.438f, 0.719f)),
        tiltY = 6f,
    ),
    // Bottom-right row
    ClusterSpec(
        plate = listOf(Offset(0.545f, 0.738f), Offset(0.940f, 0.640f), Offset(0.960f, 0.722f), Offset(0.560f, 0.822f)),
        glows = listOf(GlowSeg(1), GlowSeg(3, 0f, 0.5f)),
        tiles = listOf(9 to Offset(0.616f, 0.762f), 10 to Offset(0.743f, 0.726f), 11 to Offset(0.868f, 0.690f)),
        tiltY = -6f,
    ),
)

private val DOCK_X = listOf(0.184f, 0.351f, 0.509f, 0.668f, 0.832f)
private val DOCK_GLOWS = listOf(GlowSeg(7), GlowSeg(1), GlowSeg(3), GlowSeg(4), GlowSeg(5))

@Composable
fun HomeComposition(
    geo: Geo,
    slots: List<SlotItem>,
    weather: WeatherNow?,
    nowPlaying: NowPlaying?,
    hasMusicAccess: Boolean,
    onSlotTap: (Int) -> Unit,
    onSlotLongPress: (Int) -> Unit,
    onClock: () -> Unit,
    onWeather: () -> Unit,
    onOpenMusic: () -> Unit,
    onMusicPrev: () -> Unit,
    onMusicPlayPause: () -> Unit,
    onMusicNext: () -> Unit,
) {
    val k = geo.k
    Box(Modifier.fillMaxSize()) {
        CLUSTERS.forEach { spec -> Cluster(spec, geo, slots, onSlotTap, onSlotLongPress) }

        ClockCard(
            k = k,
            onClick = onClock,
            modifier = Modifier
                .offset(geo.x(0.062f), geo.y(0.060f))
                .size(geo.x(0.44f), geo.y(0.186f)),
        )

        WeatherCard(
            k = k,
            weather = weather,
            onClick = onWeather,
            modifier = Modifier
                .offset(geo.x(0.553f), geo.y(0.075f))
                .size(geo.x(0.392f), geo.y(0.178f))
                .graphicsLayer {
                    rotationZ = -4f
                    rotationY = -6f
                    cameraDistance = 16f * density
                },
        )

        MusicCard(
            k = k,
            nowPlaying = nowPlaying,
            hasAccess = hasMusicAccess,
            onOpen = onOpenMusic,
            onPrev = onMusicPrev,
            onPlayPause = onMusicPlayPause,
            onNext = onMusicNext,
            modifier = Modifier
                .offset(geo.x(0.065f), geo.y(0.535f))
                .size(geo.x(0.518f), geo.y(0.105f))
                .graphicsLayer {
                    rotationZ = 2.5f
                    rotationY = 5f
                    cameraDistance = 16f * density
                },
        )

        QuoteBlock(
            k = k,
            lines = listOf("SMALL STEPS", "BUILD", "BIG DREAMS"),
            modifier = Modifier.offset(geo.x(0.065f), geo.y(0.776f)),
        )
    }
}

@Composable
private fun Cluster(
    spec: ClusterSpec,
    geo: Geo,
    slots: List<SlotItem>,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    val tile = geo.x(TILE)
    val labelW = tile + 34.dp * geo.k
    val minX = spec.plate.minOf { it.x } - PAD_X
    val maxX = spec.plate.maxOf { it.x } + PAD_X
    val minY = spec.plate.minOf { it.y } - PAD_Y
    val maxY = spec.plate.maxOf { it.y } + PAD_Y
    val boxW = maxX - minX
    val boxH = maxY - minY
    val local = spec.plate.map { Offset((it.x - minX) / boxW, (it.y - minY) / boxH) }

    Box(
        Modifier
            .offset(geo.x(minX), geo.y(minY))
            .size(geo.x(boxW), geo.y(boxH))
            .graphicsLayer {
                rotationY = spec.tiltY
                cameraDistance = 14f * density
            }
    ) {
        Box(Modifier.fillMaxSize().hudPlate(local, spec.glows))
        spec.tiles.forEach { (slot, center) ->
            SlotTile(
                item = slots.getOrElse(slot) { SlotItem.Empty },
                size = tile,
                labelWidth = labelW,
                k = geo.k,
                onClick = { onTap(slot) },
                onLongClick = { onLongPress(slot) },
                modifier = Modifier.offset(
                    geo.x(center.x - minX) - labelW / 2,
                    geo.y(center.y - minY) - tile / 2,
                ),
            )
        }
    }
}

@Composable
fun SlotTile(
    item: SlotItem,
    size: Dp,
    labelWidth: Dp,
    k: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    rounded: Boolean = false,
    showLabel: Boolean = true,
) {
    val (glyph, label) = when (item) {
        is SlotItem.App -> item.app.glyph to item.app.label
        is SlotItem.FolderSlot -> GlyphSource.Drawn(item.folder.glyph) to item.folder.title
        SlotItem.Empty -> null to null
    }
    HudTile(
        glyph = glyph,
        label = if (showLabel) label else null,
        size = size,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        rounded = rounded,
        labelWidth = labelWidth,
        labelSize = (10.5f * k).sp,
    )
}

/** Glass dock slab with five rounded tiles. Emitted directly into the home screen's Box. */
@Composable
fun Dock(geo: Geo, items: List<SlotItem>, onTap: (Int) -> Unit, onLongPress: (Int) -> Unit) {
    val k = geo.k
    Box(
        Modifier
            .offset(geo.x(0.077f), geo.y(0.854f))
            .size(geo.x(0.856f), geo.y(0.081f))
            .hudCard(22.dp * k, 22.dp * k, 14.dp * k, 14.dp * k, glows = DOCK_GLOWS)
    )
    val tile = geo.x(0.108f)
    DOCK_X.forEachIndexed { i, fx ->
        SlotTile(
            item = items.getOrElse(i) { SlotItem.Empty },
            size = tile,
            labelWidth = tile,
            k = k,
            onClick = { onTap(i) },
            onLongClick = { onLongPress(i) },
            modifier = Modifier.offset(geo.x(fx) - tile / 2, geo.y(0.894f) - tile / 2),
            rounded = true,
            showLabel = false,
        )
    }
}

/** Extra home pages: a plain 4x5 grid of the same 3D tiles. */
@Composable
fun ExtraPage(geo: Geo, apps: List<AppEntry>, onTap: (AppEntry) -> Unit, onLongPress: (AppEntry) -> Unit) {
    val tile = geo.x(0.15f)
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = geo.y(0.07f), bottom = geo.h - geo.y(0.83f), start = 8.dp, end = 8.dp)
    ) {
        for (r in 0 until 5) {
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                for (c in 0 until 4) {
                    val app = apps.getOrNull(r * 4 + c)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (app != null) {
                            HudTile(
                                glyph = app.glyph,
                                label = app.label,
                                size = tile,
                                onClick = { onTap(app) },
                                onLongClick = { onLongPress(app) },
                                labelSize = (11 * geo.k).sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PageDots(geo: Geo, count: Int, current: Int) {
    Canvas(
        Modifier
            .offset(y = geo.y(0.832f))
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val gap = 16.dp.toPx()
        val total = gap * (count - 1)
        val startX = (size.width - total) / 2f
        val y = size.height / 2f
        for (i in 0 until count) {
            val p = Offset(startX + gap * i, y)
            if (i == current) {
                drawCircle(Hud.Glow.copy(alpha = 0.3f), 6.dp.toPx(), p)
                drawCircle(Hud.Glow, 2.8.dp.toPx(), p)
            } else {
                drawCircle(Hud.Grey.copy(alpha = 0.6f), 2.dp.toPx(), p)
            }
        }
    }
}