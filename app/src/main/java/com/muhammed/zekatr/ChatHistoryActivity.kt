package com.muhammed.zekatr

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.muhammed.zekatr.databinding.ActivityChatHistoryBinding

class ChatHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatHistoryBinding
    private lateinit var store: ChatHistoryStore
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = ChatHistoryStore(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnNewChatFromHistory.setOnClickListener { openSession(newSession = true) }

        val sessions = store.listSessions().toMutableList()
        binding.textEmptyHistory.visibility = if (sessions.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        adapter = HistoryAdapter(
            sessions,
            onClick = { openSession(newSession = false, sessionId = it.id) },
            onDelete = { session ->
                store.delete(session.id)
                adapter.removeItem(session)
                binding.textEmptyHistory.visibility = if (adapter.itemCount == 0) android.view.View.VISIBLE else android.view.View.GONE
            }
        )
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter
    }

    private fun openSession(newSession: Boolean, sessionId: String? = null) {
        val prefs = Prefs(this)
        val id = if (newSession) java.util.UUID.randomUUID().toString() else (sessionId ?: prefs.activeSessionId)
        prefs.activeSessionId = id
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
