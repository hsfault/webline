package com.hsfault.webline.data

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.util.Locale

data class DeviceStats(
    val storageUsed: Long,
    val storageTotal: Long,
    val ramUsed: Long,
    val ramTotal: Long,
) {
    companion object {
        fun read(context: Context): DeviceStats {
            val fs = StatFs(Environment.getDataDirectory().path)
            val total = fs.totalBytes
            val free = fs.availableBytes
            val am = context.getSystemService(ActivityManager::class.java)
            val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            return DeviceStats(
                storageUsed = total - free,
                storageTotal = total,
                ramUsed = mem.totalMem - mem.availMem,
                ramTotal = mem.totalMem,
            )
        }
    }
}

/** Decimal gigabytes, the same way phone boxes count them. */
fun gb(bytes: Long): String = String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)