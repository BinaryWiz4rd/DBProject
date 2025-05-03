package com.example.project.Doctor

// to jest do calendar nie home
data class AppointmentCalendar(
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