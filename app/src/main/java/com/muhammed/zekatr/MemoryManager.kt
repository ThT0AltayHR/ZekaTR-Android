package com.muhammed.zekatr

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class MemoryItem(
    val id: String,
    val text: String,
    val source: String, // "auto" (sohbetten cikarim) | "manual" (kullanici istedi)
    val createdAt: Long
)

/**
 * Bellek yonetim ekraninin veri katmani.
 * - Otomatik: sohbet sirasinda ogrenilen kullanici bilgileri (LearnedData ile birlikte calisir)
 * - Manuel: kullanici "bunu belleğe kaydet" dedigi anda eklenir
 * Ayri ayri veya hepsi birden silinebilir (UI: bellek yonetimi ekranindaki
 * "Sil" (tekil) ve "Tümünü Temizle" butonlari bu API'yi cagirir).
 */
class MemoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("zekatr_memory", Context.MODE_PRIVATE)
    private val learnedData = LearnedData(context)
    private val KEY = "memory_items"

    /** Kullanici "şunu belleğe kaydet / bunu unutma" dediğinde çağrılır. */
    fun rememberManual(text: String): MemoryItem {
        val item = MemoryItem(
            id = "m_${System.currentTimeMillis()}",
            text = text.trim(),
            source = "manual",
            createdAt = System.currentTimeMillis()
        )
        saveItem(item)
        return item
    }

    /** Sohbet sirasinda taninan bir kullanici bilgisi (ad, tercih, vb.) icin. */
    fun rememberAuto(text: String) {
        val item = MemoryItem(
            id = "m_${System.currentTimeMillis()}_a",
            text = text.trim(),
            source = "auto",
            createdAt = System.currentTimeMillis()
        )
        saveItem(item)
    }

    fun listAll(): List<MemoryItem> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val arr = JSONArray(raw)
        val list = mutableListOf<MemoryItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(MemoryItem(o.getString("id"), o.getString("text"), o.getString("source"), o.getLong("createdAt")))
        }
        return list.sortedByDescending { it.createdAt }
    }

    fun delete(id: String) {
        val remaining = listAll().filterNot { it.id == id }
        persist(remaining)
    }

    fun clearAll() {
        prefs.edit().remove(KEY).apply()
        learnedData.clearAll()
    }

    /**
     * Kullanici mesajinin "belleğe kaydet" / "bunu hatırla" / "unutma" gibi
     * manuel bir bellek komutu olup olmadigini tespit eder.
     * true donerse cagiran taraf (Brain.kt) rememberManual() ile kaydetmelidir.
     */
    fun isManualSaveCommand(userMessage: String): Boolean {
        val m = userMessage.lowercase()
        val triggers = listOf("belleğe kaydet", "bunu hatırla", "bunu unutma", "aklında tut", "hafızana kaydet")
        return triggers.any { m.contains(it) }
    }

    private fun saveItem(item: MemoryItem) {
        persist(listAll() + item)
    }

    private fun persist(items: List<MemoryItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            val o = JSONObject()
            o.put("id", item.id)
            o.put("text", item.text)
            o.put("source", item.source)
            o.put("createdAt", item.createdAt)
            arr.put(o)
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }
}
