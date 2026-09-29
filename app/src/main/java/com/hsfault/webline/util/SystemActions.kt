package com.hsfault.webline.util

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.KeyEvent

object SystemActions {

    private const val PHONE_MASTER = "com.transsion.phonemaster"
    private val MUSIC_FALLBACKS = listOf("com.spotify.music", "com.google.android.apps.youtube.music")

    /** Temporary until our own panel arrives: opens the system notification shade. */
    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandNotifications(context: Context) {
        try {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
        } catch (e: Throwable) {
            // Some ROMs block this; swiping from the status bar still works.
        }
    }

    fun isDefaultHome(context: Context): Boolean =
        context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)

    fun openHomeSettings(context: Context) {
        if (!start(context, Intent(Settings.ACTION_HOME_SETTINGS))) {
            start(context, Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
    }

    fun isIgnoringBattery(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)

    @SuppressLint("BatteryLife")
    fun requestIgnoreBattery(context: Context) {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        if (!start(context, direct)) {
            start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    fun openPhoneMaster(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PHONE_MASTER) ?: return false
        return start(context, intent)
    }

    fun openOwnAppInfo(context: Context) {
        start(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    }

    fun goHome(context: Context) {
        start(context, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }

    fun openClock(context: Context) {
        start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }

    fun openMusic(context: Context): Boolean {
        if (start(context, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))) return true
        for (pkg in MUSIC_FALLBACKS) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: continue
            if (start(context, intent)) return true
        }
        return false
    }

    /** Sends play/pause/next/previous to whatever is playing. No permission needed. */
    fun mediaKey(context: Context, keyCode: Int) {
        val audio = context.getSystemService(AudioManager::class.java)
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    fun voiceSearch(context: Context) {
        if (!start(context, Intent(RecognizerIntent.ACTION_WEB_SEARCH))) {
            start(context, Intent(Intent.ACTION_VOICE_COMMAND))
        }
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}