package com.muhammed.zekatr

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Bir URL icin gercek link onizlemesi (baslik, aciklama, gorsel) cikarir.
 * Sayfanin HTML'indeki <meta property="og:..."> etiketlerini okur.
 * Sadece kullanici bir link paylastiginda ve Ayarlar > Web Erisimi acikken calisir.
 */
object LinkPreviewHelper {

    data class LinkPreview(val url: String, val title: String?, val description: String?, val imageUrl: String?)

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private fun metaTag(html: String, property: String): String? {
        val pattern = Pattern.compile(
            "<meta[^>]+property=[\"']$property[\"'][^>]+content=[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val m = pattern.matcher(html)
        if (m.find()) return m.group(1)
        // content ve property sirasi ters olabilir
        val pattern2 = Pattern.compile(
            "<meta[^>]+content=[\"']([^\"']*)[\"'][^>]+property=[\"']$property[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val m2 = pattern2.matcher(html)
        if (m2.find()) return m2.group(1)
        return null
    }

    private fun titleTag(html: String): String? {
        val m = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
        return if (m.find()) m.group(1)?.trim() else null
    }

    /** Senkron - arka plan thread'inden cagir. */
    fun fetchPreview(url: String): LinkPreview? {
        return try {
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (ZekaTR)").build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string()?.take(120_000) ?: return null
                val title = metaTag(html, "og:title") ?: titleTag(html)
                val description = metaTag(html, "og:description")
                val image = metaTag(html, "og:image")
                LinkPreview(url, title, description, image)
            }
        } catch (e: Exception) {
            null
        }
    }
}
