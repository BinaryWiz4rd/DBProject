package com.example.project.doctor

import com.example.project.Admin.Patient
import com.example.project.doctor.model.Doctor

/**
 * Represents an appointment between a doctor and a patient.
 *
 * @property doctor The doctor for the appointment.
 * @property patient The patient for the appointment.
 * @property date The date of the appointment.
 * @property time The time of the appointment.
 * @property serviceName The name of the service for the appointment.
 * @property servicePrice The price of the service.
 * @property serviceDuration The duration of the service in minutes.
 */
data class Appointment(
    val doctor: Doctor = Doctor(),
    val patient: Patient = Patient(),
    val date: String = "",
    val time: String = "",
    val serviceName: String = "",
    val servicePrice: Int = 0,
    val serviceDuration: Int = 0
) {
    /**
     * Returns a string with the details of the appointment.
     * @return A formatted string with appointment details.
     */
    fun getDetails(): String {
        return "Wizyta u dr. ${doctor.firstName} ${doctor.lastName}(${doctor.specialization}) " +
                "dla pacjenta ${patient.firstName} ${patient.lastName} w dniu $date o godz. $time"
    }
}
