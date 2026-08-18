package com.muhammed.zekatr

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.muhammed.zekatr.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var brain: Brain
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        brain = Brain(this)
        adapter = ChatAdapter(messages)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerMessages.adapter = adapter

        // Karsilama mesaji
        addAiMessage(
            "Merhaba! Ben ZekaTR, Muhammed tarafından geliştirildim. Tamamen cihazının içinde çalışırım, " +
                    "internete bağlanmam. Seninle Türkçe sohbet edebilirim ve istersen sana basit kod örnekleri de yazabilirim. " +
                    "Bana bilmediğim bir şey sorarsan, öğretebilirsin!",
            null, null
        )

        binding.btnSend.setOnClickListener { onSendClicked() }
    }

    private fun onSendClicked() {
        val text = binding.editMessage.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return

        addUserMessage(text)
        binding.editMessage.setText("")
        showThinking()

        // Kullaniciya "dusunuyor" hissi vermek icin kucuk bir gecikme
        handler.postDelayed({
            val (answer, fileName, code) = brain.respond(text)
            adapter.removeLastIfThinking()

            if (fileName != null) {
                // Once "olusturuluyor..." animasyonunu goster, sonra kodu ekle
                addAiMessageWithFile(answer, fileName, code)
            } else {
                addAiMessage(answer, null, null)
            }
        }, 650)
    }

    private fun addUserMessage(text: String) {
        adapter.addMessage(ChatMessage(ChatMessage.Type.USER, text))
        scrollToBottom()
    }

    private fun addAiMessage(text: String, fileName: String?, code: String?) {
        adapter.addMessage(ChatMessage(ChatMessage.Type.AI, text, fileName, code))
        scrollToBottom()
    }

    private fun addAiMessageWithFile(text: String, fileName: String, code: String?) {
        // Ilk once dosya adi olmadan ekle (yaziliyor hissi), sonra kisa gecikmeyle kodu goster
        val msg = ChatMessage(ChatMessage.Type.AI, text, fileName, null)
        adapter.addMessage(msg)
        scrollToBottom()
        handler.postDelayed({
            msg.code = code
            adapter.notifyItemChanged(messages.size - 1)
            scrollToBottom()
        }, 500)
    }

    private fun showThinking() {
        adapter.addMessage(ChatMessage(ChatMessage.Type.THINKING))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        binding.recyclerMessages.post {
            binding.recyclerMessages.scrollToPosition(messages.size - 1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
