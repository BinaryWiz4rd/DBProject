package com.example.project.doctor

import com.example.project.Admin.Patient
import com.example.project.doctor.model.Doctor

data class Appointment(
    val doctor: Doctor = Doctor(),
    val patient: Patient = Patient(),
    val date: String = "",
    val time: String = "",
    val serviceName: String = "",
    val servicePrice: Int = 0,
    val serviceDuration: Int = 0
) {
    fun getDetails(): String {
        return "Wizyta u dr. ${doctor.firstName} ${doctor.lastName}(${doctor.specialization}) " +
                "dla pacjenta ${patient.firstName} ${patient.lastName} w dniu $date o godz. $time"
    }
}
