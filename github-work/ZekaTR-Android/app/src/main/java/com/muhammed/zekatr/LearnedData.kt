package com.muhammed.zekatr

import android.content.Context
import org.json.JSONObject

/**
 * ZekaTR'nin "yerel hafizasi".
 * Kullanicidan ogrendigi yeni anahtar kelime -> cevap ciftlerini
 * cihazin SharedPreferences deposunda JSON olarak saklar.
 * Hicbir sekilde internete veya disariya veri gondermez.
 */
class LearnedData(context: Context) {

    private val prefs = context.getSharedPreferences("zekatr_beyin", Context.MODE_PRIVATE)
    private val KEY = "learned_json"

    // anahtarKelime -> cevap listesi (birden fazla ogretilen cevap olabilir)
    private val cache: MutableMap<String, MutableList<String>> = mutableMapOf()

    init {
        load()
    }

    private fun load() {
        val raw = prefs.getString(KEY, null) ?: return
        try {
            val obj = JSONObject(raw)
            for (key in obj.keys()) {
                val arr = obj.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                cache[key] = list
            }
        } catch (e: Exception) {
            // Bozuk veri varsa sessizce yok say, beyin bos baslar
        }
    }

    private fun persist() {
        val obj = JSONObject()
        for ((key, list) in cache) {
            obj.put(key, org.json.JSONArray(list))
        }
        prefs.edit().putString(KEY, obj.toString()).apply()
    }

    /** Yeni bir anahtar kelime icin cevap ogretir ve kalici olarak kaydeder. */
    fun teach(keyword: String, answer: String) {
        val k = keyword.trim().lowercase()
        if (k.isEmpty()) return
        val list = cache.getOrPut(k) { mutableListOf() }
        if (!list.contains(answer)) list.add(answer)
        persist()
    }

    /** Verilen kelimelere en cok uyan ogrenilmis cevabi dondurur (varsa). */
    fun findAnswer(words: List<String>): String? {
        for (w in words) {
            cache[w]?.let { if (it.isNotEmpty()) return it.random() }
        }
        return null
    }

    fun learnedCount(): Int = cache.size

    fun clearAll() {
        cache.clear()
        prefs.edit().remove(KEY).apply()
    }
}
