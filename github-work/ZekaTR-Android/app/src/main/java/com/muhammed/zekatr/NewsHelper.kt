package com.muhammed.zekatr

import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit

data class NewsItem(val title: String, val sourceName: String, val link: String, val pubDate: String?)

/**
 * GERCEK RSS okuyucu. Kaynaklar herkese acik, resmi RSS beslemeleridir.
 * Durustce belirtelim: RSS her zaman "canli son dakika" degildir, kaynagin
 * kendi guncelleme sikligina baglidir (genelde birkaç dakikada bir).
 *
 * NOT: Bu ortamda internet erisimim olmadigi icin asagidaki feed URL'lerini
 * gercek zamanli test edemedim. BBC Turkce feed'i yaygin/dokumante halka
 * acik bir RSS'tir; digerlerini eklemeden once kendi cihazinda Terminal
 * ekranindan `curl -I <url>` ile dogrulaman iyi olur (200 OK donuyor mu diye).
 */
object NewsHelper {

    val defaultFeeds = listOf(
        "BBC Türkçe" to "https://feeds.bbci.co.uk/turkce/rss.xml"
        // Kendi guvendigin kaynaklari buraya ekleyebilirsin, ornek:
        // "Kaynak Adı" to "https://ornek-site.com/rss"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /** Senkron - Dispatchers.IO icinde cagir. */
    fun fetchAll(feeds: List<Pair<String, String>> = defaultFeeds): List<NewsItem> {
        val all = mutableListOf<NewsItem>()
        for ((sourceName, url) in feeds) {
            runCatching { fetchOne(sourceName, url) }.getOrNull()?.let { all.addAll(it) }
        }
        return all
    }

    private fun fetchOne(sourceName: String, url: String): List<NewsItem> {
        val req = Request.Builder().url(url).header("User-Agent", "ZekaTR/1.0").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            return parseRss(body, sourceName)
        }
    }

    private fun parseRss(xml: String, sourceName: String): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var inItem = false
        var title: String? = null
        var link: String? = null
        var pubDate: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> { inItem = true; title = null; link = null; pubDate = null }
                    "title" -> if (inItem) title = safeNextText(parser)
                    "link" -> if (inItem) link = safeNextText(parser)
                    "pubDate" -> if (inItem) pubDate = safeNextText(parser)
                }
                XmlPullParser.END_TAG -> if (parser.name == "item") {
                    if (!title.isNullOrBlank() && !link.isNullOrBlank()) {
                        items.add(NewsItem(title, sourceName, link, pubDate))
                    }
                    inItem = false
                }
            }
            eventType = parser.next()
        }
        return items
    }

    private fun safeNextText(parser: XmlPullParser): String? = try { parser.nextText() } catch (_: Exception) { null }
}
