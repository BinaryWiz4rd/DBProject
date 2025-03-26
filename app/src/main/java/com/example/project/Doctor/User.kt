package com.example.project.Doctor

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val specialty: String? = null // Używane tylko dla lekarzy
)
