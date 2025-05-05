package com.example.project.Admin

data class Doctor(
    var uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val pwz: String = "",
    val specialization: String = "",
    val role: String = "doctor",
    val add: Boolean? = false,
    val delete: Boolean? = false,
    val edit: Boolean? = false
)