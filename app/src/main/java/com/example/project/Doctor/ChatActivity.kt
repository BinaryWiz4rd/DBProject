package com.example.project.doctor

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.databinding.ActivityDoctorChatMessageBinding

/**
 * An activity for handling chat between a doctor and a patient.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorChatMessageBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var currentUserId: String
    private lateinit var otherUserId: String
    private val chatMessages = mutableListOf<ChatMessage>()

    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickDocumentLauncher: ActivityResultLauncher<Array<String>>


    /**
     * Initializes the activity, sets up the toolbar, RecyclerView, input listeners,
     * and file pickers. It also initiates loading the chat history and listening for new messages.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorChatMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = "doctor_123"
        otherUserId = intent.getStringExtra("OTHER_USER_ID") ?: "patient_456"

        setupToolbar()
        setupRecyclerView()
        setupInputListeners()
        registerFilePickers()

        loadChatHistory()
        listenForNewMessages()
    }

    /**
     * Sets up the toolbar with a back button and a title.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarChat)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbarChat.title = "Czat z Pacjentem ID: $otherUserId"
        binding.toolbarChat.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }


    /**
     * Sets up the RecyclerView with a [ChatAdapter] and a [LinearLayoutManager].
     */
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(currentUserId)
        binding.recyclerViewChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
        }
    }

    /**
     * Sets up the listeners for the send and attach buttons.
     */
    private fun setupInputListeners() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.editTextMessage.text.clear()
            }
        }

        binding.buttonAttach.setOnClickListener {
            showAttachmentOptions()
        }
    }

    /**
     * Registers the activity result launchers for picking media and documents.
     */
    private fun registerFilePickers() {
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                Log.d("ChatActivity", "Wybrano medium: $uri")
                uploadFile(uri, MessageType.IMAGE)
            } else {
                Log.d("ChatActivity", "Nie wybrano medium")
            }
        }

        pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                Log.d("ChatActivity", "Wybrano dokument: $uri")
                contentResolver.takePersistableUriPermission(uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uploadFile(uri, MessageType.DOCUMENT)
            } else {
                Log.d("ChatActivity", "Nie wybrano dokumentu")
            }
        }
    }

    /**
     * Shows options for attaching files.
     */
    private fun showAttachmentOptions() {
        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    /**
     * Sends a text message.
     * @param text The text of the message to send.
     */
    private fun sendMessage(text: String) {
        val message = ChatMessage(
            senderId = currentUserId,
            receiverId = otherUserId,
            textContent = text,
            messageType = MessageType.TEXT
        )
        Log.d("ChatActivity", "Wysyłanie wiadomości tekstowej: ${message.textContent}")

        addMessageToList(message)
    }

    /**
     * Uploads a file.
     * @param fileUri The URI of the file to upload.
     * @param messageType The type of the message (IMAGE or DOCUMENT).
     */
    private fun uploadFile(fileUri: Uri, messageType: MessageType) {
        Log.d("ChatActivity", "Rozpoczęcie uploadu pliku: $fileUri, Typ: $messageType")
        Toast.makeText(this, "Rozpoczynanie uploadu...", Toast.LENGTH_SHORT).show()

        val tempMessage = ChatMessage(
            senderId = currentUserId,
            receiverId = otherUserId,
            messageType = messageType,
            localFileUri = fileUri,
            fileName = getFileName(fileUri),
            isSending = true
        )
        addMessageToList(tempMessage)
    }


    /**
     * Loads the chat history.
     */
    private fun loadChatHistory() {
        Log.d("ChatActivity", "Ładowanie historii czatu...")
    }

    /**
     * Listens for new messages.
     */
    private fun listenForNewMessages() {
        Log.d("ChatActivity", "Nasłuchiwanie na nowe wiadomości...")
    }

    /**
     * Adds a message to the local list and updates the adapter.
     * @param message The message to add.
     */
    private fun addMessageToList(message: ChatMessage) {
        chatMessages.add(message)
        chatAdapter.submitList(chatMessages.toList())
        binding.recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    /**
     * A helper function to get the file name from a URI.
     * @param uri The URI of the file.
     * @return The file name, or null if it cannot be determined.
     */
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Błąd przy pobieraniu nazwy pliku", e)
            fileName = uri.lastPathSegment
        }
        return fileName
    }
}