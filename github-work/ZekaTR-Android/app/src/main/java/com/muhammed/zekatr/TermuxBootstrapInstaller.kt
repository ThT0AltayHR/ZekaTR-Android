package com.muhammed.zekatr

import android.content.Context
import android.os.Build
import android.system.Os
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * bootstrap-aarch64-data.zip + jniLibs/arm64-v8a/lib_zt_*.so dosyalarini
 * uygulamanin ozel dizinine kurar. PREFIX = filesDir/usr
 *
 * =====================================================================
 * ONEMLI - "Permission denied (error=13)" HATASININ GERCEK NEDENI VE COZUMU
 * =====================================================================
 * Android 10 (API 29) ve sonrasinda, targetSdk 29+ olan uygulamalarin
 * KENDI YAZDIGI (extract ettigi) dosyalar filesDir/cacheDir gibi private
 * data dizinlerinde CALISTIRILAMAZ (W^X guvenlik politikasi: bir dosya
 * ayni anda hem yazilabilir hem calistirilabilir olamaz). setExecutable(true)
 * cagrisi basarili gorunse bile, exec() aninda SELinux/kernel bunu engeller
 * ve "error=13 Permission denied" hatasi alinir. Bu, eski Termux'un da
 * Play Store'da yasadigi ve resmi Termux projesinin de cozdugu bilinen bir
 * platform kisitlamasidir.
 *
 * TEK GUVENILIR COZUM: calistirilabilir dosyalari APK'nin nativeLibraryDir'ine
 * ("jniLibs/<abi>/*.so" olarak paketlenip kurulum aninda PackageManager
 * tarafindan cikartilan, ozel olarak exec'e izin verilen tek dizin) koymak.
 * Bu yuzden derleme oncesi bir hazirlik adiminda (repack_bootstrap.py)
 * bootstrap icindeki TUM calistirilabilir dosyalar (bin/, libexec/, *.so)
 * "app/src/main/jniLibs/arm64-v8a/lib_zt_....so" olarak yeniden adlandirilip
 * tasindi; bu dosya bootstrap_exec_manifest.json ile orijinal yollarina
 * (orn. "bin/bash") eslendi. Bu sinif, PREFIX altinda o orijinal yol icin
 * dogrudan context.applicationInfo.nativeLibraryDir icindeki gercek dosyaya
 * isaret eden bir SEMBOLIK LINK olusturur - kopyalama yapmaz, calisir cunku
 * hedef nativeLibraryDir'de gercekten "executable" olarak isaretlenmis olur.
 *
 * Geri kalan (calistirilmayan, sadece okunan config/veri) dosyalar hala
 * normal sekilde bootstrap-aarch64-data.zip'ten filesDir/usr altina
 * cikartilir (bunlarin calistirilabilir olmasi gerekmedigi icin sorun yok).
 */
class TermuxBootstrapInstaller(private val context: Context) {

    val prefixDir: File = File(context.filesDir, "usr")
    private val markerFile = File(context.filesDir, ".bootstrap_installed_v2")

    /** Gercek binary/.so dosyalarinin bulundugu, APK kurulumunda PackageManager
     *  tarafindan cikartilan ve exec'e izinli TEK dizin. */
    private val nativeLibDir: File get() = File(context.applicationInfo.nativeLibraryDir)

    val supportedAbi: Boolean
        get() = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }

    fun isInstalled(): Boolean = markerFile.exists() && prefixDir.exists()

    private data class ExecEntry(val relPath: String, val soName: String)

    private fun loadExecManifest(): List<ExecEntry> {
        return try {
            context.assets.open("bootstrap_exec_manifest.json").use { stream ->
                val text = stream.bufferedReader(Charsets.UTF_8).readText()
                val arr = JSONArray(text)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    ExecEntry(obj.getString("path"), obj.getString("so"))
                }
            }
        } catch (e: Exception) {
            Log.e("ZekaTR-Bootstrap", "exec manifest okunamadi: ${e.message}")
            emptyList()
        }
    }

    /** API 24+ uyumlu symlink olusturma (java.nio.file.Files API 26 gerektirir, bu yuzden Os.symlink kullanilir). */
    private fun createSymlink(linkFile: File, targetPath: String) {
        linkFile.parentFile?.mkdirs()
        if (linkFile.exists() || java.nio.file.Files.isSymbolicLink(linkFile.toPath())) linkFile.delete()
        try {
            Os.symlink(targetPath, linkFile.absolutePath)
        } catch (e: Exception) {
            Log.w("ZekaTR-Bootstrap", "symlink basarisiz (${linkFile.name} -> $targetPath): ${e.message}")
        }
    }

    /**
     * bootstrapZip: assets/bootstrap-aarch64-data.zip (sadece veri dosyalari,
     * calistirilabilirler icinde YOK - onlar jniLibs'ten geliyor).
     * Bu fonksiyon IO thread'inde cagrilmalidir (Dispatchers.IO).
     */
    fun install(onProgress: (String) -> Unit = {}): Result<Unit> {
        if (isInstalled()) return Result.success(Unit)
        if (!supportedAbi) {
            return Result.failure(IllegalStateException("Bu cihaz arm64-v8a desteklemiyor, bootstrap kurulamaz."))
        }
        if (!nativeLibDir.exists()) {
            return Result.failure(IllegalStateException(
                "nativeLibraryDir bulunamadi (${nativeLibDir.absolutePath}). " +
                "APK'nin 'app/build.gradle' icindeki packagingOptions.jniLibs.useLegacyPackaging=true " +
                "ile derlendiginden emin ol, aksi halde terminal calistirilamaz."
            ))
        }

        return try {
            prefixDir.mkdirs()
            val execManifest = loadExecManifest()
            val execByRelPath = execManifest.associateBy { it.relPath.trimEnd('/') }
            val dataSymlinks = mutableListOf<Pair<String, String>>() // target(raw) to linkPathRaw, from SYMLINKS.txt

            // 1) Veri dosyalarini normal sekilde cikart (calistirilabilirler zip'te YOK artik)
            context.assets.open("bootstrap-aarch64-data.zip").use { assetStream ->
                ZipInputStream(assetStream.buffered()).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    var count = 0
                    while (entry != null) {
                        val name = entry.name
                        if (name == "SYMLINKS.txt") {
                            val text = zis.bufferedReader(Charsets.UTF_8).readText()
                            text.lineSequence().filter { it.isNotBlank() }.forEach { line ->
                                val parts = line.split("←")
                                if (parts.size == 2) dataSymlinks.add(parts[0] to parts[1])
                            }
                        } else if (!entry.isDirectory) {
                            val outFile = File(prefixDir, name)
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out -> zis.copyTo(out) }
                        }
                        count++
                        if (count % 200 == 0) onProgress("Kuruluyor... ($count dosya)")
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // 2) Calistirilabilir dosyalari nativeLibraryDir'e isaret eden symlink olarak olustur.
            //    Bu, W^X kisitlamasini atlatan TEK guvenilir yol.
            execManifest.forEach { entry ->
                val linkFile = File(prefixDir, entry.relPath)
                val target = File(nativeLibDir, entry.soName)
                createSymlink(linkFile, target.absolutePath)
            }
            onProgress("Calistirilabilir dosyalar baglandi (${execManifest.size})")

            // 3) SYMLINKS.txt'teki symlink'leri olustur. Eger bir symlink'in hedefi
            //    (relatif olarak PREFIX'e gore normalize edildiginde) tasinan bir
            //    exec dosyaya denk geliyorsa, dogrudan nativeLibraryDir'e yonlendir.
            dataSymlinks.forEach { (target, linkPathRaw) ->
                try {
                    val linkFile = File(prefixDir, linkPathRaw)
                    val normalizedTarget = normalizeRelTarget(linkPathRaw, target)
                    val relocated = execByRelPath[normalizedTarget]
                    if (relocated != null) {
                        createSymlink(linkFile, File(nativeLibDir, relocated.soName).absolutePath)
                    } else {
                        createSymlink(linkFile, target)
                    }
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

    /** "bin/sh" linkinden "../bin/dash" gibi bir target'i PREFIX'e gore normalize eder ("bin/dash"). */
    private fun normalizeRelTarget(linkPathRaw: String, targetRaw: String): String {
        if (targetRaw.startsWith("/")) return targetRaw.trimStart('/')
        val linkDir = File(linkPathRaw).parent ?: ""
        val combined = if (linkDir.isEmpty()) targetRaw else "$linkDir/$targetRaw"
        val parts = combined.split("/")
        val stack = ArrayDeque<String>()
        for (p in parts) {
            when (p) {
                "", "." -> {}
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(p)
            }
        }
        return stack.joinToString("/")
    }
}
