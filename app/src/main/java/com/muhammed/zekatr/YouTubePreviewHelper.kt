package com.muhammed.zekatr

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * YouTube linkleri icin GERCEK onizleme verisi ceker.
 * YouTube'un herkese acik, API-anahtari GEREKTIRMEYEN oEmbed servisini kullanir:
 * https://www.youtube.com/oembed?url=...&format=json
 * Bu servis sadece video basligini, kanal adini ve kucuk resmini dondurur;
 * videoyu indirmez, gizli/private videolarda calismaz.
 */
object YouTubePreviewHelper {

    data class YouTubePreview(
        val videoId: String,
        val title: String,
        val authorName: String,
        val thumbnailUrl: String
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val idPattern = Pattern.compile(
        "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/shorts/)([a-zA-Z0-9_-]{6,})"
    )

    fun extractVideoId(url: String): String? {
        val m = idPattern.matcher(url)
        return if (m.find()) m.group(1) else null
    }

    fun isYouTubeUrl(url: String): Boolean = extractVideoId(url) != null

    /** Senkron - arka plan thread'inden cagir. */
    fun fetchPreview(url: String): YouTubePreview? {
        val videoId = extractVideoId(url) ?: return null
        return try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder().url(oembedUrl).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                YouTubePreview(
                    videoId = videoId,
                    title = json.optString("title", "YouTube videosu"),
                    authorName = json.optString("author_name", ""),
                    thumbnailUrl = json.optString("thumbnail_url", "https://img.youtube.com/vi/$videoId/hqdefault.jpg")
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
