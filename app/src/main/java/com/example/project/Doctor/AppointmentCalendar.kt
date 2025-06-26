package com.example.project.doctor

/**
 * Data class representing the working hours of a doctor.
 *
 * @param startHour The starting hour of the working day (24-hour format). Defaults to 8.
 * @param endHour The ending hour of the working day (24-hour format). Defaults to 14.
 */
data class WorkingHours(
    val startHour: Int = 8,
    val endHour: Int = 14
)

/**
 * Data class representing an appointment in the doctor's calendar.
 *
 * @param id The unique ID of the appointment.
 * @param patientName The name of the patient.
 * @param patientId The ID of the patient.
 * @param date The date of the appointment in string format.
 * @param timeSlot The start time of the appointment slot in string format (e.g., "HH:MM").
 * @param notes Any additional notes for the appointment.
 * @param time The exact start time of the appointment in string format.
 * @param endTime The exact end time of the appointment in string format.
 * @param serviceId The ID of the service booked for the appointment.
 * @param serviceName The name of the service booked for the appointment.
 * @param servicePrice The price of the service.
 * @param serviceDuration The duration of the service in minutes.
 */
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
    var serviceName: String = "",
    var servicePrice: Int = 0,
    var serviceDuration: Int = 0
)