package com.example.project.doctor

data class WorkingHours(
    val startHour: Int = 8,
    val endHour: Int = 14
)

data class AppointmentCalendar(
    var id: String = "",
    val patientName: String = "",
    val patientId: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val notes: String = "",
    val time: String = "",
    val endTime: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val servicePrice: Int = 0,
    val serviceDuration: Int = 0
)