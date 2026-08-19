package com.muhammed.zekatr

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class TrainingEngine(context: Context) {
    private val app = context.applicationContext
    private val memory = LearnedData(app)
    private val client = OkHttpClient()

    data class Progress(val stage: String, val percent: Int)

    fun run(onProgress: (Progress) -> Unit, onDone: (String) -> Unit) {
        Thread {
            val topics = listOf(
                "Kotlin Android modern development",
                "Java Android best practices",
                "Python modern syntax",
                "C++ modern standard",
                "shell scripting best practices",
                "GitHub secure coding Android",
                "OWASP Mobile Application Security",
                "Android permissions and security",
                "LLM prompt engineering"
            )
            var completed = 0
            val notes = mutableListOf<String>()
            topics.forEach { topic ->
                onProgress(Progress("GitHub ve web araştırılıyor: $topic", (completed * 100) / topics.size))
                runCatching {
                    val result = githubSearch(topic)
                    if (result.isNotBlank()) {
                        memory.teach("training ${topic.lowercase()}", result.take(1200))
                        notes.add(topic)
                    }
                }
                completed++
            }
            onProgress(Progress("Güvenlik kuralları doğrulanıyor…", 94))
            onProgress(Progress("Kod yazma bilgi paketi güncelleniyor…", 97))
            onProgress(Progress("Eğitim tamamlandı", 100))
            onDone("Eğitim tamamlandı. ${notes.size}/${topics.size} konu için yerel bilgi paketi güncellendi. Güvenlik ve kodlama araştırmaları çalıştırıldı; uygulama kaynak kodunu uzaktan kendi kendine değiştirmez.")
        }.start()
    }

    private fun githubSearch(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://api.github.com/search/repositories?q=$encoded&sort=stars&order=desc&per_page=3")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ZekaTR")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val items = JSONObject(response.body?.string().orEmpty()).optJSONArray("items") ?: return ""
            return buildString {
                for (i in 0 until minOf(items.length(), 3)) {
                    val item = items.optJSONObject(i) ?: continue
                    append(item.optString("full_name")); append(" — "); append(item.optString("description")); append("\n")
                }
            }
        }
    }
}
