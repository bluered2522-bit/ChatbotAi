package com.chatbotai.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatbotai.app.R
import com.chatbotai.app.model.ChatSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val items: MutableList<ChatSession>,
    private val onClick: (ChatSession) -> Unit,
    private val onDelete: (ChatSession) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID"))

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvSessionTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvSessionDate)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = items[position]
        holder.tvTitle.text = session.title
        holder.tvDate.text = dateFormat.format(Date(session.createdAt))
        holder.itemView.setOnClickListener { onClick(session) }
        holder.btnDelete.setOnClickListener { onDelete(session) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<ChatSession>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeItem(session: ChatSession) {
        val idx = items.indexOfFirst { it.id == session.id }
        if (idx != -1) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}
