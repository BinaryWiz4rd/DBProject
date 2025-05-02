package com.example.project.Doctor

data class Appointment(
    var id: String = "",
    val patientName: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val notes: String = "",
    val time: String
)
data class WorkingHours(
    val startHour: Int = 8,
    val endHour: Int = 14
)