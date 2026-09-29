package com.hsfault.webline.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsfault.webline.data.GlyphSource
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudPaint
import com.hsfault.webline.ui.theme.HudType
import kotlin.math.roundToInt

private val GlyphBrush = Brush.verticalGradient(
    listOf(Color(0xFFFF4545), Color(0xFFD01422), Color(0xFF8A0913))
)

/**
 * A 3D tile with a red glyph and an optional label underneath.
 * glyph = null draws an empty slot (dimmed tile with a +).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HudTile(
    glyph: GlyphSource?,
    label: String?,
    size: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    rounded: Boolean = false,
    labelWidth: Dp = size + 30.dp,
    labelSize: TextUnit = 11.sp,
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }
    val tileImage = remember(sizePx, rounded) { HudPaint.tile(sizePx, rounded, density.density) }
    val margin = remember(density) { HudPaint.tileMargin(density.density).toFloat() }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "tilePress",
    )
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .width(labelWidth)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (glyph == null) 0.5f else 1f
                }
                .drawBehind { drawImage(tileImage, topLeft = Offset(-margin, -margin)) },
            contentAlignment = Alignment.Center,
        ) {
            when (glyph) {
                null -> Canvas(Modifier.size(size * 0.36f)) { drawUiIcon(UiIcon.PLUS, Hud.Grey) }
                is GlyphSource.Original -> Image(
                    bitmap = glyph.bitmap,
                    contentDescription = label,
                    modifier = Modifier.size(size * 0.58f),
                )
                else -> RedGlyph(glyph, Modifier.size(size * 0.5f))
            }
        }
        if (label != null) {
            Spacer(Modifier.height(5.dp))
            BasicText(
                text = label,
                style = HudType.label.copy(fontSize = labelSize, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RedGlyph(glyph: GlyphSource, modifier: Modifier) {
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        when (glyph) {
            is GlyphSource.Drawn -> drawGlyph(glyph.kind)
            is GlyphSource.Mask -> drawImage(
                image = glyph.bitmap,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.High,
            )
            is GlyphSource.Original -> Unit
        }
        drawRect(GlyphBrush, blendMode = BlendMode.SrcIn)
    }
}