package com.example.project

data class Service(
    var id: String = "", // Added id field
    val doctor_id: String = "",
    val name: String = "",
    val price: Int = 0,
    val duration_minutes: Int = 0
)

data class Availability(
    var id: String = "", // Added id field
    val doctor_id: String = "",
    val date: String = "",
    val start_time: String = "",
    val end_time: String = ""
)

data class Booking(
    var id: String = "", // Added id field
    val doctor_id: String = "",
    val service_id: String = "",
    val date: String = "",
    val start_time: String = "",
    val end_time: String = "",
    val patient_id: String = "",
    val patient_name: String = "",
    val status: String = "",
    val notes: String = "" // Added notes field to match Firestore documents
)