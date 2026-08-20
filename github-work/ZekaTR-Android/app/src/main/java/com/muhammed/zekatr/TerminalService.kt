package com.muhammed.zekatr

import android.content.Context
import java.io.File

data class ExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val success: Boolean get() = exitCode == 0
}

/**
 * Bootstrap kurulduktan sonra gercek komut calistirma katmani.
 * python / git / pip komutlarini PREFIX (filesDir/usr) icinden calistirir.
 *
 * Guvenlik: Git islemlerinde token ASLA argv (komut satiri) icine gomulmez,
 * cunku argv `ps`/`/proc` uzerinden diger uygulamalarca gorulebilir.
 * Bunun yerine gecici bir "askpass" scripti olusturulur, token SADECE
 * o script calisirken bir kere okunur, is bitince script silinir.
 */
class TerminalService(private val context: Context) {

    private val installer = TermuxBootstrapInstaller(context)
    private val vault = SecretVault(context)
    private val prefix get() = installer.prefixDir
    private val homeDir = File(context.filesDir, "home").apply { mkdirs() }

    private fun baseEnv(): MutableMap<String, String> {
        val env = System.getenv().toMutableMap()
        env["PREFIX"] = prefix.absolutePath
        env["HOME"] = homeDir.absolutePath
        // nativeLibraryDir eklendi: bin/ ve libexec/ altindaki dosyalar artik oraya symlink
        // (bkz. TermuxBootstrapInstaller) - bagli olduklari paylasilan .so'lar da orada.
        env["LD_LIBRARY_PATH"] = "${prefix.absolutePath}/lib:${context.applicationInfo.nativeLibraryDir}"
        env["PATH"] = "${prefix.absolutePath}/bin:${System.getenv("PATH")}"
        env["TMPDIR"] = context.cacheDir.absolutePath
        return env
    }

    fun isReady(): Boolean = installer.isInstalled()

    fun install(onProgress: (String) -> Unit = {}) = installer.install(onProgress)

    /** Genel komut calistirma. cmd bir shell satiri olarak `bash -c` ile calisir. */
    fun exec(cmd: String, workDir: File = homeDir, timeoutSeconds: Long = 120): ExecResult {
        if (!isReady()) return ExecResult(-1, "", "Terminal henuz kurulmadi. Once install() cagirin.")
        return try {
            val shell = File(prefix, "bin/bash")
            val shellBin = if (shell.exists()) shell.absolutePath else "/system/bin/sh"
            val pb = ProcessBuilder(shellBin, "-c", cmd)
            pb.directory(workDir.apply { mkdirs() })
            pb.environment().putAll(baseEnv())
            pb.redirectErrorStream(false)
            val process = pb.start()
            val finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ExecResult(-9, "", "Zaman asimi ($timeoutSeconds sn) - komut sonlandirildi: $cmd")
            }
            val out = process.inputStream.bufferedReader().readText()
            val err = process.errorStream.bufferedReader().readText()
            ExecResult(process.exitValue(), out, err)
        } catch (e: Exception) {
            ExecResult(-1, "", "Calistirma hatasi: ${e.message}")
        }
    }

    /** Bootstrap sadece cekirdek araclari icerir; python/git ilk kullanimda dpkg/apt ile kurulur. */
    fun ensureDevTools(onProgress: (String) -> Unit = {}): ExecResult {
        onProgress("Gelistirme araclari kontrol ediliyor (python, git, pip)...")
        val check = exec("command -v python3 >/dev/null 2>&1 && command -v git >/dev/null 2>&1 && echo OK || echo MISSING")
        if (check.stdout.trim() == "OK") return ExecResult(0, "zaten kurulu", "")
        onProgress("python/git kuruluyor (apt) - internet gerektirir...")
        return exec(
            "apt update -y && apt install -y python git openssl ca-certificates && python3 -m ensurepip --upgrade",
            timeoutSeconds = 600
        )
    }

    /**
     * Kasadaki bir token'i git push/clone gibi islemlerde kullanir.
     * Token, disariya SADECE gecici askpass scripti araciligiyla,
     * o process'in yasam suresi kadar aktarilir; ne loglara ne UI'a yazilmaz.
     */
    fun gitPushWithSecret(secretName: String, repoDir: File, remote: String = "origin", branch: String = "main"): ExecResult {
        if (!isReady()) return ExecResult(-1, "", "Terminal hazir degil.")
        return vault.useSecret(secretName) { token ->
            val askpass = File(context.cacheDir, "askpass_${System.currentTimeMillis()}.sh")
            try {
                askpass.writeText("#!/system/bin/sh\necho \"$token\"\n")
                askpass.setExecutable(true, false)
                val env = baseEnv()
                env["GIT_ASKPASS"] = askpass.absolutePath
                env["GIT_TERMINAL_PROMPT"] = "0"
                val shell = File(prefix, "bin/bash")
                val pb = ProcessBuilder(shell.absolutePath, "-c", "git push $remote $branch")
                pb.directory(repoDir)
                pb.environment().putAll(env)
                val process = pb.start()
                val finished = process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)
                if (!finished) { process.destroyForcibly(); return@useSecret ExecResult(-9, "", "git push zaman asimi") }
                ExecResult(process.exitValue(), process.inputStream.bufferedReader().readText(), process.errorStream.bufferedReader().readText())
            } finally {
                askpass.delete()
            }
        } ?: ExecResult(-1, "", "'$secretName' adinda bir secret kasada bulunamadi.")
    }
}
