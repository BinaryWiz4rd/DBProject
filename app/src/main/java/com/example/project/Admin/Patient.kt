package com.example.project.Admin

/**
 * Data class representing a Patient in the system.
 *
 * @property uid Unique ID of the patient, typically the Firebase Authentication UID.
 * @property email The email address of the patient.
 * @property firstName The first name of the patient.
 * @property lastName The last name of the patient.
 * @property role The role of the user, defaults to "patient".
 * @property add Boolean indicating if the patient has permission to add data. Nullable for flexibility.
 * @property delete Boolean indicating if the patient has permission to delete data. Nullable for flexibility.
 * @property edit Boolean indicating if the patient has permission to edit data. Nullable for flexibility.
 * @property dateOfBirth The patient's date of birth in "YYYY-MM-DD" format.
 * @property phoneNumber The patient's phone number.
 * @property address The patient's address.
 */
data class Patient(
    var uid: String = "",
    var email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "patient",
    val add: Boolean? = false,
    val delete: Boolean? = false,
    val edit: Boolean? = false,
    val dateOfBirth: String = "",
    val phoneNumber: String = "",
    val address: String = ""
)