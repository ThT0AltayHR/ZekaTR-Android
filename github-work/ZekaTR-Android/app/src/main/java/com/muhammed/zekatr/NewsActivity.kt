package com.muhammed.zekatr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.muhammed.zekatr.databinding.ActivityNewsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsBinding
    private lateinit var adapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBackNews.setOnClickListener { finish() }

        adapter = NewsAdapter { url ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        binding.recyclerNews.layoutManager = LinearLayoutManager(this)
        binding.recyclerNews.adapter = adapter

        load()
    }

    private fun load() {
        binding.progressNews.visibility = View.VISIBLE
        binding.textNewsEmpty.visibility = View.GONE
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { NewsHelper.fetchAll() }
            binding.progressNews.visibility = View.GONE
            if (items.isEmpty()) {
                binding.textNewsEmpty.visibility = View.VISIBLE
            } else {
                adapter.submit(items)
            }
        }
    }
}

class NewsAdapter(private val onClick: (String) -> Unit) : RecyclerView.Adapter<NewsAdapter.VH>() {

    private var items: List<NewsItem> = emptyList()

    fun submit(newItems: List<NewsItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textNewsTitle)
        val source: TextView = view.findViewById(R.id.textNewsSource)
        val time: TextView = view.findViewById(R.id.textNewsTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.source.text = "🔗 ${item.sourceName}"
        holder.time.text = item.pubDate?.take(16) ?: ""
        holder.itemView.setOnClickListener { onClick(item.link) }
    }

    override fun getItemCount() = items.size
}
