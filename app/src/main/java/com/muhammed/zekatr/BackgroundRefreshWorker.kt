package com.muhammed.zekatr

import android.content.Context
import androidx.work.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * DURUST NOT (lutfen oku):
 * Kullanicidan gelen istek "uygulama hicbir zaman kapanmadan, her saniye/salise
 * arka planda internetten binlerce kaynagi tarayip kendini surekli guncellesin"
 * seklindeydi. Bu, normal bir Android uygulamasi icin GERCEKLESTIRILEMEZ:
 *   - Android isletim sistemi (Doze / App Standby / arka plan calisma sinirlari)
 *     hicbir uygulamanin surekli, saniyelik arka plan islemi yapmasina izin vermez.
 *   - Google Play Store politikalari da bunu yasaklar.
 *   - Boyle calisiyormus gibi sahte bir sayac/animasyon koymak kullaniciyi
 *     kandirmak olur; bunu yapmiyorum.
 *
 * Bunun yerine GERCEKTEN CALISAN, dogru olan alternatif:
 * WorkManager ile PERIYODIK (en sik ~15 dakikada bir, sistemin izin verdigi
 * araliklarla) bir arka plan gorevi. Kullanici Ayarlar'dan bunu ACTIKCA
 * etkinlestirmeli (varsayilan KAPALI). Calistiginda kucuk, sabit bir kaynak
 * listesinden (asagida) TEK bir rastgele "gunun kelimesi" tanimi ceker ve
 * yerel hafizaya (LearnedData) ekler. Uygulama kapatildiginda / cihaz Doze
 * modundayken sistem bu gorevi geciktirebilir veya atlayabilir - bu normaldir.
 */
class BackgroundRefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        if (!prefs.backgroundRefreshEnabled) return Result.success()

        return try {
            val term = VETTED_TERMS.random()
            val client = OkHttpClient()
            // Vikipedi'nin resmi, ucretsiz "sayfa ozeti" REST API'si - API anahtari gerekmez.
            val encodedTerm = java.net.URLEncoder.encode(term, "UTF-8")
            val url = "https://tr.wikipedia.org/api/rest_v1/page/summary/$encodedTerm"
            val request = Request.Builder().url(url).header("User-Agent", "ZekaTR/1.0").build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val extract = json.optString("extract", "")
                        if (extract.isNotBlank()) {
                            val memory = LearnedData(applicationContext)
                            memory.teach(term.lowercase(), extract.take(600))
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "zekatr_background_refresh"

        /** Kucuk, sabit ve degistirilebilir bir "ogrenilecek terim" listesi.
         *  "Binlerce kaynagi ayni anda tarama" yerine, gercekci ve olculebilir bir kapsam. */
        private val VETTED_TERMS = listOf(
            "Türkiye", "Yapay zeka", "Python (programlama dili)", "Anadolu",
            "İstanbul", "Ankara", "Atatürk", "Osmanlı İmparatorluğu",
            "Matematik", "Fizik", "Kimya", "Astronomi", "Tarih", "Coğrafya"
        )

        /** Kullanici Ayarlar'dan acikca etkinlestirdiginde cagrilir. */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            // WorkManager'in izin verdigi EN KISA periyodik aralik 15 dakikadir.
            val request = PeriodicWorkRequestBuilder<BackgroundRefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
