package com.hsfault.webline.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hsfault.webline.ui.theme.Hud
import com.hsfault.webline.ui.theme.HudType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Minute-accurate clock. Updates only on the system's minute tick, so there's no polling. */
@Composable
fun ClockHeader(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val is24 = DateFormat.is24HourFormat(context)
    val time = remember(now, is24) {
        SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(Date(now))
    }
    val date = remember(now) {
        SimpleDateFormat("EEE · dd MMM yyyy", Locale.getDefault()).format(Date(now)).uppercase()
    }

    Column(modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp)) {
        BasicText("WEBLINE // ONLINE", style = HudType.label.copy(color = Hud.Crimson))
        BasicText(time, style = HudType.clock)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.width(36.dp).height(8.dp)) {
                val y = size.height / 2f
                drawLine(Hud.Crimson, Offset(0f, y), Offset(size.width - 6.dp.toPx(), y), 1.5.dp.toPx())
                drawCircle(Hud.Glow, 3.dp.toPx(), Offset(size.width - 3.dp.toPx(), y))
            }
            Spacer(Modifier.width(10.dp))
            BasicText(date, style = HudType.label.copy(color = Hud.Light))
        }
    }
}