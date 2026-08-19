package com.muhammed.zekatr

import android.content.Context
import java.io.File

/**
 * GGUF model dosyasi APK icine gomulemeyecek kadar buyuk oldugu icin
 * (Play Store/derleme limitleri), kullanici tarafindan cihaza MANUEL
 * kopyalanacagi sabit klasor.
 *
 * Kurulum: cihazda şu yola indirilen .gguf dosyasi kopyalanmali:
 *   /Android/data/com.muhammed.zekatr/files/ggufmodel/
 * (Bu, getExternalFilesDir ile uygulama kaldirilinca otomatik temizlenen,
 * ama kullanicinin dosya yoneticisinden erisebildigi bir klasordur.)
 */
object ModelPaths {

    fun ggufDir(context: Context): File {
        val dir = context.getExternalFilesDir("ggufmodel") ?: File(context.filesDir, "ggufmodel")
        dir.mkdirs()
        return dir
    }

    /** Klasordeki ilk .gguf dosyasini bulur (yoksa null). ModelRouter bunu kullanir. */
    fun findGgufModel(context: Context): File? {
        val active = activeModelFileName(context)
        if (active != null) {
            val f = File(ggufDir(context), active)
            if (f.exists()) return f
        }
        return listGgufModels(context).maxByOrNull { it.length() }
    }

    /** Klasordeki TUM .gguf dosyalarini listeler (birden fazla Qwen varyanti desteklenir). */
    fun listGgufModels(context: Context): List<File> {
        return ggufDir(context).listFiles { f -> f.isFile && f.name.endsWith(".gguf", ignoreCase = true) }
            ?.sortedBy { it.length() } // kucukten buyuge: en hafif model listenin basinda
            ?: emptyList()
    }

    /** Kullanicinin Ayarlar'dan sectigi aktif model dosya adi. */
    fun activeModelFileName(context: Context): String? =
        context.getSharedPreferences("zekatr_prefs", Context.MODE_PRIVATE).getString("active_gguf_model", null)

    fun setActiveModel(context: Context, fileName: String) {
        context.getSharedPreferences("zekatr_prefs", Context.MODE_PRIVATE).edit().putString("active_gguf_model", fileName).apply()
    }

    /** Kaba bir tahmin: dosya boyutuna gore cihazda calisabilirligi isaretler (kesin degil, sadece yol gosterici). */
    fun sizeCategoryLabel(file: File): String {
        val gb = file.length() / (1024.0 * 1024.0 * 1024.0)
        return when {
            gb < 1.5 -> "🟢 Hafif (~${"%.1f".format(gb)} GB) — çoğu telefonda akıcı"
            gb < 4.0 -> "🟡 Orta (~${"%.1f".format(gb)} GB) — 6GB+ RAM önerilir"
            else -> "🔴 Ağır (~${"%.1f".format(gb)} GB) — güçlü cihaz/uzun yanıt süresi"
        }
    }

    fun hasModel(context: Context): Boolean = listGgufModels(context).isNotEmpty()
}
