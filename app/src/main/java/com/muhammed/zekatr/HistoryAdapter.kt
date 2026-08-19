package com.muhammed.zekatr

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val items: MutableList<ChatHistoryStore.SessionSummary>,
    private val onClick: (ChatHistoryStore.SessionSummary) -> Unit,
    private val onDelete: (ChatHistoryStore.SessionSummary) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.Holder>() {

    private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"))

    class Holder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val title: android.widget.TextView = view.findViewById(R.id.textSessionTitle)
        val date: android.widget.TextView = view.findViewById(R.id.textSessionDate)
        val delete: android.widget.ImageButton = view.findViewById(R.id.btnDeleteSession)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_session, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.date.text = if (item.updatedAt > 0) dateFormat.format(Date(item.updatedAt)) else ""
        holder.itemView.setOnClickListener { onClick(item) }
        holder.delete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(item: ChatHistoryStore.SessionSummary) {
        val idx = items.indexOf(item)
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
