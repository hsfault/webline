package com.hsfault.webline.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hsfault.webline.data.AppEntry
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudIconTint
import com.hsfault.webline.ui.theme.HudType
import com.hsfault.webline.ui.theme.hudFrame

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HudAppIcon(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 54.dp,
    showLabel: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val glow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0.45f,
        animationSpec = tween(140),
        label = "nodeGlow",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "pressScale",
    )
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
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
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .hudFrame(
                    cut = iconSize * 0.22f,
                    fill = Brush.verticalGradient(
                        listOf(Hud.Maroon.copy(alpha = 0.6f), Hud.Night.copy(alpha = 0.92f))
                    ),
                    stroke = if (pressed) Hud.Crimson else Hud.Silver.copy(alpha = 0.4f),
                )
                .drawWithContent {
                    drawContent()
                    val cutPx = (iconSize * 0.22f).toPx()
                    val node = Offset(cutPx / 2f, cutPx / 2f)
                    val glowR = 9.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Hud.Glow.copy(alpha = glow), Color.Transparent),
                            center = node,
                            radius = glowR,
                        ),
                        radius = glowR,
                        center = node,
                    )
                    drawCircle(Hud.Glow, radius = (1.6f + glow * 1.2f).dp.toPx(), center = node)
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                colorFilter = HudIconTint,
                modifier = Modifier.size(iconSize * 0.64f),
            )
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = app.label,
                style = HudType.appName.copy(textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(iconSize + 18.dp),
            )
        }
    }
}