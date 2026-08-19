package com.muhammed.zekatr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.muhammed.zekatr.databinding.ActivityMainBinding
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var brain: Brain
    private lateinit var prefs: Prefs
    private lateinit var historyStore: ChatHistoryStore
    private lateinit var modelRouter: ModelRouter
    private lateinit var tts: TextToSpeech
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newCachedThreadPool()
    private var sessionId = ""
    private var streamingMsg: ChatMessage? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        ioExecutor.execute {
            val analysis = runCatching { FileAnalyzer.analyze(this, uri) }.getOrElse { FileAnalyzer.Analysis("dosya", "Hata", 0, "Dosya analiz edilirken güvenli bir hata oluştu: ${it.message ?: "bilinmeyen hata"}") }
            handler.post {
                adapter.addMessage(ChatMessage(ChatMessage.Type.AI, "📎 ${analysis.name} dosyasını analiz ettim.\n\n**Steps**\n1. Dosya okundu\n2. Tür ve boyut algılandı\n3. İçerik çıkarıldı\n4. Yapı özetlendi\n\n${analysis.text}", animate = true))
                scrollToBottom(); persistCurrentSession()
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        val spoken = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (spoken.isNotBlank()) { binding.editMessage.setText(spoken); binding.editMessage.setSelection(spoken.length) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        brain = Brain(this); prefs = Prefs(this); historyStore = ChatHistoryStore(this); modelRouter = ModelRouter(this); tts = TextToSpeech(this, this)
        sessionId = prefs.activeSessionId
        adapter = ChatAdapter(messages)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerMessages.adapter = adapter
        setupDrawer(); loadSessionOrGreet(); updateModelStatus()
        binding.btnSend.setOnClickListener { onSendClicked() }
        binding.btnAttach.setOnClickListener { filePicker.launch(arrayOf("text/*", "application/pdf", "application/zip", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/octet-stream", "image/*")) }
        binding.btnVoice.setOnClickListener { startVoiceInput() }
        requestOptionalPermissions()
    }

    private fun requestOptionalPermissions() {
        val wanted = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) wanted += Manifest.permission.READ_CONTACTS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) wanted += Manifest.permission.CALL_PHONE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) wanted += Manifest.permission.ACCESS_COARSE_LOCATION
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) wanted += Manifest.permission.POST_NOTIFICATIONS
        if (wanted.isNotEmpty()) permissionLauncher.launch(wanted.toTypedArray())
    }

    private fun setupDrawer() {
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.btnNewChat.setOnClickListener { startNewChat() }
        findViewById<View>(R.id.rowNewChat)?.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.START); startNewChat() }
        findViewById<View>(R.id.rowHistory)?.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, ChatHistoryActivity::class.java)) }
        findViewById<View>(R.id.rowSettings)?.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<View>(R.id.rowTraining)?.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, TrainingActivity::class.java)) }
        findViewById<View>(R.id.rowImageLab)?.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.START); startActivity(Intent(this, ImageGenerationActivity::class.java)) }
        findViewById<android.widget.TextView>(R.id.textDrawerName)?.text = prefs.userName?.takeIf { it.isNotBlank() } ?: "Misafir"
        findViewById<android.widget.TextView>(R.id.textDrawerAvatar)?.text = (prefs.userName?.trim()?.firstOrNull()?.uppercaseChar() ?: 'Z').toString()
    }

    private fun updateModelStatus() {
        binding.textModelStatus.text = if (prefs.modelEnabled && modelRouter.configured()) "${prefs.modelProvider.label} • streaming" else "Yerel beyin • hazır"
    }

    private fun startNewChat() {
        persistCurrentSession(); sessionId = java.util.UUID.randomUUID().toString(); prefs.activeSessionId = sessionId
        messages.clear(); adapter.notifyDataSetChanged(); greet()
    }

    private fun loadSessionOrGreet() {
        val loaded = historyStore.load(sessionId)
        if (loaded.isNotEmpty()) { messages.clear(); messages.addAll(loaded); adapter.notifyDataSetChanged(); scrollToBottom() } else greet()
    }

    private fun greet() {
        val name = prefs.userName?.takeIf { it.isNotBlank() }
        addAiMessage(if (name != null) "Merhaba $name! Ben ZekaTR. Artık model yönlendirme, streaming, hafıza, dosya analizi, web araştırması ve cihaz araçları için hazırlandım." else "Merhaba! Ben ZekaTR. Model, web, hafıza, dosya analizi ve cihaz araçlarıyla çalışmaya hazırım.", null, null, false)
    }

    private fun onSendClicked() {
        val text = binding.editMessage.text?.toString()?.trim().orEmpty(); if (text.isEmpty()) return
        addUserMessage(text); binding.editMessage.setText("")
        val device = runCatching { DeviceActions.tryHandle(this, text) }.getOrNull()
        if (device?.handled == true) { showAndSpeak(device.message); return }
        showThinking(if (prefs.thinkingLevel == Prefs.ThinkingLevel.DEEP) "Kaynaklar ve bağlam değerlendiriliyor…" else null)
        handler.postDelayed({ processRequest(text) }, when (prefs.thinkingLevel) { Prefs.ThinkingLevel.FAST -> 80L; Prefs.ThinkingLevel.NORMAL -> 250L; Prefs.ThinkingLevel.DEEP -> 450L })
    }

    private fun processRequest(text: String) {
        val answer = brain.process(text); adapter.removeLastIfThinking()
        val lowerText = text.lowercase(Locale.forLanguageTag("tr-TR"))
        if ((lowerText.contains("deep research") || lowerText.contains("derin araştır")) && prefs.webSearchEnabled) { handleDeepResearch(text); return }
        if (prefs.modelEnabled && modelRouter.configured() && (answer.source == Brain.AnswerSource.LEARNING_MODE || answer.source == Brain.AnswerSource.NEEDS_WEB_SEARCH)) { handleModel(text); return }
        if (answer.source == Brain.AnswerSource.NEEDS_WEB_SEARCH && answer.searchQuery != null) {
            if (text.lowercase(Locale.forLanguageTag("tr-TR")).contains("deep research") || text.lowercase(Locale.forLanguageTag("tr-TR")).contains("derin araştır")) handleDeepResearch(answer.searchQuery) else handleWebSearch(answer.searchQuery)
            return
        }
        deliverAnswer(answer.text, answer.fileName, answer.code, text)
    }

    private fun handleModel(userText: String) {
        showThinking("${prefs.modelProvider.label} düşünüyor…")
        val history = messages.filter { it.type == ChatMessage.Type.USER || it.type == ChatMessage.Type.AI }.takeLast(12).map { ModelRouter.Message(if (it.type == ChatMessage.Type.USER) "user" else "assistant", it.text) }
        val system = ModelRouter.Message("system", "Sen ZekaTR'sin. Türkçe konuş. Kullanıcıya doğrudan yardımcı ol. Kaynakları ham biçimde yapıştırma; bulguları birleştir ve kısa, anlaşılır sonuç üret. Bilmediğini dürüstçe belirt ama web veya yerel araçla doğrulama fırsatını değerlendir. Güvenlik kurallarını aşma.")
        val all = listOf(system) + history + ModelRouter.Message("user", userText)
        modelRouter.stream(all, onDelta = { delta -> handler.post { updateStreaming(delta) } }, onDone = { full -> handler.post { finishStreaming(full) } }, onError = { error -> handler.post { finishStreaming("$error\n\nYerel ZekaTR motoruna geri döndüm. Ayarlar'dan başka bir sağlayıcı seçebilirsin.") } })
    }

    private fun updateStreaming(delta: String) {
        adapter.removeLastIfThinking()
        if (streamingMsg == null) { streamingMsg = ChatMessage(ChatMessage.Type.AI, delta); adapter.addMessage(streamingMsg!!); } else { streamingMsg!!.text += delta; adapter.notifyItemChanged(messages.lastIndex) }
        scrollToBottom()
    }

    private fun finishStreaming(full: String) {
        adapter.removeLastIfThinking()
        if (streamingMsg == null) { addAiMessage(full, null, null, true) } else { streamingMsg!!.text = full; adapter.notifyItemChanged(messages.lastIndex); persistCurrentSession() }
        streamingMsg = null
    }

    private fun handleDeepResearch(query: String) {
        showThinking("🧠 Deep Research: birden fazla kaynak taranıyor…")
        ioExecutor.execute {
            val report = DeepResearchHelper.run(query)
            handler.post {
                adapter.removeLastIfThinking()
                val text = if (report.sources.isEmpty()) "Deep Research için güvenilir sonuç alınamadı. İnternet bağlantısını kontrol et." else buildString {
                    append("🧠 Deep Research

")
                    append("${report.sources.size} kaynak tarandı. Kaynaklar karşılaştırılarak aşağıdaki bulgular toplandı:

")
                    report.sources.forEachIndexed { i, r -> append("${i + 1}. ${r.title}\n${r.summary.take(700)}\n${r.sourceUrl ?: ""}\n\n") }
                }
                deliverAnswer(text, null, null, query)
            }
        }
    }

    private fun handleWebSearch(query: String) {
        showThinking("Web araştırması yapılıyor…")
        ioExecutor.execute {
            val result = WebSearchHelper.search(query)
            handler.post {
                adapter.removeLastIfThinking()
                if (result != null) {
                    val text = "🔎 Araştırma sonucu\n\n${result.summary}\n\nKaynak: ${result.title}"
                    brain.rememberWebResult(query, result.summary)
                    deliverAnswer(text, null, null, query, result.sourceUrl)
                } else deliverAnswer("Web araştırması şu anda başarısız oldu. Ağ bağlantısını veya arama ayarını kontrol et.", null, null, query)
            }
        }
    }

    private fun deliverAnswer(text: String, fileName: String?, code: String?, original: String, linkUrl: String? = null) {
        val msg = ChatMessage(ChatMessage.Type.AI, text, fileName, code, animate = true); adapter.addMessage(msg); scrollToBottom()
        val url = linkUrl ?: Regex("https?://\\S+").findAll(original).map { it.value }.singleOrNull()
        if (url != null && prefs.webSearchEnabled) ioExecutor.execute {
            if (YouTubePreviewHelper.isYouTubeUrl(url)) YouTubePreviewHelper.fetchPreview(url)?.let { yt -> handler.post { msg.youtubePreview = yt; adapter.notifyItemChanged(messages.indexOf(msg)) } }
            else LinkPreviewHelper.fetchPreview(url)?.let { lp -> handler.post { msg.linkPreview = lp; adapter.notifyItemChanged(messages.indexOf(msg)) } }
        }
        persistCurrentSession()
    }

    private fun showAndSpeak(text: String) { addAiMessage(text, null, null, true); speak(text) }
    private fun speak(text: String) { if (::tts.isInitialized) tts.speak(text.take(3000), TextToSpeech.QUEUE_FLUSH, null, "zekatr") }
    private fun startVoiceInput() { val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR"); putExtra(RecognizerIntent.EXTRA_PROMPT, "ZekaTR'ye konuş") }; runCatching { speechLauncher.launch(intent) }.onFailure { android.widget.Toast.makeText(this, "Bu cihazda sesli giriş kullanılamıyor.", android.widget.Toast.LENGTH_SHORT).show() } }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts.language = Locale("tr", "TR") }
    private fun addUserMessage(text: String) { adapter.addMessage(ChatMessage(ChatMessage.Type.USER, text)); scrollToBottom(); persistCurrentSession() }
    private fun addAiMessage(text: String, fileName: String?, code: String?, animate: Boolean = true) { adapter.addMessage(ChatMessage(ChatMessage.Type.AI, text, fileName, code, animate)); scrollToBottom(); persistCurrentSession() }
    private fun showThinking(label: String? = null) { adapter.addMessage(ChatMessage(ChatMessage.Type.THINKING, label ?: "")); scrollToBottom() }
    private fun scrollToBottom() { binding.recyclerMessages.post { if (messages.isNotEmpty()) binding.recyclerMessages.scrollToPosition(messages.size - 1) } }
    private fun persistCurrentSession() { if (messages.any { it.type == ChatMessage.Type.USER }) historyStore.save(sessionId, messages) }
    override fun onResume() { super.onResume(); updateModelStatus(); if (prefs.locationEnabled) WeatherNewsWorker.schedule(this) }
    override fun onPause() { super.onPause(); persistCurrentSession() }
    override fun onDestroy() { super.onDestroy(); tts.stop(); tts.shutdown(); handler.removeCallbacksAndMessages(null) }
}
