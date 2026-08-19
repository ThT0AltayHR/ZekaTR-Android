package com.muhammed.zekatr

import java.io.File

data class SelfTestReport(
    val attempts: Int,
    val passed: Int,
    val allPassed: Boolean,
    val logs: List<String>
)

/**
 * Kod uretildikten sonra "calistigini soyle degil, calistigini kanitla" prensibi.
 * runCommand ornegin "python3 main.py" veya "python3 -m pytest" olabilir.
 * Varsayilan olarak 4 kez calistirip hepsi basarili (exit code 0, stderr'de
 * "Traceback"/"Error" gecmiyor) mi diye bakar.
 */
class SelfTestRunner(private val terminal: TerminalService) {

    fun verify(projectDir: File, runCommand: String, attempts: Int = 4): SelfTestReport {
        val logs = mutableListOf<String>()
        var passed = 0
        repeat(attempts) { i ->
            val result = terminal.exec("cd '${projectDir.absolutePath}' && $runCommand", workDir = projectDir)
            val looksBroken = result.stderr.contains("Traceback", ignoreCase = true) ||
                result.stderr.contains("SyntaxError") ||
                result.stderr.contains("ModuleNotFoundError")
            val ok = result.success && !looksBroken
            if (ok) passed++
            logs.add("Deneme ${i + 1}/$attempts -> ${if (ok) "BASARILI" else "HATA"} (exit=${result.exitCode})" +
                if (!ok) "\n${result.stderr.take(500)}" else "")
        }
        return SelfTestReport(attempts, passed, passed == attempts, logs)
    }

    /** Kullanicidan onceki bir mesajla dogrudan gosterilebilecek kisa ozet. */
    fun summarize(report: SelfTestReport): String {
        return if (report.allPassed) {
            "✅ ${report.attempts} denemenin ${report.passed}'i basarili — kod gercekten calisiyor."
        } else {
            "⚠️ ${report.attempts} denemeden sadece ${report.passed} tanesi basarili. Kodda duzeltme gerekiyor, otomatik olarak ${"yeniden deneniyor"}."
        }
    }
}
