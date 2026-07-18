package com.chatbotai.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatbotai.app.R
import com.chatbotai.app.db.ChatRepository
import com.chatbotai.app.ui.adapter.HistoryAdapter
import com.chatbotai.app.util.Prefs

class HistoryActivity : AppCompatActivity() {

    private lateinit var repository: ChatRepository
    private lateinit var prefs: Prefs
    private lateinit var adapter: HistoryAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        repository = ChatRepository(this)
        prefs = Prefs(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvEmpty = findViewById(R.id.tvEmpty)
        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)

        adapter = HistoryAdapter(
            mutableListOf(),
            onClick = { session ->
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra(ChatActivity.EXTRA_SESSION_ID, session.id)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                prefs.lastSessionId = session.id
                startActivity(intent)
                finish()
            },
            onDelete = { session ->
                repository.deleteSession(session.id)
                adapter.removeItem(session)
                if (prefs.lastSessionId == session.id) prefs.lastSessionId = -1L
                refreshEmptyState()
            }
        )
        rvHistory.adapter = adapter

        loadSessions()
    }

    private fun loadSessions() {
        val sessions = repository.getSessions(prefs.loggedInUserId)
        adapter.submit(sessions)
        refreshEmptyState()
    }

    private fun refreshEmptyState() {
        tvEmpty.visibility = if (adapter.itemCount == 0) TextView.VISIBLE else TextView.GONE
    }
}
