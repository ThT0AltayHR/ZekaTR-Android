package com.muhammed.zekatr

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * GGUF model dosyalarinin yonetimi - IKI kaynaktan gelirler:
 *
 *  1) DAHILI (bundled) model: derlemeden once "app/src/main/assets/ggufmodel/"
 *     klasorune atilan .gguf dosyalari (KAYNAK KODUN icinde, android/data
 *     DEGIL - bkz. assets/ggufmodel/README.txt). Uygulama ilk acildiginda bu
 *     dosyalar otomatik olarak context.filesDir/ggufmodel altina (uygulamanin
 *     kendi private alanina - android/data/<pkg>/files DEGIL, dosya
 *     yoneticisinden gorunmez ama TAM izinli ve Android surum kisitlamasi
 *     olmayan bir alan) kopyalanir ve ana ekranda "ZekaTR Thinking Model"
 *     olarak otomatik listelenir.
 *
 *  2) KULLANICI EKLEMESI: sohbet ekranindaki Model butonu -> "Model Ekle" ile
 *     Storage Access Framework (ACTION_OPEN_DOCUMENT) uzerinden cihazdaki
 *     HERHANGI bir .gguf dosyasi secilir. Dosya KOPYALANMAZ - kalici URI
 *     izni (persistable permission) alinip dogrudan o adresten okunur; bu
 *     yuzden "android/data kisitli/calismiyor" sorunu tamamen ortadan kalkar
 *     ve yeni Android surumlerinde de calismaya devam eder. llama.cpp bir
 *     dosya yolu istedigi icin, secilen dosya ilk kullanimda bir kere
 *     filesDir/ggufmodel/user/ altina (uygulamanin kendi alani) kopyalanir.
 */
object ModelPaths {

    const val BUNDLED_MODEL_LABEL = "ZekaTR Thinking Model"

    private fun prefs(context: Context) = context.getSharedPreferences("zekatr_prefs", Context.MODE_PRIVATE)

    fun bundledDir(context: Context): File =
        File(context.filesDir, "ggufmodel").apply { mkdirs() }

    fun userModelDir(context: Context): File =
        File(context.filesDir, "ggufmodel/user").apply { mkdirs() }

    /** Uygulama her acilista bir kere: assets/ggufmodel/*.gguf -> filesDir/ggufmodel/*.gguf */
    fun syncBundledModelsFromAssets(context: Context) {
        val marker = File(context.filesDir, ".bundled_models_synced_v1")
        val assetNames = try { context.assets.list("ggufmodel")?.filter { it.endsWith(".gguf", true) } ?: emptyList() }
            catch (e: Exception) { emptyList() }
        if (assetNames.isEmpty()) return
        if (marker.exists() && marker.readText() == assetNames.sorted().joinToString(",")) return

        val dir = bundledDir(context)
        assetNames.forEach { name ->
            val out = File(dir, name)
            if (!out.exists() || out.length() == 0L) {
                context.assets.open("ggufmodel/$name").use { input ->
                    FileOutputStream(out).use { output -> input.copyTo(output) }
                }
            }
        }
        marker.writeText(assetNames.sorted().joinToString(","))
    }

    data class ModelEntry(
        val displayName: String,
        val file: File?,          // dogrudan dosya yolu (bundled veya kopyalanmis user model)
        val sourceUri: Uri?,      // SAF ile eklenmisse orijinal referans (bilgi amacli)
        val isBundled: Boolean
    )

    /** Bundled + kullanicinin ekledigi TUM modelleri listeler (otomatik algilama). */
    fun listAllModels(context: Context): List<ModelEntry> {
        val result = mutableListOf<ModelEntry>()
        bundledDir(context).listFiles { f -> f.isFile && f.name.endsWith(".gguf", true) }
            ?.sortedBy { it.length() }
            ?.forEach { f -> result.add(ModelEntry(BUNDLED_MODEL_LABEL, f, null, true)) }
        userModelDir(context).listFiles { f -> f.isFile && f.name.endsWith(".gguf", true) }
            ?.sortedBy { it.name }
            ?.forEach { f -> result.add(ModelEntry(f.name.removeSuffix(".gguf"), f, null, false)) }
        return result
    }

    fun hasModel(context: Context): Boolean = listAllModels(context).isNotEmpty()

    /** Kullanicinin Ayarlar'dan / Model butonundan sectigi aktif model dosya adi. */
    fun activeModelFileName(context: Context): String? =
        prefs(context).getString("active_gguf_model", null)

    fun setActiveModel(context: Context, fileName: String) {
        prefs(context).edit().putString("active_gguf_model", fileName).apply()
    }

    fun findActiveModel(context: Context): ModelEntry? {
        val all = listAllModels(context)
        val active = activeModelFileName(context)
        return all.firstOrNull { it.file?.name == active } ?: all.firstOrNull()
    }

    /** SAF ile secilen bir .gguf URI'sini uygulamanin kendi alanina bir kere kopyalar. */
    fun importUserModel(context: Context, uri: Uri, displayName: String): File {
        val safeName = displayName.replace(Regex("[^A-Za-z0-9_.-]"), "_").let {
            if (it.endsWith(".gguf", true)) it else "$it.gguf"
        }
        val out = File(userModelDir(context), safeName)
        context.contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(out).use { output -> input?.copyTo(output) }
        }
        return out
    }

    /** Kaba bir tahmin: dosya boyutuna gore cihazda calisabilirligi isaretler (kesin degil, sadece yol gosterici). */
    fun sizeCategoryLabel(file: File): String {
        val gb = file.length() / (1024.0 * 1024.0 * 1024.0)
        return when {
            gb < 1.5 -> "\uD83D\uDFE2 Hafif (~${"%.1f".format(gb)} GB) — çoğu telefonda akıcı"
            gb < 4.0 -> "\uD83D\uDFE1 Orta (~${"%.1f".format(gb)} GB) — 6GB+ RAM önerilir"
            else -> "\uD83D\uDD34 Ağır (~${"%.1f".format(gb)} GB) — güçlü cihaz/uzun yanıt süresi"
        }
    }
}
