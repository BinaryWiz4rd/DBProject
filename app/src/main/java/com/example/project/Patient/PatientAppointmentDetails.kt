package com.example.project.Patient

import com.example.project.Booking
import com.example.project.Service
import com.example.project.Admin.Doctor

/**
 * Enhanced appointment data class that includes detailed information
 * for display in the patient appointments list
 */
data class PatientAppointmentDetails(
    val booking: Booking,
    val doctorName: String = "Loading...",
    val serviceName: String = "Loading...",
    val servicePrice: Int = 0,
    val serviceDuration: Int = 0
) {
    val id: String get() = booking.id
    val date: String get() = booking.date
    val startTime: String get() = booking.start_time
    val endTime: String get() = booking.end_time
    val status: String get() = booking.status
    val doctorId: String get() = booking.doctor_id
    val serviceId: String get() = booking.service_id
}
