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
 * This activity is a work in progress and contains several TODOs.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorChatMessageBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var currentUserId: String // ID zalogowanego użytkownika (np. lekarza)
    private lateinit var otherUserId: String // ID użytkownika, z którym czatujemy (np. pacjenta)
    private val chatMessages = mutableListOf<ChatMessage>() // Lista wiadomości

    // Launchery do wybierania plików
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

        // TODO: Pobierz ID użytkowników (np. z Intent)
        currentUserId = "doctor_123" // Przykładowe ID
        otherUserId = intent.getStringExtra("OTHER_USER_ID") ?: "patient_456" // Przykładowe ID

        setupToolbar()
        setupRecyclerView()
        setupInputListeners()
        registerFilePickers()

        loadChatHistory() // Załaduj historię czatu (TODO)
        listenForNewMessages() // Nasłuchuj na nowe wiadomości (TODO)
    }

    /**
     * Sets up the toolbar with a back button and a title.
     * TODO: Set the title to the patient's name.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarChat)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Przycisk wstecz
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
                stackFromEnd = true // Nowe wiadomości na dole i przewijanie
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
            showAttachmentOptions() // Pokaż opcje: Zdjęcie / Dokument
        }
    }

    /**
     * Registers the activity result launchers for picking media and documents.
     */
    private fun registerFilePickers() {
        // Launcher dla zdjęć i wideo (nowoczesny sposób)
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                Log.d("ChatActivity", "Wybrano medium: $uri")
                uploadFile(uri, MessageType.IMAGE) // Lub określ typ dokładniej
            } else {
                Log.d("ChatActivity", "Nie wybrano medium")
            }
        }

        // Launcher dla dokumentów (ogólny)
        pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                Log.d("ChatActivity", "Wybrano dokument: $uri")
                // Trzeba zachować flagi persistable permission
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
     * TODO: Implement a proper dialog (AlertDialog or BottomSheet) for attachment options.
     */
    private fun showAttachmentOptions() {
        // TODO: Implementacja okna dialogowego / BottomSheet z opcjami
        // Na razie uproszczone: Wybierz obraz, potem dokument (do testów)
        // W prawdziwej aplikacji użyj AlertDialog lub BottomSheetDialog

        // Przykład: Wybór obrazu
        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

        // Przykład: Wybór dokumentu (po kliknięciu innej opcji w dialogu)
        // pickDocumentLauncher.launch(arrayOf("*/*")) // Akceptuje wszystkie typy
        // Można specyfikować MIME types np. arrayOf("application/pdf", "image/*")
    }


    /**
     * Sends a text message.
     * TODO: Implement sending the message to a backend service.
     * @param text The text of the message to send.
     */
    private fun sendMessage(text: String) {
        val message = ChatMessage(
            senderId = currentUserId,
            receiverId = otherUserId,
            textContent = text,
            messageType = MessageType.TEXT
            // ID i timestamp zostaną nadane przez backend lub lokalnie przed wysłaniem
        )
        // TODO: Implementacja wysyłania wiadomości do backendu (np. Firebase Firestore/RTDB)
        Log.d("ChatActivity", "Wysyłanie wiadomości tekstowej: ${message.textContent}")

        // Tymczasowo dodaj lokalnie dla testów UI
        addMessageToList(message)
    }

    /**
     * Uploads a file.
     * TODO: Implement the file upload to a storage service and update the message accordingly.
     * @param fileUri The URI of the file to upload.
     * @param messageType The type of the message (IMAGE or DOCUMENT).
     */
    private fun uploadFile(fileUri: Uri, messageType: MessageType) {
        // TODO: Implementacja uploadu pliku do storage (np. Firebase Storage)
        Log.d("ChatActivity", "Rozpoczęcie uploadu pliku: $fileUri, Typ: $messageType")
        Toast.makeText(this, "Rozpoczynanie uploadu...", Toast.LENGTH_SHORT).show()

        // 1. Wyświetl wiadomość z plikiem lokalnie (ze wskaźnikiem ładowania)
        val tempMessage = ChatMessage(
            senderId = currentUserId,
            receiverId = otherUserId,
            messageType = messageType,
            localFileUri = fileUri, // Przekaż URI lokalnego pliku
            fileName = getFileName(fileUri), // Pobierz nazwę pliku z URI
            isSending = true // Oznacz jako wysyłanie
        )
        addMessageToList(tempMessage)

        // 2. Rozpocznij upload w tle (np. używając WorkManager lub Coroutines + Retrofit/Firebase Storage SDK)

        // 3. Po pomyślnym uploadzie:
        //    a. Pobierz URL pliku (`fileUrl`) z backendu/storage.
        //    b. Utwórz/zaktualizuj wiadomość w backendzie (np. Firestore) z `fileUrl`, `fileName`, `fileSize`.
        //    c. Zaktualizuj wiadomość w lokalnej liście (usuń `localFileUri`, ustaw `fileUrl`, `isSending = false`).

        // 4. W razie błędu uploadu:
        //    a. Oznacz wiadomość jako nieudaną w UI.
        //    b. Daj użytkownikowi opcję ponowienia próby.
    }


    /**
     * Loads the chat history.
     * TODO: Implement fetching the chat history from a backend service.
     */
    private fun loadChatHistory() {
        // TODO: Implementacja pobierania historii czatu z backendu
        Log.d("ChatActivity", "Ładowanie historii czatu...")
        // Po załadowaniu danych:
        // chatMessages.addAll(pobraneWiadomosci)
        // chatAdapter.submitList(chatMessages.toList()) // Użyj toList() dla nowej, niemutowalnej listy
        // binding.recyclerViewChat.scrollToPosition(chatAdapter.itemCount - 1) // Przewiń na dół
    }

    /**
     * Listens for new messages.
     * TODO: Implement listening for new messages from a backend service (e.g., WebSocket, Firestore listener).
     */
    private fun listenForNewMessages() {
        // TODO: Implementacja nasłuchiwania na nowe wiadomości (np. przez Firebase Listener, WebSocket)
        Log.d("ChatActivity", "Nasłuchiwanie na nowe wiadomości...")
        // Gdy nadejdzie nowa wiadomość:
        // val newMessage = ... // obiekt ChatMessage z backendu
        // addMessageToList(newMessage)
    }

    /**
     * Adds a message to the local list and updates the adapter.
     * TODO: Consider handling duplicate messages if listening for changes.
     * @param message The message to add.
     */
    private fun addMessageToList(message: ChatMessage) {
        // TODO: Rozważ obsługę duplikatów, jeśli nasłuchujesz na zmiany
        chatMessages.add(message)
        chatAdapter.submitList(chatMessages.toList()) // Przekaż nową kopię listy
        // Płynne przewinięcie, jeśli użytkownik jest blisko dołu
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
            fileName = uri.lastPathSegment // Fallback
        }
        return fileName
    }
}