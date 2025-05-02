package com.example.project.Admin

data class Patient(
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val role: String = "patient"
)