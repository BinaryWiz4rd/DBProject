package com.example.project.chat

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    var id: String = "",
    var senderId: String = "",
    var text: String = "",
    @ServerTimestamp
    var timestamp: Date? = null
) 