package com.example.project.doctor.model

/**
 * Represents a [Doctor] with their personal, contact, and professional details.
 *
 * @property uid Unique identifier for the doctor.
 * @property firstName The doctor's first name.
 * @property lastName The doctor's last name.
 * @property email The doctor's email address.
 * @property specialization The doctor's medical specialization.
 * @property pwzNumber The professional license number (PWZ) required in Poland.
 * @property phoneNumber The doctor's phone number.
 * @property role The user's role, defaults to "doctor".
 */
data class Doctor(
    var uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val specialization: String = "",
    val pwzNumber: String = "",
    val phoneNumber: String = "",
    val role: String = "doctor"
) {
    /**
     * Returns the doctor's full name by combining [firstName] and [lastName].
     */
    val fullName: String
        get() = "$firstName $lastName"

    /**
     * Converts the [Doctor] object to a [HashMap] for Firestore storage.
     * Excludes [uid] as it's typically used as the document ID.
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