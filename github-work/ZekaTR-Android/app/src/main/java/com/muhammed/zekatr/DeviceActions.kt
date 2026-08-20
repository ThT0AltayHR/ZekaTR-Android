package com.muhammed.zekatr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.net.URLEncoder

object DeviceActions {
    data class Result(val handled: Boolean, val message: String = "")

    fun tryHandle(activity: Activity, input: String): Result {
        val text = input.trim()
        val lower = text.lowercase(java.util.Locale.forLanguageTag("tr-TR"))
        if (lower.startsWith("youtube'da ") || lower.startsWith("youtube da ") || lower.startsWith("youtube ")) {
            val query = lower.removePrefix("youtube'da ").removePrefix("youtube da ").removePrefix("youtube ").replace("ara", "").trim()
            if (query.isNotBlank()) {
                val url = "https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}"
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return Result(true, "YouTube'da $query için arama açıldı.")
            }
        }

        if (lower.matches(Regex("(youtube|uygulama|app) aç.*")) || lower.endsWith(" aç") || lower.endsWith("'ı aç") || lower.endsWith("'i aç") || lower.endsWith("'u aç") || lower.endsWith("'ü aç")) {
            val wanted = lower.replace(Regex("(?i)(uygulama|app|youtube)"), "").replace(Regex("(?i)['’]?[ıiuü]?\\s*aç$"), "").trim()
            openApp(activity, wanted)?.let { return Result(true, "$it açıldı.") }
        }
        if (lower.startsWith("whatsapp")) {
            val name = lower.substringAfter("whatsapp").replace("dan", "").replace("'tan", "").replace("ara", "").replace("mesaj gönder", "").trim()
            val number = findContactNumber(activity, name)
            if (number == null) return Result(true, "WhatsApp için $name adlı kişinin telefon numarasını bulamadım.")
            val clean = number.filter { it.isDigit() || it == '+' }
            val url = "https://wa.me/${clean.removePrefix("+")}" 
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            return Result(true, "WhatsApp konuşması açıldı.")
        }
        if (lower.contains("normalden ara") || lower.startsWith("ara ") || lower.contains("telefonla ara") || lower.contains("direkt ara")) {
            val name = text.replace(Regex("(?i)(normalden|telefonla|direkt)?\\s*ara"), "").trim()
            if (name.isNotBlank()) {
                val number = findContactNumber(activity, name)
                if (number == null) return Result(true, "$name kişisini rehberde bulamadım.")
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "Arama yapmak için Telefon izni gerekiyor.", Toast.LENGTH_LONG).show()
                    return Result(true, "Telefon izni gerekiyor. Android izin ekranından izin verince tekrar deneyebilirsin.")
                }
                activity.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
                return Result(true, "$name aranıyor.")
            }
        }
        if (lower.startsWith("telegram")) {
            val name = lower.substringAfter("telegram").replace("dan", "").replace("'dan", "").replace("ara", "").trim()
            val number = findContactNumber(activity, name)
            if (number != null) {
                val clean = number.filter { it.isDigit() || it == '+' }
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=${clean.removePrefix("+")}")))
                    return Result(true, "Telegram kişisi açıldı.")
                } catch (_: Exception) { }
            }
            return Result(true, "Telegram için $name kişisinin bağlantısını açamadım; Telegram yüklü ve kişi numarası görünür olmalı.")
        }
        return Result(false)
    }

    private fun openApp(context: Context, wanted: String): String? {
        if (wanted.isBlank()) return null
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(wanted)
        if (intent != null) { context.startActivity(intent); return wanted }
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val hit = apps.firstOrNull {
            val label = it.loadLabel(pm).toString().lowercase(java.util.Locale.forLanguageTag("tr-TR"))
            label.contains(wanted) || wanted.contains(label)
        } ?: return null
        val launch = pm.getLaunchIntentForPackage(hit.activityInfo.packageName) ?: return null
        context.startActivity(launch)
        return hit.loadLabel(pm).toString()
    }

    private fun findContactNumber(context: Context, name: String): String? {
        if (name.isBlank()) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$name%"), null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return null
    }
}
