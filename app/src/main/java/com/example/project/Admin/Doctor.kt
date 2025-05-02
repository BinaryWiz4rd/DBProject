package com.example.project.Admin

data class Doctor(
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val pwz: String = "",
    val specialization: String = "",
    val role: String = "doctor"
)