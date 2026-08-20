package com.muhammed.zekatr

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipFile
import kotlin.math.min

object FileAnalyzer {
    data class Analysis(val name: String, val type: String, val size: Long, val text: String)

    fun analyze(context: Context, uri: Uri): Analysis {
        val name = queryName(context, uri) ?: "dosya"
        val size = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: 0L }.getOrDefault(0L)
        val lower = name.lowercase()
        return when {
            lower.endsWith(".zip") -> analyzeZip(context, uri, name, size)
            lower.endsWith(".docx") -> analyzeDocx(context, uri, name, size)
            lower.endsWith(".txt") || lower.endsWith(".kt") || lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".cpp") || lower.endsWith(".h") || lower.endsWith(".sh") || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".md") -> analyzeText(context, uri, name, size)
            lower.endsWith(".pdf") -> Analysis(name, "PDF", size, "PDF dosyası seçildi. Bu sürüm dosyanın varlığını ve meta bilgisini aldı; metin çıkarımı için PDF motoru yapılandırılabilir.")
            else -> Analysis(name, "Dosya", size, "Dosya seçildi. Dosya adı: $name")
        }
    }

    private fun analyzeText(context: Context, uri: Uri, name: String, size: Long): Analysis {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText().take(30000)
        }.orEmpty()
        val lines = text.lines()
        val codeHints = listOf("fun ", "class ", "import ", "def ", "public static", "#!/", "<manifest", "{\"", "package ").count { text.contains(it) }
        val summary = "Dosya: $name\nBoyut: ${size / 1024} KB\nSatır: ${lines.size}\nKod/işaretçi yoğunluğu: $codeHints\n\nİçerik önizlemesi:\n${text.take(12000)}"
        return Analysis(name, "Metin/Kod", size, summary)
    }

    private fun analyzeZip(context: Context, uri: Uri, name: String, size: Long): Analysis {
        val temp = java.io.File.createTempFile("zekatr_", ".zip", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
        val entries = mutableListOf<String>()
        var total = 0L
        var codeFiles = 0
        ZipFile(temp).use { zip ->
            zip.entries().asSequence().take(1000).forEach { e ->
                entries.add(e.name)
                total += e.size.coerceAtLeast(0)
                if (e.name.matches(Regex(".*\\.(kt|java|py|cpp|h|js|ts|xml|gradle|kts|sh)$", RegexOption.IGNORE_CASE))) codeFiles++
            }
        }
        temp.delete()
        val important = entries.filter { it.endsWith("build.gradle") || it.endsWith("settings.gradle") || it.endsWith("AndroidManifest.xml") || it.contains("src/main") }.take(40)
        return Analysis(name, "ZIP/Proje", size, "ZIP içeriği tarandı.\nDosya sayısı: ${entries.size}\nToplam açılmış boyut: ${total / 1024} KB\nKod dosyaları: $codeFiles\n\nÖnemli dosyalar:\n${important.joinToString("\n")}")
    }

    private fun analyzeDocx(context: Context, uri: Uri, name: String, size: Long): Analysis {
        val temp = java.io.File.createTempFile("zekatr_", ".docx", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
        val xml = ZipFile(temp).use { zip -> zip.getInputStream(zip.getEntry("word/document.xml")).bufferedReader().readText() }
        temp.delete()
        val text = xml.replace(Regex("<[^>]+>"), " ").replace("&amp;", "&").replace(Regex("\\s+"), " ").trim()
        return Analysis(name, "DOCX", size, "DOCX metni çıkarıldı.\n\n${text.take(16000)}")
    }

    private fun queryName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}
