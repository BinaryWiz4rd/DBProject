package com.example.project.chat

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class MessageType {
    TEXT,
    IMAGE,
    DOCUMENT
}

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