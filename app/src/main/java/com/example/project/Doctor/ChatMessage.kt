package com.example.project.doctor

import android.net.Uri
import java.util.Date

/**
 * An enum representing the different types of chat messages.
 */
enum class MessageType {
    TEXT,
    IMAGE,
    DOCUMENT
}

/**
 * Represents a single chat message.
 *
 * @property id The unique ID of the message.
 * @property senderId The ID of the sender.
 * @property receiverId The ID of the receiver.
 * @property timestamp The time the message was sent.
 * @property messageType The type of the message.
 * @property textContent The text content of the message.
 * @property fileUrl The URL of an attached file.
 * @property fileName The name of an attached file.
 * @property fileSize The size of an attached file.
 * @property localFileUri A transient property for the local URI of a file before upload.
 * @property isSending A transient property to indicate if the message is currently being sent.
 */
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
    /**
     * An additional constructor for easily creating text messages.
     * @param sender The ID of the sender.
     * @param receiver The ID of the receiver.
     * @param text The text content of the message.
     */
    constructor(sender: String, receiver: String, text: String) : this(
        senderId = sender,
        receiverId = receiver,
        messageType = MessageType.TEXT,
        textContent = text
    )
}