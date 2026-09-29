package com.hsfault.webline.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
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
import com.hsfault.webline.ui.components.HudTile
import com.hsfault.webline.ui.components.UiIcon
import com.hsfault.webline.ui.components.drawUiIcon
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import kotlinx.coroutines.launch

private val LETTERS = ('A'..'Z').toList()

@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    title: String,
    onClose: () -> Unit,
    onTap: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onVoiceSearch: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
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

    val gridPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 6.dp, bottom = 32.dp)

    Column(modifier = Modifier.fillMaxSize().background(Hud.Bg).systemBarsPadding().imePadding()) {
        DrawerTitle(title = title, onClose = onClose)
        SearchBar(
            query = query,
            onQueryChange = { newText ->
                query = newText
                scope.launch { gridState.scrollToItem(0) }
            },
            onSubmit = { filtered.firstOrNull()?.let(onTap) },
            onVoice = onVoiceSearch,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.weight(1f).fillMaxHeight().nestedScroll(pullToClose),
                contentPadding = gridPadding,
            ) {
                items(filtered, key = { it.key }) { app ->
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        HudTile(
                            glyph = app.glyph,
                            label = app.label,
                            size = 54.dp,
                            onClick = { onTap(app) },
                            onLongClick = { onLongPress(app) },
                            labelWidth = 76.dp,
                        )
                    }
                }
            }
            if (query.isBlank()) {
                AlphabetRail(onPick = { letter: Char ->
                    val index = filtered.indexOfFirst { entry ->
                        val section = sectionOf(entry.label)
                        section != '#' && section >= letter
                    }
                    val target = if (index < 0) filtered.lastIndex else index
                    if (target >= 0) {
                        scope.launch { gridState.scrollToItem(target) }
                    }
                })
            }
        }
    }
}

@Composable
private fun DrawerTitle(title: String, onClose: () -> Unit) {
    val latestClose by rememberUpdatedState(onClose)
    val dragToClose = Modifier.pointerInput(Unit) {
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
    Row(
        modifier = Modifier.fillMaxWidth().then(dragToClose).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text = title, style = HudType.title, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(width = 22.dp, height = 2.dp).background(Hud.Red))
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoice: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val micInteraction = remember { MutableInteractionSource() }
    val searchOptions = KeyboardOptions(imeAction = ImeAction.Search)
    val searchActions = KeyboardActions(onSearch = { onSubmit() })
    val fieldModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = HudType.body,
        cursorBrush = SolidColor(Hud.Red),
        keyboardOptions = searchOptions,
        keyboardActions = searchActions,
        modifier = fieldModifier,
        decorationBox = { inner ->
            Row(
                modifier = Modifier.fillMaxWidth().height(46.dp).clip(shape).background(Hud.Surface).border(1.dp, Hud.Line, shape).padding(start = 14.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(modifier = Modifier.size(17.dp)) { drawUiIcon(UiIcon.SEARCH, Hud.Red) }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        BasicText(text = "Search apps...", style = HudType.body.copy(color = Hud.Grey))
                    }
                    inner()
                }
                Box(
                    modifier = Modifier.size(44.dp).clickable(interactionSource = micInteraction, indication = null, onClick = onVoice),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(18.dp)) { drawUiIcon(UiIcon.MIC, Hud.Red) }
                }
            }
        },
    )
}

@Composable
private fun AlphabetRail(onPick: (Char) -> Unit) {
    var active by remember { mutableStateOf<Char?>(null) }
    val haptics = LocalHapticFeedback.current
    val pick by rememberUpdatedState(onPick)

    val scrubber = Modifier.pointerInput(Unit) {
        fun select(y: Float) {
            val index = ((y / size.height) * LETTERS.size).toInt().coerceIn(0, LETTERS.lastIndex)
            val letter = LETTERS[index]
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
    }

    Column(
        modifier = Modifier.width(24.dp).fillMaxHeight().padding(vertical = 10.dp).then(scrubber),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LETTERS.forEach { letter ->
            val on = letter == active
            BasicText(
                text = letter.toString(),
                style = HudType.rail.copy(
                    fontSize = if (on) 13.sp else 9.sp,
                    color = if (on) Hud.Glow else Hud.Red,
                ),
            )
        }
    }
}

private fun sectionOf(label: String): Char {
    val c = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}