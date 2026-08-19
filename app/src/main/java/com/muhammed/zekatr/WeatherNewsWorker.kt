package com.muhammed.zekatr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.work.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WeatherNewsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val client = OkHttpClient()
    override fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        if (!prefs.locationEnabled) return Result.success()
        try { updateWeather(prefs) } catch (_: Exception) { }
        try { NewsNotifier(applicationContext).refresh() } catch (_: Exception) { }
        SpecialDaysNotifier(applicationContext).check()
        return Result.success()
    }

    private fun updateWeather(prefs: Prefs) {
        var lat = prefs.weatherLat
        var lon = prefs.weatherLon
        if (lat.isNaN() || lon.isNaN()) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
            val lm = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            val loc = providers.mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }.maxByOrNull { it.time } ?: return
            lat = loc.latitude; lon = loc.longitude
            prefs.weatherLat = lat; prefs.weatherLon = lon
        }
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,is_day&timezone=auto"
        val req = Request.Builder().url(url).header("User-Agent", "ZekaTR").build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return
            val current = JSONObject(response.body?.string().orEmpty()).optJSONObject("current") ?: return
            val temp = current.optDouble("temperature_2m", Double.NaN)
            val code = current.optInt("weather_code", -1)
            val previous = prefs.weatherLastCode
            val now = System.currentTimeMillis()
            val changed = previous == -999 || previous != code
            val twiceDaily = now - prefs.weatherLastNotificationAt >= 12 * 60 * 60 * 1000L
            if (changed || twiceDaily) {
                val message = WeatherText.message(code, temp)
                NotificationHelper.showWeather(applicationContext, message)
                prefs.weatherLastCode = code
                prefs.weatherLastNotificationAt = now
            }
        }
    }

    companion object {
        private const val WORK = "zekatr_weather_news"
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<WeatherNewsWorker>(30, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}

object WeatherText {
    fun message(code: Int, temp: Double): String {
        val base = when (code) {
            0 -> "Hava açık ve güneşli görünüyor. Güneş kremi sürmeyi unutma."
            1,2 -> "Hava çoğunlukla açık, zaman zaman bulutlu."
            3 -> "Bugün hava kapalı görünüyor."
            in 45..48 -> "Sisli bir hava var; görüş mesafesine dikkat et."
            in 51..67 -> "Yağış ihtimali var. Şemsiyeni yanında bulundurman iyi olabilir."
            in 71..77 -> "Kar yağışı görülüyor. Dışarı çıkacaksan hazırlıklı ol."
            in 80..82 -> "Sağanak yağış bekleniyor."
            in 95..99 -> "Gök gürültülü fırtına ihtimali var; dikkatli ol."
            else -> "Hava durumu değişmiş görünüyor."
        }
        return "$base${if (!temp.isNaN()) " Şu an yaklaşık ${temp}°C." else ""}"
    }
}

class NewsNotifier(private val context: Context) {
    private val client = OkHttpClient()
    fun refresh() {
        val request = Request.Builder().url("https://news.google.com/rss?hl=tr&gl=TR&ceid=TR:tr").header("User-Agent", "ZekaTR").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return
            val xml = response.body?.string().orEmpty()
            val title = Regex("<item>.*?<title>(.*?)</title>.*?<link>(.*?)</link>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(xml)?.groupValues?.getOrNull(1)?.replace("<![CDATA[", "")?.replace("]]>", "") ?: return
            NotificationHelper.showNews(context, "Son dakika: $title")
        }
    }
}


class SpecialDaysNotifier(private val context: Context) {
    fun check() {
        val now = java.util.Calendar.getInstance()
        val month = now.get(java.util.Calendar.MONTH) + 1
        val day = now.get(java.util.Calendar.DAY_OF_MONTH)
        val year = now.get(java.util.Calendar.YEAR)
        val fixed = mapOf(
            "1/1" to "🎉 Mutlu yıllar!", "4/23" to "🇹🇷 23 Nisan Ulusal Egemenlik ve Çocuk Bayramı kutlu olsun!",
            "5/1" to "🌿 Emek ve Dayanışma Günü kutlu olsun!", "5/19" to "🇹🇷 19 Mayıs Atatürk'ü Anma, Gençlik ve Spor Bayramı kutlu olsun!",
            "7/15" to "🇹🇷 15 Temmuz Demokrasi ve Millî Birlik Günü.", "8/30" to "🇹🇷 30 Ağustos Zafer Bayramı kutlu olsun!",
            "10/29" to "🇹🇷 Cumhuriyet Bayramı kutlu olsun!"
        )
        fixed["$month/$day"]?.let { NotificationHelper.showSpecial(context, it) }
        val cal = now.clone() as java.util.Calendar
        if (month == 5 && cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY && day in 8..14) NotificationHelper.showSpecial(context, "💐 Anneler Günü kutlu olsun!")
        if (month == 6 && cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY && day in 15..21) NotificationHelper.showSpecial(context, "💙 Babalar Günü kutlu olsun!")
        if (year == 2026) {
            when (month to day) {
                3 to 20, 3 to 21, 3 to 22 -> NotificationHelper.showSpecial(context, "🌙 Ramazan Bayramı kutlu olsun!")
                5 to 27, 5 to 28, 5 to 29, 5 to 30 -> NotificationHelper.showSpecial(context, "🐑 Kurban Bayramı kutlu olsun!")
            }
        }
    }
}
