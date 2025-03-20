package com.example.project.model

import com.google.firebase.database.FirebaseDatabase

// Patient Model
class PatientModel {
    private val database = FirebaseDatabase.getInstance().reference

    data class Patient(
        val userId: String = "",
        val username: String = "",
        val email: String = "",
        val dateOfBirth: String = "",
        val gender: String = "",
        val medicalHistory: String = "",
        val adminPermissions: Boolean = false
    )

    fun savePatientData(patient: Patient, callback: (Boolean, String?) -> Unit) {
        database.child("patients").child(patient.userId).setValue(patient)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(true, null)
                else callback(false, task.exception?.message)
            }
    }

    fun getPatientData(userId: String, callback: (Patient?) -> Unit) {
        database.child("patients").child(userId).get().addOnSuccessListener { snapshot ->
            val patient = snapshot.getValue(Patient::class.java)
            callback(patient)
        }.addOnFailureListener {
            callback(null)
        }
    }
}