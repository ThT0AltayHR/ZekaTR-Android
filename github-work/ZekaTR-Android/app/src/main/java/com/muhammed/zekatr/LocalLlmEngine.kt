package com.muhammed.zekatr

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

/**
 * ZekaTR'nin gercekten "sohbete bağlanan" yerel GGUF motoru.
 *
 * Native (.so) llama.cpp koprusu henuz derlenmediyse (bkz.
 * app/src/main/cpp/README.md - submodule + build.gradle yorumu acilana kadar)
 * bu sinif ZORLA CALISIYORMUS GIBI YAPMAZ: acikca "native motor henuz
 * baglanmadi" mesaji dondurur ki hicbir zaman sahte/uydurma bir cevap
 * "yerel model" gibi sunulmasin. Submodule eklenip derlendigi an,
 * System.loadLibrary basarili olur ve gercek token uretimi baslar.
 *
 * ZekaTR Thinking Model kimligi: kullanici "seni kim gelistirdi" turu bir
 * soru sordugunda modelin kendi egitiminden gelen genel cevaplar yerine
 * ZekaTR/Muhammed kimligini one cikarmasi icin her uretimin basina bir
 * sistem talimati ekleniyor (bkz. SYSTEM_PERSONA).
 */
class LocalLlmEngine(private val context: Context) {

    companion object {
        val PERSONA_PROMPT = """
            Senin adin "ZekaTR Thinking Model". Bu uygulamayi ve seni Muhammed gelistirdi.
            Kim tarafindan gelistirildigin veya kim oldugun sorulursa, bunu acikca soyle.
            Turkce, samimi ve net cevap ver.
        """.trimIndent()

        private var nativeAvailable: Boolean? = null

        private fun ensureNativeLoaded(): Boolean {
            nativeAvailable?.let { return it }
            return try {
                System.loadLibrary("zekatr_llama")
                nativeAvailable = true
                true
            } catch (e: UnsatisfiedLinkError) {
                nativeAvailable = false
                false
            }
        }
    }

    interface TokenCallback {
        fun onToken(piece: String)
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var handle: Long = 0
    @Volatile var loadedModelPath: String? = null
        private set

    val nativeEngineLinked: Boolean get() = ensureNativeLoaded()

    // --- JNI ---
    private external fun nativeLoadModel(modelPath: String, nCtx: Int, nThreads: Int): Long
    private external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int, callback: TokenCallback)
    private external fun nativeFree(handle: Long)

    fun loadModel(modelFile: File, onResult: (Result<Unit>) -> Unit) {
        executor.execute {
            if (!ensureNativeLoaded()) {
                onResult(Result.failure(IllegalStateException(
                    "Yerel cikarim motoru (llama.cpp) henuz bu APK'ya baglanmamis. " +
                    "app/src/main/cpp/README.md dosyasindaki 2 adimi uygulayip yeniden derle."
                )))
                return@execute
            }
            if (!modelFile.exists()) {
                onResult(Result.failure(IllegalStateException("Model dosyasi bulunamadi: ${modelFile.absolutePath}")))
                return@execute
            }
            try {
                if (handle != 0L) { nativeFree(handle); handle = 0 }
                val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
                handle = nativeLoadModel(modelFile.absolutePath, 2048, threads)
                if (handle == 0L) {
                    onResult(Result.failure(IllegalStateException("Model yuklenemedi (bozuk/desteklenmeyen GGUF dosyasi olabilir): ${modelFile.name}")))
                } else {
                    loadedModelPath = modelFile.absolutePath
                    onResult(Result.success(Unit))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    /** history: onceki sohbet turleri (role/content). Streaming token'lar onToken ile, bitince onDone ile gelir. */
    fun generate(
        history: List<ModelRouter.Message>,
        userMessage: String,
        maxTokens: Int = 512,
        onToken: (String) -> Unit,
        onDone: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            if (handle == 0L) { onError("Once bir model yuklenmeli (Model butonu -> ZekaTR Thinking Model'e dokun)."); return@execute }
            try {
                val prompt = buildPrompt(history, userMessage)
                val full = StringBuilder()
                nativeGenerate(handle, prompt, maxTokens, object : TokenCallback {
                    override fun onToken(piece: String) {
                        full.append(piece)
                        onToken(piece)
                    }
                })
                onDone(full.toString())
            } catch (e: Exception) {
                onError(e.message ?: "Yerel model calistirilirken beklenmeyen hata olustu.")
            }
        }
    }

    private fun buildPrompt(history: List<ModelRouter.Message>, userMessage: String): String {
        // ChatML benzeri basit format - Qwen/pek cok modern GGUF modeli bunu tanir.
        val sb = StringBuilder()
        sb.append("<|im_start|>system\n").append(PERSONA_PROMPT).append("<|im_end|>\n")
        history.takeLast(12).forEach { m ->
            val role = if (m.role == "assistant") "assistant" else "user"
            sb.append("<|im_start|>").append(role).append('\n').append(m.content).append("<|im_end|>\n")
        }
        sb.append("<|im_start|>user\n").append(userMessage).append("<|im_end|>\n<|im_start|>assistant\n")
        return sb.toString()
    }

    fun release() {
        executor.execute { if (handle != 0L) { nativeFree(handle); handle = 0; loadedModelPath = null } }
    }
}
