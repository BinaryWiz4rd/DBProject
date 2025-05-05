package com.example.project.Admin

data class Admin(
    var uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "admin",
    val add: Boolean? = false,
    val delete: Boolean? = false,
    val edit: Boolean? = false
)