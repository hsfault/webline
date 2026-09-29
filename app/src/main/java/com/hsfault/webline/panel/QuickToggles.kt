package com.hsfault.webline.panel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.res.Resources
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/** Reads and changes the quick-toggle states shown in the panel. */
class QuickToggles(private val context: Context) {

    private val resolver = context.contentResolver
    private val wifiManager: WifiManager? = context.applicationContext.getSystemService(WifiManager::class.java)
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val camera = context.getSystemService(CameraManager::class.java)

    @Suppress("DEPRECATION")
    private val bluetooth: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val torchId: String? = try {
        camera.cameraIdList.firstOrNull {
            camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) {
        null
    }

    @SuppressLint("DiscouragedApi")
    private val maxBrightness: Int = run {
        val res = Resources.getSystem()
        val id = res.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android")
        (if (id != 0) res.getInteger(id) else 255).coerceAtLeast(1)
    }

    var wifi by mutableStateOf(false)
        private set
    var mobileData by mutableStateOf(false)
        private set
    var bluetoothOn by mutableStateOf(false)
        private set
    var torch by mutableStateOf(false)
        private set
    var vibrate by mutableStateOf(false)
        private set
    var airplane by mutableStateOf(false)
        private set
    var brightness by mutableFloatStateOf(0.5f)
        private set

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchId) torch = enabled
        }
    }

    fun start() {
        try {
            camera.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            // no camera service
        }
        refresh()
    }

    fun stop() {
        try {
            camera.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            // already removed
        }
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        wifi = try {
            wifiManager?.isWifiEnabled == true
        } catch (e: Exception) {
            false
        }
        mobileData = try {
            connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } catch (e: Exception) {
            false
        }
        bluetoothOn = try {
            bluetooth?.isEnabled == true
        } catch (e: SecurityException) {
            false
        }
        vibrate = audio.ringerMode == AudioManager.RINGER_MODE_VIBRATE
        airplane = Settings.Global.getInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        brightness = (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, maxBrightness / 2) /
            maxBrightness.toFloat()).coerceIn(0f, 1f)
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun toggleBluetooth(): Boolean {
        val adapter = bluetooth ?: return false
        return try {
            if (adapter.isEnabled) adapter.disable() else adapter.enable()
            bluetoothOn = !bluetoothOn
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun toggleTorch(): Boolean {
        val id = torchId ?: return false
        return try {
            camera.setTorchMode(id, !torch)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun toggleVibrate(): Boolean = try {
        audio.ringerMode = if (vibrate) AudioManager.RINGER_MODE_NORMAL else AudioManager.RINGER_MODE_VIBRATE
        vibrate = !vibrate
        true
    } catch (e: SecurityException) {
        false
    }

    fun canWriteSettings(): Boolean = Settings.System.canWrite(context)

    /** Returns false when "Modify system settings" hasn't been granted yet. */
    fun setBrightness(value: Float): Boolean {
        brightness = value.coerceIn(0f, 1f)
        if (!canWriteSettings()) return false
        return try {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (brightness * maxBrightness).roundToInt().coerceIn(1, maxBrightness),
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}