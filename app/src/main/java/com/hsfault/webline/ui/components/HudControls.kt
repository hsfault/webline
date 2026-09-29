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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import com.hsfault.webline.ui.theme.hudFrame

data class MenuAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
fun HudMenu(title: String, actions: List<MenuAction>, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Hud.Void.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { } }
                .hudFrame(cut = 16.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            BasicText(
                text = "// ${title.uppercase()}",
                style = HudType.label.copy(color = Hud.Crimson),
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
                    Canvas(Modifier.size(8.dp)) {
                        drawCircle(if (action.enabled) Hud.Glow else Hud.Muted)
                    }
                    Spacer(Modifier.width(14.dp))
                    BasicText(
                        text = action.label,
                        style = HudType.action.copy(color = if (action.enabled) Hud.Light else Hud.Muted),
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
            .hudFrame(
                cut = 10.dp,
                fill = SolidColor(if (enabled) Hud.Maroon.copy(alpha = 0.75f) else Hud.Night),
                stroke = if (enabled) Hud.Crimson.copy(alpha = 0.8f) else Hud.Silver.copy(alpha = 0.25f),
                accent = if (enabled) Hud.Glow else Hud.Muted,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = HudType.action.copy(color = if (enabled) Hud.Light else Hud.Muted),
        )
    }
}