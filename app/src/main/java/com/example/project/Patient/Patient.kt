package com.example.project.Patient

data class Patient(
    val email: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val role: String = "patient"
)
