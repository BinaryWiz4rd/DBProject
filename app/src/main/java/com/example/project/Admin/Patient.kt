package com.example.project.Admin

data class Patient(
    var uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "patient",
    val add: Boolean? = false,
    val delete: Boolean? = false,
    val edit: Boolean? = false,
    val dateOfBirth: String =""
)