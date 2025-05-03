package com.example.project.Doctor
// to jest do home
import com.example.project.Admin.Doctor
import com.example.project.Admin.Patient

// Renamed to Appointment (singular)
data class Appointment(
    // Added default values and made fields 'var' if they might be updated,
    // or keep 'val' if immutable after creation. Added default constructor hint.
    // Firebase requires a no-arg constructor. Add one if needed, or ensure all fields have defaults.
    val doctor: Doctor = Doctor(), // Provide default instances or make nullable if appropriate
    val patient: Patient = Patient(), // Provide default instances or make nullable if appropriate
    val date: String = "",   // np. "2025-03-31"
    val time: String = ""    // np. "14:30"
    // Add an 'id' field if you need to uniquely identify appointments, especially for updates/deletes
    // var id: String = ""
) {
    // Default constructor for Firebase (if needed and fields don't have defaults)
    // constructor() : this(Doctor(), Patient(), "", "")

    fun getDetails(): String {
        // Ensure doctor and patient objects are not null if they can be
        return "Wizyta u dr. ${doctor.firstName} ${doctor.lastName}(${doctor.specialization}) " +
                "dla pacjenta ${patient.firstName} ${patient.lastName} w dniu $date o godz. $time"
    }
}
