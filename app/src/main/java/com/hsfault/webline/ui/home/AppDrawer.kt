package com.hsfault.webline.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hsfault.webline.data.AppEntry
import com.hsfault.webline.ui.components.HudAppIcon
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import com.hsfault.webline.ui.theme.hudFrame
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    onClose: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }
    val sections = remember(filtered) {
        val map = LinkedHashMap<Char, Int>()
        filtered.forEachIndexed { index, app -> map.putIfAbsent(sectionOf(app.label), index) }
        map
    }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val closeThreshold = with(LocalDensity.current) { 110.dp.toPx() }
    val latestClose by rememberUpdatedState(onClose)

    BackHandler(onBack = onClose)
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }

    // Pulling down while the grid is already at the top closes the drawer.
    val pullToClose = remember(closeThreshold) {
        object : NestedScrollConnection {
            private var pull = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pull += available.y
                    if (pull > closeThreshold) {
                        pull = 0f
                        latestClose()
                    }
                } else if (consumed.y != 0f || available.y < 0f) {
                    pull = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pull = 0f
                return Velocity.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Hud.Void.copy(alpha = 0.96f), Hud.Night.copy(alpha = 0.98f)))
            )
            .systemBarsPadding()
            .imePadding(),
    ) {
        DrawerHeader(count = apps.size, onClose = onClose)
        SearchField(
            query = query,
            onQueryChange = {
                query = it
                scope.launch { gridState.scrollToItem(0) }
            },
            onSubmit = { filtered.firstOrNull()?.let(onLaunch) },
        )
        Row(Modifier.weight(1f).fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.weight(1f).fillMaxHeight().nestedScroll(pullToClose),
                contentPadding = PaddingValues(start = 6.dp, end = 2.dp, top = 8.dp, bottom = 32.dp),
            ) {
                items(filtered, key = { it.key }) { app ->
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        HudAppIcon(
                            app = app,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPress(app) },
                        )
                    }
                }
            }
            if (query.isBlank() && sections.size > 1) {
                AlphabetRail(
                    letters = sections.keys.toList(),
                    onPick = { letter ->
                        sections[letter]?.let { index -> scope.launch { gridState.scrollToItem(index) } }
                    },
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(count: Int, onClose: () -> Unit) {
    val latestClose by rememberUpdatedState(onClose)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                val threshold = 56.dp.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > threshold) latestClose() },
                    onVerticalDrag = { change, dy ->
                        total += dy
                        change.consume()
                    },
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { latestClose() }
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(width = 60.dp, height = 20.dp)) {
            val x = size.width / 2f
            val nodeY = size.height - 5.dp.toPx()
            drawLine(Hud.Silver.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, nodeY), 1.dp.toPx())
            drawCircle(Hud.Glow.copy(alpha = 0.3f), 6.dp.toPx(), Offset(x, nodeY))
            drawCircle(Hud.Glow, 2.5.dp.toPx(), Offset(x, nodeY))
        }
        BasicText("APPS // $count", style = HudType.label)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = HudType.body,
        cursorBrush = SolidColor(Hud.Crimson),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .hudFrame(cut = 12.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(14.dp)) {
                    val stroke = 1.5.dp.toPx()
                    drawCircle(Hud.Crimson, size.minDimension / 2.8f, center, style = Stroke(stroke))
                    drawLine(
                        Hud.Crimson,
                        center + Offset(size.width * 0.26f, size.height * 0.26f),
                        Offset(size.width, size.height),
                        stroke,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        BasicText("SEARCH THE WEB", style = HudType.label.copy(fontSize = 13.sp))
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
private fun AlphabetRail(letters: List<Char>, onPick: (Char) -> Unit) {
    var active by remember { mutableStateOf<Char?>(null) }
    val haptics = LocalHapticFeedback.current
    val pick by rememberUpdatedState(onPick)

    Column(
        modifier = Modifier
            .width(26.dp)
            .fillMaxHeight()
            .padding(vertical = 12.dp)
            .drawBehind {
                val x = size.width / 2f
                drawLine(Hud.Silver.copy(alpha = 0.25f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
            .pointerInput(letters) {
                fun select(y: Float) {
                    val index = ((y / size.height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
                    val letter = letters[index]
                    if (letter != active) {
                        active = letter
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pick(letter)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    select(down.position.y)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        select(change.position.y)
                        change.consume()
                    }
                    active = null
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val on = letter == active
            BasicText(
                text = letter.toString(),
                style = HudType.label.copy(
                    fontSize = if (on) 14.sp else 10.sp,
                    color = if (on) Hud.Glow else Hud.Muted,
                    letterSpacing = 0.sp,
                ),
            )
        }
    }
}

private fun sectionOf(label: String): Char {
    val c = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}