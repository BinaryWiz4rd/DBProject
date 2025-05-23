package com.example.project

data class Doctor(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val specialization: String = "",
    val pwzNumber: String = "",
    val phoneNumber: String = "",
    val role: String = "doctor"
) {
    // Computed property for full name
    val fullName: String
        get() = "$firstName $lastName"

    // Helper method to convert doctor to HashMap for Firestore storage
    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "specialization" to specialization,
            "pwzNumber" to pwzNumber,
            "phoneNumber" to phoneNumber,
            "role" to role
        )
    }
}