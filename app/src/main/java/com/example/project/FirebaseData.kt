package com.example.project

/**
 * Represents a medical service offered by a doctor.
 *
 * @property id The unique identifier of the service.
 * @property doctor_id The ID of the doctor who provides this service.
 * @property name The name of the service (e.g., "General Consultation", "Dental Check-up").
 * @property price The price of the service in a numerical format.
 * @property duration_minutes The duration of the service in minutes.
 */
data class Service(
    var id: String = "",
    val doctor_id: String = "",
    val name: String = "",
    val price: Int = 0,
    val duration_minutes: Int = 0
)

/**
 * Represents a doctor's availability slot.
 *
 * @property id The unique identifier of the availability slot.
 * @property doctor_id The ID of the doctor whose availability this represents.
 * @property date The date of the availability slot in "YYYY-MM-DD" format.
 * @property start_time The start time of the availability slot in "HH:mm" format.
 * @property end_time The end time of the availability slot in "HH:mm" format.
 */
data class Availability(
    var id: String = "",
    val doctor_id: String = "",
    val date: String = "",
    val start_time: String = "",
    val end_time: String = ""
)

/**
 * Represents a patient's booking for a medical service with a doctor.
 *
 * @property id The unique identifier of the booking.
 * @property doctor_id The ID of the doctor for this booking.
 * @property service_id The ID of the service booked.
 * @property date The date of the booking in "YYYY-MM-DD" format.
 * @property start_time The start time of the booking in "HH:mm" format.
 * @property end_time The end time of the booking in "HH:mm" format.
 * @property patient_id The ID of the patient who made the booking.
 * @property patient_name The name of the patient who made the booking.
 * @property status The current status of the booking (e.g., "pending", "confirmed", "completed", "cancelled").
 * @property notes Any additional notes associated with the booking.
 */
data class Booking(
    var id: String = "",
    val doctor_id: String = "",
    val service_id: String = "",
    val date: String = "",
    val start_time: String = "",
    val end_time: String = "",
    val patient_id: String = "",
    val patient_name: String = "",
    val status: String = "",
    val notes: String = ""
)