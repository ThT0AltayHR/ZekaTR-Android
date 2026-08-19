package com.muhammed.zekatr

import android.content.Context
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ModelRouter(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = Prefs(appContext)
    private val secure = SecurePrefs(appContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    enum class Provider(val label: String) {
        OPENAI("OpenAI"), GROQ("Groq"), OPENROUTER("OpenRouter"), GEMINI("Gemini"), CLAUDE("Claude"), LOCAL("Yerel model")
    }

    data class Message(val role: String, val content: String)

    fun configured(): Boolean = prefs.modelProvider != Provider.LOCAL && !secure.get("model_api_key").isNullOrBlank()

    fun stream(messages: List<Message>, onDelta: (String) -> Unit, onDone: (String) -> Unit, onError: (String) -> Unit) {
        if (!configured()) { onError("Model sağlayıcısı yapılandırılmamış."); return }
        Thread {
            try {
                val provider = prefs.modelProvider
                when (provider) {
                    Provider.GEMINI -> callGemini(messages, onDone, onError)
                    Provider.CLAUDE -> callClaude(messages, onDone, onError)
                    Provider.OPENAI, Provider.GROQ, Provider.OPENROUTER -> callOpenAiCompatible(provider, messages, onDelta, onDone, onError)
                    Provider.LOCAL -> onError("Yerel model yapılandırılmamış.")
                }
            } catch (e: Exception) {
                onError(cleanError(e))
            }
        }.start()
    }

    private fun callOpenAiCompatible(provider: Provider, messages: List<Message>, onDelta: (String) -> Unit, onDone: (String) -> Unit, onError: (String) -> Unit) {
        val key = secure.get("model_api_key") ?: return onError("API anahtarı eksik.")
        val url = when (provider) {
            Provider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
            Provider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }
        val msgs = JSONArray().apply { messages.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) } }
        val body = JSONObject().put("model", prefs.modelName).put("messages", msgs).put("temperature", 0.35).put("stream", true)
        val requestBuilder = Request.Builder().url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $key")
            .header("Accept", "text/event-stream")
        if (provider == Provider.OPENROUTER) requestBuilder.header("HTTP-Referer", "https://github.com/muhammed/zekatr")
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) return onError("AI sağlayıcısı HTTP ${response.code} döndürdü. Lütfen model ve API anahtarını kontrol et.")
            val source = response.body?.source() ?: return onError("AI yanıtı boş geldi.")
            val full = StringBuilder()
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                try {
                    val delta = JSONObject(data).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content", "") ?: ""
                    if (delta.isNotEmpty()) { full.append(delta); onDelta(delta) }
                } catch (_: Exception) { }
            }
            onDone(full.toString())
        }
    }

    private fun callGemini(messages: List<Message>, onDone: (String) -> Unit, onError: (String) -> Unit) {
        val key = secure.get("model_api_key") ?: return onError("Gemini API anahtarı eksik.")
        val parts = JSONArray()
        messages.filter { it.role != "system" }.forEach { parts.put(JSONObject().put("role", if (it.role == "assistant") "model" else "user").put("parts", JSONArray().put(JSONObject().put("text", it.content)))) }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${prefs.modelName}:generateContent?key=$key"
        val body = JSONObject().put("contents", parts).put("generationConfig", JSONObject().put("temperature", 0.35))
        val request = Request.Builder().url(url).post(body.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return onError("Gemini HTTP ${response.code} döndürdü. API anahtarını ve modeli kontrol et.")
            val json = JSONObject(response.body?.string().orEmpty())
            val text = json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""
            if (text.isBlank()) onError("Gemini boş yanıt verdi.") else onDone(text)
        }
    }

    private fun callClaude(messages: List<Message>, onDone: (String) -> Unit, onError: (String) -> Unit) {
        val key = secure.get("model_api_key") ?: return onError("Claude API anahtarı eksik.")
        val arr = JSONArray().apply { messages.filter { it.role != "system" }.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) } }
        val system = messages.firstOrNull { it.role == "system" }?.content
        val body = JSONObject().put("model", prefs.modelName).put("max_tokens", 4096).put("messages", arr)
        if (!system.isNullOrBlank()) body.put("system", system)
        val request = Request.Builder().url("https://api.anthropic.com/v1/messages")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .header("x-api-key", key).header("anthropic-version", "2023-06-01").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return onError("Claude HTTP ${response.code} döndürdü. API anahtarını ve modeli kontrol et.")
            val content = JSONObject(response.body?.string().orEmpty()).optJSONArray("content")
            val text = content?.optJSONObject(0)?.optString("text", "") ?: ""
            if (text.isBlank()) onError("Claude boş yanıt verdi.") else onDone(text)
        }
    }

    private fun cleanError(e: Exception): String = when (e) {
        is IOException -> "Ağ bağlantısı kurulamadı. İnternet bağlantını kontrol et ve tekrar dene."
        else -> e.message?.take(180) ?: "Beklenmeyen bir AI hatası oluştu."
    }
}
