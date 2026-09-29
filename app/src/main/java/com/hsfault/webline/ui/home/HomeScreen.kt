package com.hsfault.webline.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hsfault.webline.data.AppEntry
import com.hsfault.webline.data.LayoutStore
import com.hsfault.webline.ui.components.HudAppIcon
import com.hsfault.webline.ui.components.HudMenu
import com.hsfault.webline.ui.components.MenuAction
import com.hsfault.webline.ui.theme.Hud

private const val COLUMNS = 4
private const val FIRST_PAGE = 16 // page one: clock header + 4x4
private const val PAGE = 20       // other pages: 4x5

private enum class Source { Drawer, Home, Dock }
private data class MenuTarget(val app: AppEntry, val source: Source)

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    layout: LayoutStore,
    drawerOpen: Boolean,
    homeSignal: Int,
    onDrawerOpenChange: (Boolean) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onSwipeDown: () -> Unit,
) {
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val homeApps = remember(byKey, layout.home) { layout.home.mapNotNull { byKey[it] } }
    val dockApps = remember(byKey, layout.dock) { layout.dock.mapNotNull { byKey[it] } }
    val pages = remember(homeApps) {
        buildList {
            add(homeApps.take(FIRST_PAGE))
            homeApps.drop(FIRST_PAGE).chunked(PAGE).forEach { add(it) }
        }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    var menu by remember { mutableStateOf<MenuTarget?>(null) }

    // Home button pressed while already on the home screen: close things, go to page one.
    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            menu = null
            pagerState.animateScrollToPage(0)
        }
    }

    // Swallow back on the home screen (the drawer and menu register their own handlers on top).
    BackHandler { menu = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(drawerOpen) {
                if (drawerOpen) return@pointerInput
                val threshold = 72.dp.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -threshold -> onDrawerOpenChange(true)
                            total > threshold -> onSwipeDown()
                        }
                    },
                    onVerticalDrag = { change, dy ->
                        total += dy
                        change.consume()
                    },
                )
            },
    ) {
        WebParallax(pagerState)

        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                val pageApps = pages.getOrElse(page) { emptyList() }
                Column(Modifier.fillMaxSize()) {
                    if (page == 0) ClockHeader(Modifier.padding(top = 20.dp))
                    IconGrid(
                        apps = pageApps,
                        rows = if (page == 0) FIRST_PAGE / COLUMNS else PAGE / COLUMNS,
                        modifier = Modifier.weight(1f),
                        onLaunch = onLaunch,
                        onLongPress = { menu = MenuTarget(it, Source.Home) },
                    )
                }
            }
            PageIndicator(count = pages.size, current = pagerState.currentPage)
            Dock(
                apps = dockApps,
                onLaunch = onLaunch,
                onLongPress = { menu = MenuTarget(it, Source.Dock) },
            )
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(260)) { it / 3 } + fadeOut(tween(200)),
        ) {
            AppDrawer(
                apps = apps,
                onClose = { onDrawerOpenChange(false) },
                onLaunch = onLaunch,
                onLongPress = { menu = MenuTarget(it, Source.Drawer) },
            )
        }

        menu?.let { target ->
            HudMenu(
                title = target.app.label,
                actions = menuActions(target, layout, onAppInfo),
                onDismiss = { menu = null },
            )
        }
    }
}

private fun menuActions(
    target: MenuTarget,
    layout: LayoutStore,
    onAppInfo: (AppEntry) -> Unit,
): List<MenuAction> {
    val key = target.app.key
    val dockFull = layout.dock.size >= LayoutStore.DOCK_SIZE
    val dockAction = if (key in layout.dock) null else {
        MenuAction(if (dockFull) "DOCK FULL" else "ADD TO DOCK", enabled = !dockFull) { layout.addToDock(key) }
    }
    val info = MenuAction("APP INFO") { onAppInfo(target.app) }

    return when (target.source) {
        Source.Drawer -> listOfNotNull(
            if (key in layout.home) null else MenuAction("ADD TO HOME") { layout.addToHome(key) },
            dockAction,
            info,
        )
        Source.Home -> listOfNotNull(
            MenuAction("REMOVE FROM HOME") { layout.removeFromHome(key) },
            dockAction,
            info,
        )
        Source.Dock -> listOf(
            MenuAction("REMOVE FROM DOCK") { layout.removeFromDock(key) },
            info,
        )
    }
}

@Composable
private fun IconGrid(
    apps: List<AppEntry>,
    rows: Int,
    modifier: Modifier,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (c in 0 until COLUMNS) {
                    val app = apps.getOrNull(r * COLUMNS + c)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (app != null) {
                            HudAppIcon(
                                app = app,
                                onClick = { onLaunch(app) },
                                onLongClick = { onLongPress(app) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int) {
    if (count <= 1) {
        Spacer(Modifier.height(18.dp))
        return
    }
    Canvas(Modifier.fillMaxWidth().height(18.dp)) {
        val gap = 18.dp.toPx()
        val total = gap * (count - 1)
        val startX = (size.width - total) / 2f
        val y = size.height / 2f
        drawLine(Hud.Silver.copy(alpha = 0.3f), Offset(startX, y), Offset(startX + total, y), 1.dp.toPx())
        for (i in 0 until count) {
            val p = Offset(startX + gap * i, y)
            if (i == current) {
                drawCircle(Hud.Glow.copy(alpha = 0.35f), 7.dp.toPx(), p)
                drawCircle(Hud.Glow, 3.dp.toPx(), p)
            } else {
                drawCircle(Hud.Silver.copy(alpha = 0.6f), 2.dp.toPx(), p)
            }
        }
    }
}

/** Dock: icons sit as nodes on a curved web strand. */
@Composable
private fun Dock(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val slots = LayoutStore.DOCK_SIZE
    val edge = 24.dp
    val dip = 20.dp

    Box(Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 4.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val y0 = edge.toPx()
            val sag = dip.toPx()
            val strand = Path().apply {
                moveTo(0f, y0)
                quadraticBezierTo(size.width / 2f, y0 + 2f * sag, size.width, y0)
            }
            drawPath(strand, Hud.Crimson.copy(alpha = 0.22f), style = Stroke(width = 6.dp.toPx()))
            drawPath(strand, Hud.Silver.copy(alpha = 0.75f), style = Stroke(width = 1.2.dp.toPx()))
            for (i in apps.size until slots) {
                val f = (i + 0.5f) / slots
                val p = Offset(size.width * f, y0 + 4f * f * (1f - f) * sag)
                drawCircle(Hud.Glow.copy(alpha = 0.25f), 6.dp.toPx(), p)
                drawCircle(Hud.Glow, 2.dp.toPx(), p)
            }
        }
        Row(Modifier.fillMaxSize()) {
            for (i in 0 until slots) {
                val f = (i + 0.5f) / slots
                val centerY = edge + dip * (4f * f * (1f - f))
                val app = apps.getOrNull(i)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    if (app != null) {
                        HudAppIcon(
                            app = app,
                            onClick = { onLaunch(app) },
                            onLongClick = { onLongPress(app) },
                            iconSize = 48.dp,
                            showLabel = false,
                            modifier = Modifier.offset(y = centerY - 28.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Faint web strands behind the icons that drift slightly as you swipe pages. */
@Composable
private fun WebParallax(pagerState: PagerState) {
    val shiftPerPage = with(LocalDensity.current) { 40.dp.toPx() }
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = -(pagerState.currentPage + pagerState.currentPageOffsetFraction) * shiftPerPage
            }
    ) {
        val w = size.width
        val h = size.height
        val strandColor = Hud.Silver.copy(alpha = 0.16f)
        STRANDS.forEach { s ->
            val a = Offset(s[0] * w, s[1] * h)
            val b = Offset(s[2] * w, s[3] * h)
            drawLine(strandColor, a, b, 1.dp.toPx())
            listOf(0.32f, 0.71f).forEach { t ->
                val p = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                drawCircle(Hud.Glow.copy(alpha = 0.18f), 5.dp.toPx(), p)
                drawCircle(Hud.Glow.copy(alpha = 0.7f), 1.5.dp.toPx(), p)
            }
        }
    }
}

private val STRANDS = listOf(
    floatArrayOf(-0.3f, 0.10f, 1.5f, 0.34f),
    floatArrayOf(-0.4f, 0.58f, 1.7f, 0.40f),
    floatArrayOf(0.2f, 1.05f, 1.4f, 0.64f),
    floatArrayOf(-0.5f, 0.82f, 0.8f, 1.08f),
    floatArrayOf(0.6f, -0.05f, 1.9f, 0.55f),
)