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
    /**
     * A computed property that returns the full name of the doctor,
     * combining their first name and last name.
     */
    val fullName: String
        get() = "$firstName $lastName"

    /**
     * Converts the [Doctor] object into a [HashMap] suitable for storage in Firestore.
     * The `uid` field is not included in the HashMap as it typically serves as the document ID in Firestore.
     *
     * @return A [HashMap] containing the doctor's data.
     */
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