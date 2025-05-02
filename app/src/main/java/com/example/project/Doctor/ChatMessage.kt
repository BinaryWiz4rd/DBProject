package com.example.project.Doctor// Dostosuj pakiet

import android.net.Uri
import java.util.Date

// Typy wiadomości
enum class MessageType {
    TEXT,
    IMAGE,
    DOCUMENT
}

data class ChatMessage(
    val id: String = "", // Unikalne ID wiadomości (np. z Firebase)
    val senderId: String = "", // ID nadawcy (np. "doctor_123", "patient_456")
    val receiverId: String = "", // ID odbiorcy
    val timestamp: Long = System.currentTimeMillis(), // Czas wysłania
    val messageType: MessageType = MessageType.TEXT,
    val textContent: String? = null, // Treść dla wiadomości tekstowych
    val fileUrl: String? = null, // URL do pobrania pliku (po uploadzie)
    val fileName: String? = null, // Oryginalna nazwa pliku
    val fileSize: Long? = null, // Rozmiar pliku (opcjonalnie)

    // Pola pomocnicze (nie muszą być w backendzie)
    @Transient var localFileUri: Uri? = null, // Tymczasowy URI pliku przed uploadem
    @Transient var isSending: Boolean = false // Czy wiadomość jest w trakcie wysyłania/uploadu?
) {
    // Dodatkowy konstruktor dla łatwiejszego tworzenia wiadomości tekstowych
    constructor(sender: String, receiver: String, text: String) : this(
        senderId = sender,
        receiverId = receiver,
        messageType = MessageType.TEXT,
        textContent = text
    )
}