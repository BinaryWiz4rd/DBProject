package com.example.project.chat

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Enumeration of possible message types for chat messages.
 */
enum class MessageType {
    TEXT,
    IMAGE,
    DOCUMENT
}

/**
 * Data class representing a chat message, including text and optional file support.
 *
 * @property id Unique identifier for the message.
 * @property senderId ID of the user who sent the message.
 * @property text Text content of the message.
 * @property timestamp Timestamp when the message was sent (set by Firestore).
 * @property messageType Type of the message (text, image, document).
 * @property fileUrl Optional URL of the attached file.
 * @property fileName Optional name of the attached file.
 * @property fileSize Optional size of the attached file in bytes.
 * @property mimeType Optional MIME type of the attached file.
 */
data class Message(
    var id: String = "",
    var senderId: String = "",
    var text: String = "",
    @ServerTimestamp
    var timestamp: Date? = null,
    
    // File support fields
    var messageType: MessageType = MessageType.TEXT,
    var fileUrl: String? = null,
    var fileName: String? = null,
    var fileSize: Long? = null,
    var mimeType: String? = null
)