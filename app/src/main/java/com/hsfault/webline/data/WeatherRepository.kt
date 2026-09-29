package com.hsfault.webline.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

data class WeatherNow(
    val tempC: Int,
    val code: Int,
    val isDay: Boolean,
    val place: String,
    val fetchedAt: Long,
)

enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, RAIN, STORM, SNOW }

fun skyOf(code: Int): Sky = when (code) {
    0, 1 -> Sky.CLEAR
    2 -> Sky.PARTLY
    3 -> Sky.CLOUDY
    45, 48 -> Sky.FOG
    in 51..67, in 80..82 -> Sky.RAIN
    in 71..77, 85, 86 -> Sky.SNOW
    in 95..99 -> Sky.STORM
    else -> Sky.CLOUDY
}

fun conditionText(code: Int): String = when (code) {
    0 -> "Clear Sky"
    1 -> "Mostly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    in 51..57 -> "Drizzle"
    in 61..67 -> "Rain"
    in 71..77 -> "Snow"
    in 80..82 -> "Showers"
    85, 86 -> "Snow Showers"
    in 95..99 -> "Thunderstorm"
    else -> "Cloudy"
}

/** Current weather from Open-Meteo (free, no API key). Refreshes at most every 30 minutes. */
class WeatherRepository(context: Context) {

    private val prefs = context.getSharedPreferences("webline_weather", Context.MODE_PRIVATE)

    var now by mutableStateOf(load())
        private set

    val city: String
        get() = prefs.getString(KEY_CITY, DEFAULT_CITY) ?: DEFAULT_CITY

    fun setCity(value: String) {
        prefs.edit()
            .putString(KEY_CITY, value.trim().ifEmpty { DEFAULT_CITY })
            .remove(KEY_LAT)
            .remove(KEY_LON)
            .remove(KEY_GEO_NAME)
            .putLong(KEY_TIME, 0L)
            .apply()
    }

    suspend fun refresh(force: Boolean = false) {
        val last = prefs.getLong(KEY_TIME, 0L)
        if (!force && System.currentTimeMillis() - last < STALE_MS) {
            now = load()
            return
        }
        val fresh = withContext(Dispatchers.IO) { runCatching { fetch() }.getOrNull() } ?: return
        save(fresh)
        now = fresh
    }

    private fun fetch(): WeatherNow {
        var lat = prefs.getFloat(KEY_LAT, Float.NaN)
        var lon = prefs.getFloat(KEY_LON, Float.NaN)
        var place = prefs.getString(KEY_GEO_NAME, null)

        if (lat.isNaN() || lon.isNaN() || place == null) {
            val q = URLEncoder.encode(city, "UTF-8")
            val geo = getJson("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=en&format=json")
            val first = geo.getJSONArray("results").getJSONObject(0)
            lat = first.getDouble("latitude").toFloat()
            lon = first.getDouble("longitude").toFloat()
            place = first.getString("name")
            prefs.edit().putFloat(KEY_LAT, lat).putFloat(KEY_LON, lon).putString(KEY_GEO_NAME, place).apply()
        }

        val data = getJson(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code,is_day&timezone=auto"
        )
        val current = data.getJSONObject("current")
        return WeatherNow(
            tempC = current.getDouble("temperature_2m").roundToInt(),
            code = current.getInt("weather_code"),
            isDay = current.getInt("is_day") == 1,
            place = place ?: city,
            fetchedAt = System.currentTimeMillis(),
        )
    }

    private fun getJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        try {
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private fun save(w: WeatherNow) {
        prefs.edit()
            .putInt(KEY_TEMP, w.tempC)
            .putInt(KEY_CODE, w.code)
            .putBoolean(KEY_DAY, w.isDay)
            .putString(KEY_PLACE, w.place)
            .putLong(KEY_TIME, w.fetchedAt)
            .apply()
    }

    private fun load(): WeatherNow? {
        if (!prefs.contains(KEY_TEMP)) return null
        return WeatherNow(
            tempC = prefs.getInt(KEY_TEMP, 0),
            code = prefs.getInt(KEY_CODE, 2),
            isDay = prefs.getBoolean(KEY_DAY, true),
            place = prefs.getString(KEY_PLACE, "") ?: "",
            fetchedAt = prefs.getLong(KEY_TIME, 0L),
        )
    }

    companion object {
        const val DEFAULT_CITY = "Gujranwala"
        private const val STALE_MS = 30 * 60 * 1000L
        private const val KEY_CITY = "city"
        private const val KEY_LAT = "lat"
        private const val KEY_LON = "lon"
        private const val KEY_GEO_NAME = "geo_name"
        private const val KEY_TEMP = "temp"
        private const val KEY_CODE = "code"
        private const val KEY_DAY = "day"
        private const val KEY_PLACE = "place"
        private const val KEY_TIME = "time"
    }
}