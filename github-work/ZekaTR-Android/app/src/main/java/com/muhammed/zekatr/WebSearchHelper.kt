package com.muhammed.zekatr

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * GERCEK web arama katmani.
 *
 * Onemli / durust not:
 *  - Bu, uygulamanin "her zaman internete baglanmasi" degil, kullanicinin
 *    Ayarlar > Web Arama'yi ACIKCA actigi durumlarda calisan, istege bagli (opt-in)
 *    bir ozelliktir. Prefs.webSearchEnabled false ise hicbir agdan istek atilmaz.
 *  - Varsayilan kaynak DuckDuckGo Instant Answer API'sidir (ucretsiz, API anahtari
 *    gerektirmez) ama bu API tam bir arama motoru degildir; cogunlukla Wikipedia
 *    tabanli kisa ozetler dondurur. Daha genis/guncel sonuclar istersen
 *    SETTINGS.md dosyasindaki adimlari izleyip kendi Bing/Brave/Serp API anahtarini
 *    girebilirsin (apiKeyOverride).
 *  - Bu siniflar "kesintisiz arka planda internette gezinen, saniyede bir kendini
 *    guncelleyen" bir yapi DEGILDIR. Boyle bir seyi Android isletim sistemi zaten
 *    izin vermez (pil/Doze kisitlamalari) ve bunu var gibi gostermek yaniltici olurdu.
 */
object WebSearchHelper {

    data class SearchResult(val title: String, val summary: String, val sourceUrl: String?)

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    /** Senkron calisir - mutlaka arka plan thread'inden (Dispatchers.IO) cagirilmali. */
    fun search(query: String, apiKeyOverride: String? = null): SearchResult? {
        return try {
            if (!apiKeyOverride.isNullOrBlank()) {
                searchWithBraveApi(query, apiKeyOverride)
            } else {
                searchDuckDuckGo(query)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun searchDuckDuckGo(query: String): SearchResult? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val json = JSONObject(body)
            val abstractText = json.optString("AbstractText", "")
            val heading = json.optString("Heading", query)
            val sourceUrl = json.optString("AbstractURL", "").ifBlank { null }
            if (abstractText.isNotBlank()) {
                return SearchResult(heading, abstractText, sourceUrl)
            }
            // AbstractText bossa, ilgili konularin ilkine bak
            val related = json.optJSONArray("RelatedTopics")
            if (related != null && related.length() > 0) {
                val first = related.optJSONObject(0)
                val text = first?.optString("Text", "") ?: ""
                val firstUrl = first?.optString("FirstURL", "")
                if (text.isNotBlank()) return SearchResult(heading, text, firstUrl)
            }
            return null
        }
    }

    /** Kullanici kendi Brave Search API anahtarini girerse daha zengin sonuc icin kullanilir. */
    private fun searchWithBraveApi(query: String, apiKey: String): SearchResult? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.search.brave.com/res/v1/web/search?q=$encoded"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Subscription-Token", apiKey)
            .addHeader("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val json = JSONObject(body)
            val web = json.optJSONObject("web") ?: return null
            val results = web.optJSONArray("results") ?: return null
            if (results.length() == 0) return null
            val first = results.getJSONObject(0)
            return SearchResult(
                title = first.optString("title", query),
                summary = first.optString("description", ""),
                sourceUrl = first.optString("url", null)
            )
        }
    }
}
