package com.example.project.chat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var messagesListener: ListenerRegistration

    private var chatId: String? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        firestoreHelper = FirestoreHelper()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        recyclerView = findViewById(R.id.recyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        chatAdapter = ChatAdapter(mutableListOf())
        recyclerView.adapter = chatAdapter

        val patientId = intent.getStringExtra("patientId")
        val doctorId = intent.getStringExtra("doctorId")
        val patientName = intent.getStringExtra("patientName")
        val doctorName = intent.getStringExtra("doctorName")
        val chatTitle = intent.getStringExtra("chatTitle")

        supportActionBar?.title = chatTitle ?: "Chat"


        if (patientId != null && doctorId != null && patientName != null && doctorName != null) {
            firestoreHelper.getOrCreateChat(patientId, doctorId, patientName, doctorName)
                .addOnSuccessListener { id ->
                    chatId = id
                    listenForMessages()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to start chat: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (messageText.isNotEmpty() && chatId != null && currentUserId != null) {
                val message = Message(
                    senderId = currentUserId!!,
                    text = messageText,
                    timestamp = Date()
                )
                firestoreHelper.sendMessage(chatId!!, message)
                    .addOnSuccessListener {
                        messageEditText.text.clear()
                        recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun listenForMessages() {
        if (chatId != null) {
            messagesListener = firestoreHelper.getChatMessages(chatId!!)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error fetching messages: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val messages = snapshot.toObjects(Message::class.java)
                        chatAdapter = ChatAdapter(messages.toMutableList())
                        recyclerView.adapter = chatAdapter
                        recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::messagesListener.isInitialized) {
            messagesListener.remove()
        }
    }
} 