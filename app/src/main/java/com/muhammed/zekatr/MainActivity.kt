package com.muhammed.zekatr

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.muhammed.zekatr.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var brain: Brain
    private lateinit var prefs: Prefs
    private lateinit var historyStore: ChatHistoryStore
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newCachedThreadPool()
    private var sessionId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        brain = Brain(this)
        prefs = Prefs(this)
        historyStore = ChatHistoryStore(this)
        sessionId = prefs.activeSessionId

        adapter = ChatAdapter(messages)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerMessages.adapter = adapter

        setupDrawer()
        loadSessionOrGreet()

        binding.btnSend.setOnClickListener { onSendClicked() }
    }

    private fun setupDrawer() {
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.btnNewChat.setOnClickListener { startNewChat() }

        findViewById<android.view.View>(R.id.rowNewChat)?.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startNewChat()
        }
        findViewById<android.view.View>(R.id.rowHistory)?.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, ChatHistoryActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowSettings)?.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<android.widget.TextView>(R.id.textDrawerName)?.text =
            prefs.userName?.takeIf { it.isNotBlank() } ?: "Misafir"
        findViewById<android.widget.TextView>(R.id.textDrawerAvatar)?.text =
            (prefs.userName?.trim()?.firstOrNull()?.uppercaseChar() ?: 'Z').toString()
    }

    private fun startNewChat() {
        persistCurrentSession()
        sessionId = java.util.UUID.randomUUID().toString()
        prefs.activeSessionId = sessionId
        messages.clear()
        adapter.notifyDataSetChanged()
        greet()
    }

    private fun loadSessionOrGreet() {
        val loaded = historyStore.load(sessionId)
        if (loaded.isNotEmpty()) {
            messages.clear()
            messages.addAll(loaded)
            adapter.notifyDataSetChanged()
            scrollToBottom()
        } else {
            greet()
        }
    }

    private fun greet() {
        val name = prefs.userName?.takeIf { it.isNotBlank() }
        val greeting = if (name != null) {
            "Merhaba $name! Ben ZekaTR, Muhammed tarafından geliştirildim. Temel sohbet motorum cihazının içinde çalışır. " +
                    "Bana bilmediğim bir şey sorarsan öğretebilirsin; Ayarlar'dan web araması açıksa internetten de araştırabilirim."
        } else {
            "Merhaba! Ben ZekaTR, Muhammed tarafından geliştirildim. Bana bilmediğin bir şey sorarsan öğretebilirsin; " +
                    "Ayarlar'dan web araması açıksa internetten de araştırabilirim."
        }
        addAiMessage(greeting, null, null, animate = false)
    }

    private fun onSendClicked() {
        val text = binding.editMessage.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return

        addUserMessage(text)
        binding.editMessage.setText("")
        showThinking()

        val thinkDelay = when (prefs.thinkingLevel) {
            Prefs.ThinkingLevel.FAST -> 150L
            Prefs.ThinkingLevel.NORMAL -> 550L
            Prefs.ThinkingLevel.DEEP -> 900L
        }

        handler.postDelayed({
            val answer = brain.process(text)
            adapter.removeLastIfThinking()

            if (answer.source == Brain.AnswerSource.NEEDS_WEB_SEARCH && answer.searchQuery != null) {
                handleWebSearch(answer.searchQuery)
                return@postDelayed
            }

            deliverAnswer(answer.text, answer.fileName, answer.code, text)
        }, thinkDelay)
    }

    /** Kullanici Ayarlar'dan web aramasini actiysa, bilinmeyen sorular icin GERCEK arama yapar. */
    private fun handleWebSearch(query: String) {
        showThinking("İnternette araştırıyorum…")
        ioExecutor.execute {
            val result = WebSearchHelper.search(query)
            handler.post {
                adapter.removeLastIfThinking()
                if (result != null) {
                    val text = result.summary + if (result.sourceUrl != null) "\n\nKaynak: ${result.sourceUrl}" else ""
                    brain.rememberWebResult(query, result.summary)
                    deliverAnswer(text, null, null, query, linkUrl = result.sourceUrl)
                } else {
                    deliverAnswer(
                        "İnternette bu konuda net bir sonuç bulamadım. Bana doğru cevabı öğretebilir misin?",
                        null, null, query
                    )
                }
            }
        }
    }

    private fun deliverAnswer(text: String, fileName: String?, code: String?, originalUserText: String, linkUrl: String? = null) {
        val msg = ChatMessage(ChatMessage.Type.AI, text, fileName, code, animate = true)
        adapter.addMessage(msg)
        scrollToBottom()

        if (fileName != null && code != null) {
            handler.postDelayed({
                msg.code = code
                adapter.notifyItemChanged(messages.size - 1)
                scrollToBottom()
            }, 400)
        }

        // Tek bir URL paylasilan/donen cevaplar icin gercek onizleme karti getir (opt-in web erisimi gerektirir)
        val urlToPreview = linkUrl ?: extractSingleUrl(originalUserText)
        if (urlToPreview != null && prefs.webSearchEnabled) {
            ioExecutor.execute {
                if (YouTubePreviewHelper.isYouTubeUrl(urlToPreview)) {
                    val yt = YouTubePreviewHelper.fetchPreview(urlToPreview)
                    if (yt != null) handler.post {
                        msg.youtubePreview = yt
                        adapter.notifyItemChanged(messages.indexOf(msg))
                    }
                } else {
                    val lp = LinkPreviewHelper.fetchPreview(urlToPreview)
                    if (lp != null) handler.post {
                        msg.linkPreview = lp
                        adapter.notifyItemChanged(messages.indexOf(msg))
                    }
                }
            }
        }

        persistCurrentSession()
    }

    private fun extractSingleUrl(text: String): String? {
        val regex = Regex("https?://\\S+")
        val matches = regex.findAll(text).map { it.value }.toList()
        return if (matches.size == 1) matches[0] else null
    }

    private fun addUserMessage(text: String) {
        adapter.addMessage(ChatMessage(ChatMessage.Type.USER, text))
        scrollToBottom()
        persistCurrentSession()
    }

    private fun addAiMessage(text: String, fileName: String?, code: String?, animate: Boolean = true) {
        adapter.addMessage(ChatMessage(ChatMessage.Type.AI, text, fileName, code, animate = animate))
        scrollToBottom()
        persistCurrentSession()
    }

    private fun showThinking(label: String? = null) {
        val msg = ChatMessage(ChatMessage.Type.THINKING)
        if (label != null) msg.text = label
        adapter.addMessage(msg)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        binding.recyclerMessages.post {
            if (messages.isNotEmpty()) binding.recyclerMessages.scrollToPosition(messages.size - 1)
        }
    }

    private fun persistCurrentSession() {
        if (messages.any { it.type == ChatMessage.Type.USER }) {
            historyStore.save(sessionId, messages)
        }
    }

    override fun onPause() {
        super.onPause()
        persistCurrentSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
