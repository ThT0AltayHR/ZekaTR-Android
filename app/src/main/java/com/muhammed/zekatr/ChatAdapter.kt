package com.muhammed.zekatr

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val items: MutableList<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AI = 1
        private const val TYPE_THINKING = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].type) {
            ChatMessage.Type.USER -> TYPE_USER
            ChatMessage.Type.AI -> TYPE_AI
            ChatMessage.Type.THINKING -> TYPE_THINKING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserHolder(inflater.inflate(R.layout.item_message_user, parent, false))
            TYPE_THINKING -> ThinkingHolder(inflater.inflate(R.layout.item_thinking, parent, false))
            else -> AiHolder(inflater.inflate(R.layout.item_message_ai, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is UserHolder -> holder.bind(item)
            is AiHolder -> holder.bind(item)
            is ThinkingHolder -> holder.start(item.text)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ThinkingHolder) holder.stop()
    }

    override fun getItemCount(): Int = items.size

    fun addMessage(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    fun removeLastIfThinking() {
        if (items.isNotEmpty() && items.last().type == ChatMessage.Type.THINKING) {
            items.removeAt(items.size - 1)
            notifyItemRemoved(items.size)
        }
    }

    private class UserHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val text: android.widget.TextView = itemView.findViewById(R.id.textUserMessage)
        fun bind(msg: ChatMessage) {
            text.text = msg.text
        }
    }

    private class AiHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val text: android.widget.TextView = itemView.findViewById(R.id.textAiMessage)
        private val fileLabel: android.widget.TextView = itemView.findViewById(R.id.textFileCreating)
        private val codeBlock: android.widget.TextView = itemView.findViewById(R.id.textCodeBlock)
        private val previewContainer: android.widget.FrameLayout = itemView.findViewById(R.id.previewContainer)

        fun bind(msg: ChatMessage) {
            TypingAnimator.cancel(text)
            if (msg.animate) {
                TypingAnimator.animate(text, msg.text)
            } else {
                text.text = msg.text
            }

            if (msg.fileName != null) {
                fileLabel.visibility = android.view.View.VISIBLE
                fileLabel.text = "⚙ Oluşturuluyor: ${msg.fileName}"
            } else {
                fileLabel.visibility = android.view.View.GONE
            }
            if (msg.code != null) {
                codeBlock.visibility = android.view.View.VISIBLE
                codeBlock.text = msg.code
            } else {
                codeBlock.visibility = android.view.View.GONE
            }

            previewContainer.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)

            msg.youtubePreview?.let { yt ->
                previewContainer.visibility = android.view.View.VISIBLE
                val v = inflater.inflate(R.layout.item_youtube_preview, previewContainer, false)
                v.findViewById<android.widget.TextView>(R.id.textYoutubeTitle).text = yt.title
                v.findViewById<android.widget.TextView>(R.id.textYoutubeAuthor).text = yt.authorName
                val thumb = v.findViewById<android.widget.ImageView>(R.id.imageYoutubeThumb)
                ImageLoader.load(thumb, yt.thumbnailUrl)
                v.setOnClickListener {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.youtube.com/watch?v=${yt.videoId}")
                    )
                    itemView.context.startActivity(intent)
                }
                previewContainer.addView(v)
                return@let
            }

            if (msg.youtubePreview == null) {
                msg.linkPreview?.let { lp ->
                    previewContainer.visibility = android.view.View.VISIBLE
                    val v = inflater.inflate(R.layout.item_link_preview, previewContainer, false)
                    v.findViewById<android.widget.TextView>(R.id.textPreviewTitle).text = lp.title ?: lp.url
                    v.findViewById<android.widget.TextView>(R.id.textPreviewUrl).text = lp.url
                    val img = v.findViewById<android.widget.ImageView>(R.id.imagePreview)
                    if (lp.imageUrl != null) ImageLoader.load(img, lp.imageUrl) else img.visibility = android.view.View.GONE
                    v.setOnClickListener {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(lp.url))
                        itemView.context.startActivity(intent)
                    }
                    previewContainer.addView(v)
                }
            }

            if (msg.youtubePreview == null && msg.linkPreview == null) {
                previewContainer.visibility = android.view.View.GONE
            }
        }
    }

    private class ThinkingHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val dot1: android.view.View = itemView.findViewById(R.id.dot1)
        private val dot2: android.view.View = itemView.findViewById(R.id.dot2)
        private val dot3: android.view.View = itemView.findViewById(R.id.dot3)
        private val label: android.widget.TextView = itemView.findViewById(R.id.textThinkingLabel)
        private val animators = mutableListOf<ValueAnimator>()

        fun start(labelText: String?) {
            stop()
            if (!labelText.isNullOrBlank()) {
                label.visibility = android.view.View.VISIBLE
                label.text = labelText
            } else {
                label.visibility = android.view.View.GONE
            }
            val dots = listOf(dot1, dot2, dot3)
            dots.forEachIndexed { index, dot ->
                val anim = ObjectAnimator.ofFloat(dot, "alpha", 0.25f, 1f, 0.25f)
                anim.duration = 900
                anim.startDelay = (index * 150).toLong()
                anim.repeatCount = ValueAnimator.INFINITE
                anim.start()
                animators.add(anim)
            }
        }

        fun stop() {
            animators.forEach { it.cancel() }
            animators.clear()
        }
    }
}
