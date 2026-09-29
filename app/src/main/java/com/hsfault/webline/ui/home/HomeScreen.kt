package com.hsfault.webline.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.hsfault.webline.data.AppEntry
import com.hsfault.webline.data.Folder
import com.hsfault.webline.data.LayoutStore
import com.hsfault.webline.data.WeatherNow
import com.hsfault.webline.ui.components.HudMenu
import com.hsfault.webline.ui.components.MenuAction

/** Everything the home screen asks the Activity to do. */
class HomeActions(
    val launch: (AppEntry) -> Unit,
    val appInfo: (AppEntry) -> Unit,
    val swipeDown: () -> Unit,
    val openClock: () -> Unit,
    val refreshWeather: () -> Unit,
    val openMusic: () -> Unit,
    val mediaKey: (Int) -> Unit,
    val voiceSearch: () -> Unit,
)

/** What a home slot or dock slot currently holds. */
sealed interface SlotItem {
    data class App(val app: AppEntry) : SlotItem
    data class FolderSlot(val folder: Folder) : SlotItem
    data object Empty : SlotItem
}

fun resolveSlot(value: String, byKey: Map<String, AppEntry>): SlotItem =
    Folder.fromToken(value)?.let { SlotItem.FolderSlot(it) }
        ?: byKey[value]?.let { SlotItem.App(it) }
        ?: SlotItem.Empty

private sealed interface MenuTarget {
    data class Slot(val index: Int) : MenuTarget
    data class DockSlot(val index: Int) : MenuTarget
    data class Extra(val app: AppEntry) : MenuTarget
    data class DrawerApp(val app: AppEntry) : MenuTarget
    data class FolderApp(val folder: Folder, val app: AppEntry) : MenuTarget
}

private sealed interface PickTarget {
    data class Slot(val index: Int) : PickTarget
    data class DockSlot(val index: Int) : PickTarget
}

private const val EXTRA_PAGE_SIZE = 20

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    layout: LayoutStore,
    weather: WeatherNow?,
    drawerOpen: Boolean,
    homeSignal: Int,
    onDrawerOpenChange: (Boolean) -> Unit,
    actions: HomeActions,
) {
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val slots = remember(byKey, layout.slots) { layout.slots.map { resolveSlot(it, byKey) } }
    val dock = remember(byKey, layout.dock) { layout.dock.map { resolveSlot(it, byKey) } }
    val extras = remember(byKey, layout.extras) { layout.extras.mapNotNull { byKey[it] } }
    val extraPages = remember(extras) { extras.chunked(EXTRA_PAGE_SIZE) }
    val pagerState = rememberPagerState(pageCount = { 1 + extraPages.size })

    var menu by remember { mutableStateOf<MenuTarget?>(null) }
    var pick by remember { mutableStateOf<PickTarget?>(null) }
    var openFolder by remember { mutableStateOf<Folder?>(null) }

    // Home pressed while already home: close everything and return to page one.
    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            menu = null
            pick = null
            openFolder = null
            pagerState.animateScrollToPage(0)
        }
    }

    // Swallow back on the home screen; overlays register their own handlers on top.
    BackHandler { menu = null }

    val overlayOpen = drawerOpen || pick != null || openFolder != null || menu != null

    fun tapSlot(item: SlotItem, onEmpty: () -> Unit) {
        when (item) {
            is SlotItem.App -> actions.launch(item.app)
            is SlotItem.FolderSlot -> openFolder = item.folder
            SlotItem.Empty -> onEmpty()
        }
    }

    fun assign(target: PickTarget, key: String) = when (target) {
        is PickTarget.Slot -> layout.setSlot(target.index, key)
        is PickTarget.DockSlot -> layout.setDock(target.index, key)
    }

    fun buildMenu(target: MenuTarget): Pair<String, List<MenuAction>> = when (target) {
        is MenuTarget.Slot -> when (val item = slots.getOrElse(target.index) { SlotItem.Empty }) {
            is SlotItem.App -> item.app.label to listOf(
                MenuAction("REPLACE APP") { pick = PickTarget.Slot(target.index) },
                MenuAction("REMOVE") { layout.setSlot(target.index, "") },
                MenuAction("APP INFO") { actions.appInfo(item.app) },
            )
            is SlotItem.FolderSlot -> item.folder.title to listOf(
                MenuAction("OPEN") { openFolder = item.folder },
                MenuAction("REPLACE WITH APP") { pick = PickTarget.Slot(target.index) },
                MenuAction("REMOVE") { layout.setSlot(target.index, "") },
            )
            SlotItem.Empty -> "Empty slot" to listOf(
                MenuAction("ADD APP") { pick = PickTarget.Slot(target.index) },
                MenuAction("ADD TOOLS FOLDER") { layout.setSlot(target.index, Folder.TOOLS.token) },
                MenuAction("ADD SOCIAL FOLDER") { layout.setSlot(target.index, Folder.SOCIAL.token) },
            )
        }
        is MenuTarget.DockSlot -> when (val item = dock.getOrElse(target.index) { SlotItem.Empty }) {
            is SlotItem.App -> item.app.label to listOf(
                MenuAction("REPLACE APP") { pick = PickTarget.DockSlot(target.index) },
                MenuAction("REMOVE") { layout.setDock(target.index, "") },
                MenuAction("APP INFO") { actions.appInfo(item.app) },
            )
            else -> "Dock slot" to listOf(
                MenuAction("ADD APP") { pick = PickTarget.DockSlot(target.index) },
            )
        }
        is MenuTarget.Extra -> target.app.label to listOf(
            MenuAction("REMOVE FROM HOME") { layout.removeExtra(target.app.key) },
            MenuAction("APP INFO") { actions.appInfo(target.app) },
        )
        is MenuTarget.DrawerApp -> target.app.label to listOfNotNull(
            if (target.app.key in layout.extras) null
            else MenuAction("ADD TO HOME PAGES") { layout.addExtra(target.app.key) },
            if (target.app.key in layout.tools) null
            else MenuAction("ADD TO TOOLS") { layout.addToFolder(Folder.TOOLS, target.app.key) },
            if (target.app.key in layout.social) null
            else MenuAction("ADD TO SOCIAL") { layout.addToFolder(Folder.SOCIAL, target.app.key) },
            MenuAction("APP INFO") { actions.appInfo(target.app) },
        )
        is MenuTarget.FolderApp -> target.app.label to listOf(
            MenuAction("REMOVE FROM FOLDER") { layout.removeFromFolder(target.folder, target.app.key) },
            MenuAction("APP INFO") { actions.appInfo(target.app) },
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(overlayOpen) {
                if (overlayOpen) return@pointerInput
                val threshold = 72.dp.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -threshold -> onDrawerOpenChange(true)
                            total > threshold -> actions.swipeDown()
                        }
                    },
                    onVerticalDrag = { change, dy ->
                        total += dy
                        change.consume()
                    },
                )
            },
    ) {
        val geo = Geo(maxWidth, maxHeight)

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                HomeComposition(
                    geo = geo,
                    slots = slots,
                    weather = weather,
                    onSlotTap = { i -> tapSlot(slots[i]) { menu = MenuTarget.Slot(i) } },
                    onSlotLongPress = { i -> menu = MenuTarget.Slot(i) },
                    onClock = actions.openClock,
                    onWeather = actions.refreshWeather,
                    onOpenMusic = actions.openMusic,
                    onMediaKey = actions.mediaKey,
                )
            } else {
                ExtraPage(
                    geo = geo,
                    apps = extraPages.getOrElse(page - 1) { emptyList() },
                    onTap = actions.launch,
                    onLongPress = { menu = MenuTarget.Extra(it) },
                )
            }
        }

        if (extraPages.isNotEmpty()) {
            PageDots(geo, count = 1 + extraPages.size, current = pagerState.currentPage)
        }

        Dock(
            geo = geo,
            items = dock,
            onTap = { i -> tapSlot(dock[i]) { pick = PickTarget.DockSlot(i) } },
            onLongPress = { i -> menu = MenuTarget.DockSlot(i) },
        )

        AnimatedVisibility(
            visible = drawerOpen || pick != null,
            enter = slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(260)) { it / 3 } + fadeOut(tween(200)),
        ) {
            AppDrawer(
                apps = apps,
                title = if (pick != null) "Select App" else "All Apps",
                onClose = {
                    pick = null
                    onDrawerOpenChange(false)
                },
                onTap = { app ->
                    val target = pick
                    if (target != null) {
                        assign(target, app.key)
                        pick = null
                        onDrawerOpenChange(false)
                    } else {
                        actions.launch(app)
                    }
                },
                onLongPress = { app -> if (pick == null) menu = MenuTarget.DrawerApp(app) },
                onVoiceSearch = actions.voiceSearch,
            )
        }

        openFolder?.let { folder ->
            FolderPopup(
                folder = folder,
                apps = layout.folder(folder).mapNotNull { byKey[it] },
                onLaunch = { app ->
                    openFolder = null
                    actions.launch(app)
                },
                onLongPress = { app -> menu = MenuTarget.FolderApp(folder, app) },
                onDismiss = { openFolder = null },
            )
        }

        menu?.let { target ->
            val (title, list) = buildMenu(target)
            HudMenu(title = title, actions = list, onDismiss = { menu = null })
        }
    }
}