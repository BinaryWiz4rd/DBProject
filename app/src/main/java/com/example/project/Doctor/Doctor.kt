package com.example.project.Doctor

data class Doctor(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val pwz: String,
    val specialization: String,
    val role: String = "doctor"
)
