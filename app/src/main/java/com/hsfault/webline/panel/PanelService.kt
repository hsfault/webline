package com.hsfault.webline.panel

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hsfault.webline.data.EmblemStore
import com.hsfault.webline.data.UserPrefs
import com.hsfault.webline.data.WeatherRepository
import com.hsfault.webline.media.MediaRepository
import com.hsfault.webline.util.SystemActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PanelState {
    var open by mutableStateOf(false)
}

/** Everything the panel UI can ask the service to do. */
class PanelActions(
    val close: () -> Unit,
    val openSettings: () -> Unit,
    val openSystemPanel: () -> Unit,
    val openPlayer: () -> Unit,
    val prev: () -> Unit,
    val playPause: () -> Unit,
    val next: () -> Unit,
    val wifi: () -> Unit,
    val mobileData: () -> Unit,
    val bluetooth: () -> Unit,
    val torch: () -> Unit,
    val vibrate: () -> Unit,
    val airplane: () -> Unit,
    val brightness: (Float) -> Unit,
    val openNotification: (PanelNotification) -> Unit,
    val dismissNotification: (PanelNotification) -> Unit,
    val clearAll: () -> Unit,
)

/**
 * Accessibility service that owns three overlay windows:
 *  - the notification panel (kept composed, shrunk to 1px while closed so opening is instant)
 *  - a thin invisible strip over the status bar that detects the pull-down swipe
 *  - the lock cover (placed when the screen turns off, splits away on unlock)
 */
class PanelService : AccessibilityService() {

    private var wm: WindowManager? = null
    private val owner = OverlayLifecycleOwner()
    private val state = PanelState()
    private val lockState = LockState()
    private val main = Handler(Looper.getMainLooper())
    private var scope: CoroutineScope? = null

    private var trigger: View? = null
    private var panel: ComposeView? = null
    private var lockView: ComposeView? = null
    private var panelShown = false
    private var lockShown = false
    private var previewing = false
    private var openedAt = 0L
    private var askedWriteSettings = false
    private var receiverRegistered = false

    private var media: MediaRepository? = null
    private var toggles: QuickToggles? = null
    private var weatherRepo: WeatherRepository? = null
    private var userPrefs: UserPrefs? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    lockState.screenOn = false
                    closePanel()
                    if (userPrefs?.lockCoverEnabled == true) showCover(preview = false)
                }
                Intent.ACTION_SCREEN_ON -> {
                    lockState.screenOn = true
                    if (lockShown) {
                        refreshCoverData()
                        // Woke up already unlocked (fingerprint from screen-off, or lock grace period): split now.
                        if (!isLocked()) startSplit()
                    }
                }
                Intent.ACTION_USER_PRESENT -> if (lockShown) startSplit()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (panel != null) return
        instance = this

        val windowManager = getSystemService(WindowManager::class.java)
        wm = windowManager
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        owner.start()

        val mediaRepo = MediaRepository(this).also { it.start() }
        val quick = QuickToggles(this).also { it.start() }
        media = mediaRepo
        toggles = quick
        weatherRepo = WeatherRepository(this)
        userPrefs = UserPrefs(this)

        val actions = buildActions(mediaRepo, quick)
        val statusBarPx = systemDimen("status_bar_height", 24)
        val navBarPx = systemDimen("navigation_bar_height", 48)

        // 1. Notification panel
        val panelView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                PanelRoot(
                    open = state.open,
                    media = mediaRepo,
                    toggles = quick,
                    statusBarPx = statusBarPx,
                    actions = actions,
                    onHidden = { hidePanelWindow() },
                )
            }
        }
        panel = panelView
        windowManager.addView(panelView, hiddenParams())

        // 2. Pull-down strip
        addTrigger(windowManager, statusBarPx)

        // 3. Lock cover (added last so it sits on top of everything)
        val coverView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                LockCoverUi(
                    state = lockState,
                    media = mediaRepo,
                    statusBarPx = statusBarPx,
                    navBarPx = navBarPx,
                    onSwipeUp = { startSplit() },
                    onSplitDone = { hideCoverNow() },
                    onDoubleTap = {
                        if (previewing) startSplit() else performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    },
                    onPlayPause = {
                        if (!mediaRepo.playPause()) SystemActions.mediaKey(this@PanelService, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    },
                    onNext = {
                        if (!mediaRepo.next()) SystemActions.mediaKey(this@PanelService, KeyEvent.KEYCODE_MEDIA_NEXT)
                    },
                    onTick = { readBattery() },
                )
            }
        }
        lockView = coverView
        windowManager.addView(coverView, hiddenParams())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    // ---------- Notification panel ----------

    fun openPanel() {
        val windowManager = wm ?: return
        val view = panel ?: return
        // On the lock screen, hand over to the system shade so notifications stay private.
        if (isLocked()) {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            return
        }
        if (!panelShown) {
            windowManager.updateViewLayout(view, shownParams())
            panelShown = true
        }
        askedWriteSettings = false
        toggles?.refresh()
        NotificationStore.refresh()
        media?.start()
        openedAt = SystemClock.uptimeMillis()
        state.open = true
    }

    fun closePanel() {
        state.open = false
    }

    private fun hidePanelWindow() {
        if (state.open || !panelShown) return
        val windowManager = wm ?: return
        val view = panel ?: return
        windowManager.updateViewLayout(view, hiddenParams())
        panelShown = false
    }

    // ---------- Lock cover ----------

    /** Shows the cover. preview = true is used from Setup while the phone is unlocked. */
    fun showCover(preview: Boolean) {
        val windowManager = wm ?: return
        val view = lockView ?: return
        previewing = preview
        refreshCoverData()
        lockState.splitting = false
        lockState.showToken++
        lockState.visible = true
        if (!lockShown) {
            windowManager.updateViewLayout(view, coverParams())
            lockShown = true
        }
    }

    private fun startSplit() {
        if (!lockShown || lockState.splitting) return
        lockState.splitting = true
    }

    private fun hideCoverNow() {
        lockState.visible = false
        lockState.splitting = false
        previewing = false
        if (!lockShown) return
        val windowManager = wm ?: return
        val view = lockView ?: return
        windowManager.updateViewLayout(view, hiddenParams())
        lockShown = false
    }

    private fun refreshCoverData() {
        lockState.emblem = EmblemStore.load(this)
        readBattery()
        val weather = weatherRepo ?: return
        lockState.weather = weather.now
        scope?.launch {
            weather.refresh()
            lockState.weather = weather.now
        }
    }

    private fun readBattery() {
        val bm = getSystemService(BatteryManager::class.java) ?: return
        lockState.battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        lockState.charging = bm.isCharging
    }

    private fun isLocked(): Boolean = getSystemService(KeyguardManager::class.java).isKeyguardLocked

    /** Apps that must never be blocked by the cover (calls, alarms, camera from lock screen). */
    private fun isInterruptingApp(pkg: String): Boolean {
        val p = pkg.lowercase()
        return listOf("incallui", "dialer", "telecom", "com.android.phone", "clock", "alarm", "camera").any { p.contains(it) }
    }

    // ---------- Actions ----------

    private fun buildActions(mediaRepo: MediaRepository, quick: QuickToggles) = PanelActions(
        close = { closePanel() },
        openSettings = { launch(Intent(Settings.ACTION_SETTINGS)) },
        openSystemPanel = {
            closePanel()
            performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        },
        openPlayer = { if (mediaRepo.openPlayer()) closePanel() },
        prev = { if (!mediaRepo.previous()) SystemActions.mediaKey(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
        playPause = { if (!mediaRepo.playPause()) SystemActions.mediaKey(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
        next = { if (!mediaRepo.next()) SystemActions.mediaKey(this, KeyEvent.KEYCODE_MEDIA_NEXT) },
        wifi = { launch(Intent(Settings.Panel.ACTION_WIFI)) },
        mobileData = { launch(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)) },
        bluetooth = {
            if (quick.toggleBluetooth()) {
                main.postDelayed({ quick.refresh() }, 900)
            } else {
                launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        },
        torch = {
            if (!quick.toggleTorch()) Toast.makeText(this, "Flashlight is busy", Toast.LENGTH_SHORT).show()
        },
        vibrate = { if (!quick.toggleVibrate()) launch(Intent(Settings.ACTION_SOUND_SETTINGS)) },
        airplane = { launch(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)) },
        brightness = { value ->
            if (!quick.setBrightness(value) && !askedWriteSettings) {
                askedWriteSettings = true
                Toast.makeText(this, "Allow WEBLINE to modify system settings to control brightness", Toast.LENGTH_LONG).show()
                launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            }
        },
        openNotification = { n ->
            closePanel()
            try {
                n.intent?.send()
            } catch (e: Exception) {
                // the app cancelled its intent
            }
            if (n.autoCancel) NotificationStore.dismiss(n.key)
        },
        dismissNotification = { n -> NotificationStore.dismiss(n.key) },
        clearAll = { NotificationStore.clearAll() },
    )

    private fun launch(intent: Intent) {
        closePanel()
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open that screen", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Windows ----------

    private fun baseParams(width: Int, height: Int, extraFlags: Int) = WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        extraFlags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        fitInsetsTypes = 0
    }

    /** Closed: a 1px untouchable window, so it costs nothing and blocks nothing. */
    private fun hiddenParams() = baseParams(1, 1, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

    /** Panel open: full screen over the status bar, stopping above the navigation buttons. */
    private fun shownParams() = baseParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        0,
    ).apply { fitInsetsTypes = WindowInsets.Type.navigationBars() }

    /** Lock cover: the entire screen, status and navigation bars included. */
    private fun coverParams() = baseParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        0,
    )

    @SuppressLint("ClickableViewAccessibility")
    private fun addTrigger(windowManager: WindowManager, heightPx: Int) {
        val threshold = 16f * resources.displayMetrics.density
        var startY = 0f
        var fired = false
        val view = View(this)
        view.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = e.rawY
                    fired = false
                }
                MotionEvent.ACTION_MOVE -> if (!fired && e.rawY - startY > threshold) {
                    fired = true
                    openPanel()
                }
            }
            true
        }
        windowManager.addView(view, baseParams(WindowManager.LayoutParams.MATCH_PARENT, heightPx, 0))
        trigger = view
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    private fun systemDimen(name: String, fallbackDp: Int): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else (fallbackDp * resources.displayMetrics.density).toInt()
    }

    // ---------- Accessibility callbacks ----------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString().orEmpty()

        // A call, alarm or camera came up over the lock screen: get out of the way.
        if (lockShown && !lockState.splitting && isInterruptingApp(pkg)) hideCoverNow()

        // Home pressed or another app opened: close the panel.
        if (state.open && SystemClock.uptimeMillis() - openedAt > 600) closePanel()
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || !state.open) return false
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) closePanel()
            return true
        }
        return false
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        if (instance === this) instance = null
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
        val windowManager = wm
        trigger?.let { view -> runCatching { windowManager?.removeView(view) } }
        trigger = null
        panel?.let { view -> runCatching { windowManager?.removeView(view) } }
        panel = null
        lockView?.let { view -> runCatching { windowManager?.removeView(view) } }
        lockView = null
        panelShown = false
        lockShown = false
        media?.stop()
        media = null
        toggles?.stop()
        toggles = null
        scope?.cancel()
        scope = null
        owner.destroy()
    }

    companion object {
        @Volatile
        var instance: PanelService? = null
            private set
    }
}