package com.muhammed.zekatr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.muhammed.zekatr.databinding.ActivityMemoryManagementBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemoryManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryManagementBinding
    private lateinit var memory: MemoryManager
    private lateinit var adapter: MemoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        memory = MemoryManager(this)

        binding.btnBackMemory.setOnClickListener { finish() }
        binding.btnClearAllMemory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Tüm bellek silinsin mi?")
                .setMessage("Öğrenilen tüm bilgiler kalıcı olarak silinecek.")
                .setPositiveButton("Sil") { _, _ -> memory.clearAll(); refresh() }
                .setNegativeButton("Vazgeç", null)
                .show()
        }

        adapter = MemoryAdapter { id ->
            memory.delete(id)
            refresh()
        }
        binding.recyclerMemory.layoutManager = LinearLayoutManager(this)
        binding.recyclerMemory.adapter = adapter

        refresh()
    }

    private fun refresh() {
        val items = memory.listAll()
        adapter.submit(items)
        binding.textMemoryEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}

class MemoryAdapter(private val onDelete: (String) -> Unit) : RecyclerView.Adapter<MemoryAdapter.VH>() {

    private var items: List<MemoryItem> = emptyList()
    private val dateFmt = SimpleDateFormat("d MMM yyyy HH:mm", Locale("tr"))

    fun submit(newItems: List<MemoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.textMemoryText)
        val source: TextView = view.findViewById(R.id.textMemorySource)
        val delete: View = view.findViewById(R.id.btnDeleteMemory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_memory, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.text.text = item.text
        val sourceLabel = if (item.source == "manual") "✍️ Manuel kayıt" else "🤖 Otomatik öğrenildi"
        holder.source.text = "$sourceLabel · ${dateFmt.format(Date(item.createdAt))}"
        holder.delete.setOnClickListener { onDelete(item.id) }
    }

    override fun getItemCount() = items.size
}
