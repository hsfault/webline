package com.hsfault.webline.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hsfault.webline.ui.theme.GlowSeg
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import com.hsfault.webline.ui.theme.hudCard

data class MenuAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

private val MENU_GLOWS = listOf(GlowSeg(7), GlowSeg(5), GlowSeg(1))
private val BUTTON_GLOWS = listOf(GlowSeg(4, 0.15f, 0.85f))

@Composable
fun HudMenu(title: String, actions: List<MenuAction>, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 44.dp)
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { } }
                .hudCard(12.dp, 26.dp, 12.dp, 18.dp, glows = MENU_GLOWS)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            BasicText(
                text = title.uppercase(),
                style = HudType.header,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            actions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(enabled = action.enabled) {
                            action.onClick()
                            onDismiss()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(Modifier.size(7.dp)) {
                        drawCircle(if (action.enabled) Hud.Glow else Hud.Grey)
                    }
                    Spacer(Modifier.width(14.dp))
                    BasicText(
                        text = action.label,
                        style = HudType.menu.copy(color = if (action.enabled) Hud.White else Hud.Grey),
                    )
                }
            }
        }
    }
}

@Composable
fun HudButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .hudCard(8.dp, 8.dp, 8.dp, 8.dp, glows = if (enabled) BUTTON_GLOWS else emptyList())
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = HudType.menu.copy(color = if (enabled) Hud.White else Hud.Grey),
        )
    }
}