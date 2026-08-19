package com.muhammed.zekatr

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.muhammed.zekatr.databinding.ActivityWeatherBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * GERCEK veri: Open-Meteo (ucretsiz, API anahtari gerektirmez).
 * Referans ekran goruntulerindeki gorunumu (hero kart + fon fotografi,
 * saatlik satir, sensor izgarasi, 7 gunluk liste) ayni yapida uretir.
 * "SISTEM DOGRULADI" gibi kozmetik yazilar yerine gercek zaman damgasi
 * ve gercek API verisi kullanilir - yanitici olmayacak sekilde.
 */
class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherBinding
    private val client = OkHttpClient()

    private val weatherIcons = mapOf(
        0 to "☀️", 1 to "🌤️", 2 to "⛅", 3 to "☁️",
        45 to "🌫️", 48 to "🌫️",
        51 to "🌦️", 53 to "🌦️", 55 to "🌦️",
        61 to "🌧️", 63 to "🌧️", 65 to "🌧️",
        71 to "🌨️", 73 to "🌨️", 75 to "🌨️",
        80 to "🌦️", 81 to "🌧️", 82 to "⛈️",
        95 to "⛈️", 96 to "⛈️", 99 to "⛈️"
    )

    private fun iconFor(code: Int) = weatherIcons[code] ?: "🌡️"

    private fun conditionText(code: Int): String = when (code) {
        0 -> "Açık"; 1, 2 -> "Parçalı bulutlu"; 3 -> "Kapalı"
        45, 48 -> "Sisli"; 51, 53, 55 -> "Çisenti"
        61, 63, 65 -> "Yağmurlu"; 71, 73, 75 -> "Karlı"
        80, 81 -> "Sağanak yağış"; 82 -> "Kuvvetli sağanak"
        95, 96, 99 -> "Gök gürültülü fırtına"
        else -> "Belirsiz"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBackWeather.setOnClickListener { finish() }
        binding.btnRefreshWeather.setOnClickListener { load() }
        load()
    }

    private fun load() {
        binding.textWeatherCity.text = "Konum alınıyor…"
        lifecycleScope.launch {
            val (lat, lon) = withContext(Dispatchers.IO) { resolveLocation() } ?: run {
                binding.textWeatherCity.text = "Konum alınamadı"
                binding.textWeatherCondition.text = "Ayarlar > Konum + hava durumu iznini açman gerekiyor."
                return@launch
            }
            val data = withContext(Dispatchers.IO) { fetch(lat, lon) }
            if (data == null) {
                binding.textWeatherCity.text = "Veri alınamadı"
                binding.textWeatherCondition.text = "İnternet bağlantısını kontrol et."
                return@launch
            }
            render(data)
        }
    }

    private fun resolveLocation(): Pair<Double, Double>? {
        val prefs = Prefs(this)
        if (!prefs.weatherLat.isNaN() && !prefs.weatherLon.isNaN()) return prefs.weatherLat to prefs.weatherLon
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val loc = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time } ?: return null
        prefs.weatherLat = loc.latitude; prefs.weatherLon = loc.longitude
        return loc.latitude to loc.longitude
    }

    private data class WeatherData(val json: JSONObject, val cityLabel: String)

    private fun fetch(lat: Double, lon: Double): WeatherData? {
        return try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,weather_code,relative_humidity_2m,wind_speed_10m,wind_direction_10m" +
                "&hourly=temperature_2m,weather_code,visibility,uv_index" +
                "&daily=temperature_2m_max,temperature_2m_min,weather_code" +
                "&timezone=auto&forecast_days=7"
            val req = Request.Builder().url(url).header("User-Agent", "ZekaTR").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string().orEmpty())
                val cityLabel = reverseGeocodeLabel(lat, lon) ?: "Konum · %.2f°N".format(lat)
                WeatherData(json, cityLabel)
            }
        } catch (_: Exception) { null }
    }

    /** Basit, ucretsiz ters-geocoding (Open-Meteo'nun kendi geocoding servisi). Basarisiz olursa koordinat gosterilir. */
    private fun reverseGeocodeLabel(lat: Double, lon: Double): String? {
        return try {
            val url = "https://geocoding-api.open-meteo.com/v1/reverse?latitude=$lat&longitude=$lon&language=tr"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val results = JSONObject(resp.body?.string().orEmpty()).optJSONArray("results") ?: return null
                if (results.length() == 0) return null
                val r = results.getJSONObject(0)
                val name = r.optString("name", "")
                val country = r.optString("country", "")
                if (name.isBlank()) null else if (country.isBlank()) name else "$name, $country"
            }
        } catch (_: Exception) { null }
    }

    private fun render(data: WeatherData) {
        val j = data.json
        val current = j.optJSONObject("current") ?: return
        val temp = current.optDouble("temperature_2m", Double.NaN)
        val feels = current.optDouble("apparent_temperature", Double.NaN)
        val code = current.optInt("weather_code", 0)
        val humidity = current.optInt("relative_humidity_2m", -1)
        val windSpeed = current.optDouble("wind_speed_10m", Double.NaN)
        val windDir = current.optDouble("wind_direction_10m", Double.NaN)

        binding.textWeatherCity.text = data.cityLabel
        binding.textWeatherUpdated.text = "Güncelleme · ${SimpleDateFormat("d MMMM, HH:mm", Locale("tr")).format(Date())}"
        binding.textWeatherTemp.text = if (!temp.isNaN()) "${temp.toInt()}°" else "--°"
        binding.textWeatherCondition.text = "${iconFor(code)} ${conditionText(code)}"
        binding.textWeatherFeelsLike.text = if (!feels.isNaN()) "Hissedilen ${feels.toInt()}°" else ""

        // Saatlik satir (gelecek 6 saat)
        binding.rowHourly.removeAllViews()
        val hourly = j.optJSONObject("hourly")
        val times = hourly?.optJSONArray("time")
        val htemps = hourly?.optJSONArray("temperature_2m")
        val hcodes = hourly?.optJSONArray("weather_code")
        if (times != null) {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).format(Date())
            var startIdx = (0 until times.length()).firstOrNull { times.getString(it) >= nowIso } ?: 0
            val inflater = LayoutInflater.from(this)
            for (i in startIdx until minOf(startIdx + 6, times.length())) {
                val v = inflater.inflate(R.layout.item_hourly_weather, binding.rowHourly, false)
                val hh = times.getString(i).substringAfter('T').take(5)
                v.findViewById<TextView>(R.id.textHourlyTime).text = hh
                v.findViewById<TextView>(R.id.textHourlyIcon).text = iconFor(hcodes?.optInt(i) ?: 0)
                v.findViewById<TextView>(R.id.textHourlyTemp).text = "${htemps?.optDouble(i)?.toInt() ?: "--"}°"
                binding.rowHourly.addView(v)
            }
        }

        // Sensor izgarasi
        binding.gridSensors.removeAllViews()
        val visibility = hourly?.optJSONArray("visibility")?.let { arr -> if (arr.length() > 0) arr.optDouble(0) / 1000.0 else Double.NaN } ?: Double.NaN
        val uv = hourly?.optJSONArray("uv_index")?.let { arr -> if (arr.length() > 0) arr.optDouble(0) else Double.NaN } ?: Double.NaN
        addSensorCard("RÜZGÂR", "🧭", windDirLabel(windDir), if (!windSpeed.isNaN()) "${windSpeed.toInt()} km/sa" else "—")
        addSensorCard("NEM", "💧", if (humidity >= 0) "%$humidity" else "—", humidityLabel(humidity))
        addSensorCard("GÖRÜŞ", "👁️", if (!visibility.isNaN()) "${visibility.toInt()} km" else "—", "")
        addSensorCard("UV İNDEKSİ", "☀️", if (!uv.isNaN()) "${uv.toInt()} ${uvLabel(uv)}" else "—", "")

        // 7 gunluk
        binding.listDaily.removeAllViews()
        val daily = j.optJSONObject("daily")
        val dtimes = daily?.optJSONArray("time")
        val dmax = daily?.optJSONArray("temperature_2m_max")
        val dmin = daily?.optJSONArray("temperature_2m_min")
        val dcodes = daily?.optJSONArray("weather_code")
        if (dtimes != null) {
            val inflater = LayoutInflater.from(this)
            for (i in 0 until dtimes.length()) {
                val v = inflater.inflate(R.layout.item_daily_weather, binding.listDaily, false)
                val dateStr = dtimes.getString(i)
                val dayName = dayLabel(dateStr, i)
                v.findViewById<TextView>(R.id.textDailyDay).text = dayName
                v.findViewById<TextView>(R.id.textDailyIcon).text = iconFor(dcodes?.optInt(i) ?: 0)
                v.findViewById<TextView>(R.id.textDailyTemp).text = "${dmax?.optDouble(i)?.toInt()}° / ${dmin?.optDouble(i)?.toInt()}°"
                binding.listDaily.addView(v)
            }
        }
    }

    private fun windDirLabel(deg: Double): String {
        if (deg.isNaN()) return "—"
        val dirs = listOf("Kuzey", "Kuzeydoğu", "Doğu", "Güneydoğu", "Güney", "Güneybatı", "Batı", "Kuzeybatı")
        return dirs[(((deg + 22.5) / 45).toInt()) % 8]
    }

    private fun humidityLabel(h: Int) = when { h < 0 -> ""; h < 30 -> "Kuru"; h < 60 -> "Dengeli"; else -> "Nemli" }
    private fun uvLabel(uv: Double) = when { uv < 3 -> "Düşük"; uv < 6 -> "Orta"; uv < 8 -> "Yüksek"; else -> "Çok yüksek" }

    private fun dayLabel(iso: String, index: Int): String {
        if (index == 0) return "Bugün"
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)!!
            SimpleDateFormat("EEEE", Locale("tr")).format(date).replaceFirstChar { it.uppercase() }
        } catch (_: Exception) { iso }
    }

    private fun addSensorCard(label: String, icon: String, valueMain: String, valueSub: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.item_sensor_card, binding.gridSensors, false)
        val params = GridLayout.LayoutParams().apply {
            width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(0, 0, 12, 12)
        }
        v.layoutParams = params
        v.findViewById<TextView>(R.id.textSensorLabel).text = label
        v.findViewById<TextView>(R.id.textSensorIcon).text = icon
        v.findViewById<TextView>(R.id.textSensorMain).text = valueMain
        v.findViewById<TextView>(R.id.textSensorSub).text = valueSub
        binding.gridSensors.addView(v)
    }
}
