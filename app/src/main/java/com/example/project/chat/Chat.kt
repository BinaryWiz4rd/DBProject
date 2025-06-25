package com.example.project.chat

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class representing a chat between a patient and a doctor.
 *
 * @property id Unique identifier for the chat.
 * @property participants List of user IDs participating in the chat.
 * @property lastMessage The last message sent in the chat.
 * @property lastMessageTimestamp Timestamp of the last message (set by Firestore).
 * @property patientId ID of the patient in the chat.
 * @property doctorId ID of the doctor in the chat.
 * @property patientName Name of the patient.
 * @property doctorName Name of the doctor.
 */
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