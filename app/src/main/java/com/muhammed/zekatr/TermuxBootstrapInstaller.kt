package com.muhammed.zekatr

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * bootstrap-aarch64.zip dosyasini uygulamanin ozel dizinine kurar.
 * PREFIX = filesDir/usr  (Termux'un kendi kurulum semasiyla ayni mantik)
 *
 * Bootstrap ZIP formati standarttir:
 *   - Normal dosyalar oldugu gibi PREFIX altina cikartilir
 *   - "SYMLINKS.txt" adinda ozel bir kayit dosyasi bulunur; her satir
 *     "hedef←baglanti" seklindedir (aralarinda \u2192 degil gercek sembolik
 *     link olusturmak icin "←" karakteri kullanilir). Bu dosya normal
 *     extract edilmez, ayrica islenip gercek symlink'lere donusturulur.
 *
 * NOT: Bu temel bootstrap paketinde sadece coreutils/bash/dpkg gibi
 * cekirdek arac seti bulunur. python / git gibi paketler bootstrap'ta
 * YOKTUR — Termux'ta oldugu gibi kurulumdan sonra paket yoneticisiyle
 * ("apt"/"pkg", bu bootstrap icinde dpkg+apt hazir gelir) ayrica
 * kurulmalari gerekir. TerminalService.ensureDevTools() bunu yapar.
 */
class TermuxBootstrapInstaller(private val context: Context) {

    val prefixDir: File = File(context.filesDir, "usr")
    private val markerFile = File(context.filesDir, ".bootstrap_installed_v1")

    val supportedAbi: Boolean
        get() = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }

    fun isInstalled(): Boolean = markerFile.exists() && prefixDir.exists()

    /**
     * bootstrapZip: assets/bootstrap-aarch64.zip olarak APK'ya gomulu olmali
     * (dosya ~90MB, app/src/main/assets/bootstrap-aarch64.zip yoluna kopyalanmali).
     * Bu fonksiyon IO thread'inde cagrilmalidir (Dispatchers.IO).
     */
    fun install(onProgress: (String) -> Unit = {}): Result<Unit> {
        if (isInstalled()) return Result.success(Unit)
        if (!supportedAbi) {
            return Result.failure(IllegalStateException("Bu cihaz arm64-v8a desteklemiyor, bootstrap kurulamaz."))
        }

        return try {
            prefixDir.mkdirs()
            val symlinks = mutableListOf<Pair<String, String>>()

            context.assets.open("bootstrap-aarch64.zip").use { assetStream ->
                ZipInputStream(assetStream.buffered()).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    var count = 0
                    while (entry != null) {
                        val name = entry.name
                        if (name == "SYMLINKS.txt") {
                            val text = zis.bufferedReader(Charsets.UTF_8).readText()
                            text.lineSequence().filter { it.isNotBlank() }.forEach { line ->
                                val parts = line.split("←")
                                if (parts.size == 2) symlinks.add(parts[0] to parts[1])
                            }
                        } else if (!entry.isDirectory) {
                            val outFile = File(prefixDir, name)
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out -> zis.copyTo(out) }
                            // bin/ ve libexec/ altindaki dosyalar calistirilabilir olmali
                            if (name.startsWith("bin/") || name.startsWith("libexec/") || name.contains("/bin/")) {
                                outFile.setExecutable(true, false)
                            }
                        }
                        count++
                        if (count % 200 == 0) onProgress("Kuruluyor... ($count dosya)")
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // Simdi gercek symlink'leri olustur
            symlinks.forEach { (target, linkPathRaw) ->
                try {
                    val linkFile = File(prefixDir, linkPathRaw)
                    linkFile.parentFile?.mkdirs()
                    if (linkFile.exists()) linkFile.delete()
                    java.nio.file.Files.createSymbolicLink(linkFile.toPath(), File(target).toPath())
                } catch (_: Exception) {
                    // Bazi API seviyelerinde/FS'lerde symlink basarisiz olabilir; sessizce atla,
                    // TerminalService PATH cozumlemesinde hedefe dogrudan da bakar.
                }
            }

            markerFile.writeText(System.currentTimeMillis().toString())
            onProgress("Terminal hazir.")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
