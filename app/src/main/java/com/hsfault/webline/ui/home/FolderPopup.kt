package com.hsfault.webline.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsfault.webline.data.AppEntry
import com.hsfault.webline.data.Folder
import com.hsfault.webline.ui.components.HudTile
import com.hsfault.webline.ui.theme.GlowSeg
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import com.hsfault.webline.ui.theme.hudCard

private val FOLDER_GLOWS = listOf(GlowSeg(7), GlowSeg(5), GlowSeg(1))

@Composable
fun FolderPopup(
    folder: Folder,
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 30.dp)
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { } }
                .hudCard(14.dp, 34.dp, 14.dp, 24.dp, glows = FOLDER_GLOWS)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(folder.title.uppercase(), style = HudType.header.copy(fontSize = 13.sp))
                Spacer(Modifier.weight(1f))
                BasicText("${apps.size} APPS", style = HudType.header.copy(color = Hud.Grey))
            }
            Spacer(Modifier.height(18.dp))
            if (apps.isEmpty()) {
                BasicText(
                    text = "Long-press an app in All Apps to add it here.",
                    style = HudType.cardSub.copy(fontSize = 13.sp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            } else {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    apps.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            row.forEach { app ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    HudTile(
                                        glyph = app.glyph,
                                        label = app.label,
                                        size = 54.dp,
                                        onClick = { onLaunch(app) },
                                        onLongClick = { onLongPress(app) },
                                    )
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}