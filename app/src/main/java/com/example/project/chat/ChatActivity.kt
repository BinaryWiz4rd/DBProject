package com.example.project.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.example.project.util.FirebaseStorageHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

/**
 * An activity that displays a chat screen between two users.
 * It handles sending and receiving text messages and file attachments.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var attachButton: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var storageHelper: FirebaseStorageHelper
    private lateinit var messagesListener: ListenerRegistration

    private var chatId: String? = null
    private var currentUserId: String? = null

    // File picker launchers
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickDocumentLauncher: ActivityResultLauncher<Array<String>>

    /**
     * Initializes the activity, sets up the toolbar, and initializes
     * Firebase helpers, UI components, and listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firestoreHelper = FirestoreHelper()
        storageHelper = FirebaseStorageHelper()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        recyclerView = findViewById(R.id.recyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        attachButton = findViewById(R.id.attachButton)

        setupRecyclerView()
        setupFilePickers()
        setupClickListeners()
        initializeChat()
    }

    /**
     * Sets up the RecyclerView with a [LinearLayoutManager] and a [ChatAdapter].
     */
    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        chatAdapter = ChatAdapter(mutableListOf())
        recyclerView.adapter = chatAdapter
    }

    /**
     * Initializes the activity result launchers for picking images and documents.
     */
    private fun setupFilePickers() {
        // Image picker
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { 
                uploadFile(it, MessageType.IMAGE)
            }
        }

        // Document picker
        pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                // Take persistable permission
                contentResolver.takePersistableUriPermission(
                    it, 
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                uploadFile(it, MessageType.DOCUMENT)
            }
        }
    }

    /**
     * Sets up the click listeners for the send and attach buttons.
     */
    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (messageText.isNotEmpty() && chatId != null && currentUserId != null) {
                sendTextMessage(messageText)
                messageEditText.text.clear()
            }
        }

        attachButton.setOnClickListener {
            showAttachmentOptions()
        }
    }

    /**
     * Initializes the chat by getting or creating a a chat session between the patient and doctor.
     * It retrieves user details from the intent extras.
     */
    private fun initializeChat() {
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
    }

    /**
     * Sends a text message to the current chat.
     * @param messageText The text of the message to be sent.
     */
    private fun sendTextMessage(messageText: String) {
        chatId?.let { chatId ->
            currentUserId?.let { senderId ->
                val message = Message(
                    senderId = senderId,
                    text = messageText,
                    timestamp = Date(),
                    messageType = MessageType.TEXT
                )
                
                firestoreHelper.sendMessage(chatId, message)
                    .addOnSuccessListener {
                        recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    /**
     * Shows a dialog with options for attaching a file (Image or Document).
     */
    private fun showAttachmentOptions() {
        val options = arrayOf("Image", "Document")
        
        AlertDialog.Builder(this)
            .setTitle("Choose attachment type")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    1 -> pickDocumentLauncher.launch(arrayOf("*/*"))
                }
            }
            .show()
    }

    /**
     * Uploads a file to Firebase Storage and sends a corresponding message in the chat.
     *
     * @param fileUri The URI of the file to be uploaded.
     * @param messageType The type of the message ([MessageType.IMAGE] or [MessageType.DOCUMENT]).
     */
    private fun uploadFile(fileUri: Uri, messageType: MessageType) {
        chatId?.let { chatId ->
            currentUserId?.let { senderId ->
                // Create temporary message to show upload progress
                val tempMessageId = UUID.randomUUID().toString()
                val tempMessage = Message(
                    id = tempMessageId,
                    senderId = senderId,
                    text = "Uploading...",
                    timestamp = Date(),
                    messageType = messageType
                )
                
                chatAdapter.addMessage(tempMessage)
                recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                
                // Upload file to Firebase Storage
                storageHelper.uploadFile(
                    context = this,
                    fileUri = fileUri,
                    chatId = chatId,
                    senderId = senderId,
                    onProgress = { progress ->
                        // You could update progress here if needed
                    },
                    onSuccess = { downloadUrl, fileName, fileSize, mimeType ->
                        // Create final message with file data
                        val finalMessage = Message(
                            senderId = senderId,
                            text = if (messageType == MessageType.IMAGE) "" else fileName,
                            timestamp = Date(),
                            messageType = messageType,
                            fileUrl = downloadUrl,
                            fileName = fileName,
                            fileSize = fileSize,
                            mimeType = mimeType
                        )
                        
                        // Save to Firestore
                        firestoreHelper.sendMessage(chatId, finalMessage)
                            .addOnSuccessListener {
                                // Remove temporary message and let Firestore listener add the real one
                                val tempIndex = chatAdapter.getMessages().indexOfFirst { it.id == tempMessageId }
                                if (tempIndex != -1) {
                                    chatAdapter.getMessages().removeAt(tempIndex)
                                    chatAdapter.notifyItemRemoved(tempIndex)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to send file: ${e.message}", Toast.LENGTH_SHORT).show()
                                // Remove temporary message
                                val tempIndex = chatAdapter.getMessages().indexOfFirst { it.id == tempMessageId }
                                if (tempIndex != -1) {
                                    chatAdapter.getMessages().removeAt(tempIndex)
                                    chatAdapter.notifyItemRemoved(tempIndex)
                                }
                            }
                    },
                    onFailure = { exception ->
                        Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        // Remove temporary message
                        val tempIndex = chatAdapter.getMessages().indexOfFirst { it.id == tempMessageId }
                        if (tempIndex != -1) {
                            chatAdapter.getMessages().removeAt(tempIndex)
                            chatAdapter.notifyItemRemoved(tempIndex)
                        }
                    }
                )
            }
        }
    }

    /**
     * Sets up a Firestore snapshot listener to listen for new messages in the current chat
     * and updates the RecyclerView adapter accordingly.
     */
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

    /**
     * Handles the "Up" button navigation.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * Cleans up resources, specifically removing the Firestore listener when the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (::messagesListener.isInitialized) {
            messagesListener.remove()
        }
    }
} 