package com.example.project.chat

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Chat(
    var id: String = "",
    val participants: List<String> = emptyList(),
    var lastMessage: String = "",
    @ServerTimestamp
    var lastMessageTimestamp: Date? = null,
    val patientId: String = "",
    val doctorId: String = "",
    val patientName: String = "",
    val doctorName: String = ""
) 