package com.chatbotai.app.ui.adapter

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatbotai.app.R
import com.chatbotai.app.model.Message
import com.chatbotai.app.model.ROLE_TYPING
import com.chatbotai.app.model.ROLE_USER
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import java.io.File

class MessageAdapter(private val items: MutableList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AI = 1
        private const val TYPE_TYPING = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].role) {
            ROLE_USER -> TYPE_USER
            ROLE_TYPING -> TYPE_TYPING
            else -> TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserViewHolder(inflater.inflate(R.layout.item_message_user, parent, false))
            TYPE_TYPING -> TypingViewHolder(inflater.inflate(R.layout.item_typing, parent, false))
            else -> AiViewHolder(inflater.inflate(R.layout.item_message_ai, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = items[position]
        when (holder) {
            is UserViewHolder -> holder.bind(message)
            is AiViewHolder -> holder.bind(message)
            is TypingViewHolder -> holder.start()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is TypingViewHolder) holder.stop()
    }

    override fun getItemCount(): Int = items.size

    // ---------- helper dipanggil dari ChatActivity ----------

    fun submitInitial(newItems: List<Message>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    fun showTyping() {
        items.add(Message(sessionId = 0, role = ROLE_TYPING, content = ""))
        notifyItemInserted(items.size - 1)
    }

    fun removeTyping() {
        val idx = items.indexOfLast { it.role == ROLE_TYPING }
        if (idx != -1) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    /** Update konten pesan paling akhir — dipakai buat efek streaming teks AI real-time */
    fun updateLastMessageContent(content: String) {
        if (items.isEmpty()) return
        val lastIndex = items.size - 1
        items[lastIndex].content = content
        notifyItemChanged(lastIndex)
    }

    fun clearAll() {
        items.clear()
        notifyDataSetChanged()
    }

    // ---------- ViewHolders ----------

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)

        fun bind(message: Message) {
            tvMessage.text = message.content
            if (!message.imagePath.isNullOrBlank() && File(message.imagePath).exists()) {
                ivImage.visibility = View.VISIBLE
                ivImage.setImageURI(Uri.fromFile(File(message.imagePath)))
            } else {
                ivImage.visibility = View.GONE
            }
        }
    }

    class AiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)

        // Satu instance Markwon dipakai berulang buat viewholder ini (di-recycle, bukan dibuat tiap bind)
        private val markwon: Markwon = Markwon.builder(itemView.context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()

        fun bind(message: Message) {
            markwon.setMarkdown(tvMessage, message.content)
        }
    }

    class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTyping: TextView = itemView.findViewById(R.id.tvTyping)
        private val handler = Handler(Looper.getMainLooper())
        private var dotCount = 1
        private val runnable = object : Runnable {
            override fun run() {
                tvTyping.text = ".".repeat(dotCount)
                dotCount = if (dotCount >= 3) 1 else dotCount + 1
                handler.postDelayed(this, 400)
            }
        }

        fun start() {
            handler.post(runnable)
        }

        fun stop() {
            handler.removeCallbacks(runnable)
        }
    }
}
