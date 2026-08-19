package com.muhammed.zekatr

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Provider based image engine. It deliberately does not pretend that the APK contains a 4K diffusion model.
 * Generation is delegated to a configured image provider and the app handles prompt, edit, download and caching.
 */
class ImageGenerationEngine(private val context: Context) {
    enum class Provider(val label: String) { OPENAI("OpenAI Images"), GEMINI_IMAGEN("Gemini Imagen") }
    data class Result(val bytes: ByteArray, val mime: String, val provider: Provider, val model: String)

    private val secure = SecurePrefs(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS).writeTimeout(90, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS).callTimeout(240, TimeUnit.SECONDS).build()

    suspend fun generate(prompt: String, provider: Provider, model: String, size: String, quality: String): Result = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank()) { "Görsel açıklaması boş olamaz." }
        when (provider) {
            Provider.OPENAI -> openAiGenerate(prompt, model, size, quality)
            Provider.GEMINI_IMAGEN -> geminiGenerate(prompt, model, size)
        }
    }

    suspend fun edit(uri: Uri, prompt: String, model: String, size: String): Result = withContext(Dispatchers.IO) {
        val key = secure.get("image_api_key") ?: secure.get("model_api_key")
            ?: error("Görsel üretimi için API anahtarı ayarlanmamış.")
        val input = File.createTempFile("zekatr_input_", ".png", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { it.copyTo(input.outputStream()) }
            ?: error("Seçilen görsel okunamadı.")
        try {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("model", model)
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("size", size)
                .addFormDataPart("image", input.name, input.asRequestBody("image/png".toMediaType()))
                .build()
            val request = Request.Builder().url("https://api.openai.com/v1/images/edits")
                .header("Authorization", "Bearer $key").post(body).build()
            parseOpenAi(request)
        } finally { input.delete() }
    }

    private fun openAiGenerate(prompt: String, model: String, size: String, quality: String): Result {
        val key = secure.get("image_api_key") ?: secure.get("model_api_key")
            ?: error("Görsel üretimi için API anahtarı ayarlanmamış.")
        val body = JSONObject().put("model", model).put("prompt", prompt).put("size", size).put("quality", quality).put("n", 1)
        val request = Request.Builder().url("https://api.openai.com/v1/images/generations")
            .header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        return parseOpenAi(request)
    }

    private fun parseOpenAi(request: Request): Result {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw friendlyHttp(response.code, raw)
            val json = JSONObject(raw)
            val item = json.optJSONArray("data")?.optJSONObject(0) ?: error("Görsel sağlayıcısı boş sonuç döndürdü.")
            val b64 = item.optString("b64_json")
            if (b64.isNotBlank()) return Result(Base64.decode(b64, Base64.DEFAULT), "image/png", Provider.OPENAI, json.optString("model", "image"))
            val url = item.optString("url")
            if (url.isBlank()) error("Görsel verisi bulunamadı.")
            val download = client.newCall(Request.Builder().url(url).get().build()).execute()
            download.use { if (!it.isSuccessful) throw friendlyHttp(it.code, "Görsel indirilemedi")
                return Result(it.body?.bytes() ?: error("Görsel boş."), it.body?.contentType()?.toString() ?: "image/png", Provider.OPENAI, json.optString("model", "image")) }
        }
    }

    private fun geminiGenerate(prompt: String, model: String, size: String): Result {
        val key = secure.get("image_api_key") ?: secure.get("model_api_key")
            ?: error("Gemini görsel API anahtarı ayarlanmamış.")
        val aspect = when (size) { "1792x1024" -> "16:9"; "1024x1792" -> "9:16"; else -> "1:1" }
        val body = JSONObject().put("instances", org.json.JSONArray().put(JSONObject().put("prompt", prompt)))
            .put("parameters", JSONObject().put("sampleCount", 1).put("aspectRatio", aspect))
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:predict?key=$key"
        val request = Request.Builder().url(url).post(body.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw friendlyHttp(response.code, raw)
            val pred = JSONObject(raw).optJSONArray("predictions")?.optJSONObject(0) ?: error("Gemini görsel sonucu boş.")
            val b64 = pred.optString("bytesBase64Encoded")
            if (b64.isBlank()) error("Gemini görsel verisi bulunamadı.")
            return Result(Base64.decode(b64, Base64.DEFAULT), "image/png", Provider.GEMINI_IMAGEN, model)
        }
    }

    private fun friendlyHttp(code: Int, raw: String): IllegalStateException {
        val msg = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
        return IllegalStateException(when (code) {
            400 -> "İstek sağlayıcı tarafından kabul edilmedi. Model, boyut veya prompt ayarlarını kontrol et."
            401, 403 -> "Görsel API anahtarı geçersiz veya bu model için yetkin yok."
            429 -> "Görsel üretim kotası doldu. Biraz sonra tekrar deneyebilir."
            500, 502, 503 -> "Görsel sağlayıcısı geçici olarak yanıt vermiyor. Tekrar deneyebilir."
            else -> "Görsel servisi HTTP $code döndürdü.${if (msg.isNotBlank()) " $msg" else ""}"
        })
    }
}
