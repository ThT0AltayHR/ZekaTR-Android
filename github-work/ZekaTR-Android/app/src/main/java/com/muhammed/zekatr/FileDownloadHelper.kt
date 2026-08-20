package com.muhammed.zekatr

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * Sohbette uretilen bir kod/dosyayi cihazin genel "Downloads" (Indirilenler)
 * klasorune kaydeder. Kullanicinin dosya yoneticisinden veya bildirimden
 * dogrudan erisebilecegi gercek bir dosya olusturur.
 */
object FileDownloadHelper {

    fun saveToDownloads(context: Context, fileName: String, content: String): Uri? {
        val safeName = if (fileName.isBlank()) "zekatr_dosya.txt" else fileName
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ZekaTR")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ZekaTR")
                dir.mkdirs()
                val file = File(dir, safeName)
                FileOutputStream(file).use { it.write(content.toByteArray()) }
                Uri.fromFile(file)
            }
        } catch (_: Exception) {
            null
        }
    }
}
