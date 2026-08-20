package com.muhammed.zekatr

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cihaz uzerinde, JSON dosyalari olarak birden fazla sohbet gecmisini saklar.
 * Her sohbet /files/chats/<id>.json altinda durur. Bulut/sunucu YOKTUR,
 * hicbir veri cihaz disina cikmaz.
 */
class ChatHistoryStore(private val context: Context) {

    data class SessionSummary(val id: String, val title: String, val updatedAt: Long)

    private val dir: File by lazy {
        File(context.filesDir, "chats").apply { mkdirs() }
    }

    private fun fileFor(id: String) = File(dir, "$id.json")

    fun save(id: String, messages: List<ChatMessage>) {
        val arr = JSONArray()
        messages.filter { it.type != ChatMessage.Type.THINKING }.forEach { msg ->
            val o = JSONObject()
            o.put("type", msg.type.name)
            o.put("text", msg.text)
            o.put("fileName", msg.fileName ?: JSONObject.NULL)
            o.put("code", msg.code ?: JSONObject.NULL)
            arr.put(o)
        }
        val root = JSONObject()
        root.put("id", id)
        root.put("title", deriveTitle(messages))
        root.put("updatedAt", System.currentTimeMillis())
        root.put("messages", arr)
        fileFor(id).writeText(root.toString())
    }

    fun load(id: String): List<ChatMessage> {
        val f = fileFor(id)
        if (!f.exists()) return emptyList()
        return try {
            val root = JSONObject(f.readText())
            val arr = root.getJSONArray("messages")
            val out = mutableListOf<ChatMessage>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    ChatMessage(
                        type = ChatMessage.Type.valueOf(o.getString("type")),
                        text = o.getString("text"),
                        fileName = if (o.isNull("fileName")) null else o.getString("fileName"),
                        code = if (o.isNull("code")) null else o.getString("code")
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun listSessions(): List<SessionSummary> {
        val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { f ->
            try {
                val root = JSONObject(f.readText())
                SessionSummary(root.getString("id"), root.optString("title", "Sohbet"), root.optLong("updatedAt", 0L))
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    fun delete(id: String) {
        fileFor(id).delete()
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun deriveTitle(messages: List<ChatMessage>): String {
        val firstUser = messages.firstOrNull { it.type == ChatMessage.Type.USER }?.text
        if (firstUser.isNullOrBlank()) {
            val fmt = SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("tr-TR"))
            return "Sohbet - ${fmt.format(Date())}"
        }
        return if (firstUser.length > 40) firstUser.take(40) + "…" else firstUser
    }
}
