package com.example.project.Admin

/**
 * Data class representing a Doctor in the system.
 *
 * @property uid Unique ID of the doctor, typically the Firebase Authentication UID.
 * @property email The email address of the doctor.
 * @property firstName The first name of the doctor.
 * @property lastName The last name of the doctor.
 * @property pwz The PWZ (Prawo Wykonywania Zawodu) number of the doctor, which is a professional license number in Poland.
 * @property specialization The medical specialization of the doctor.
 * @property role The role of the user, defaults to "doctor".
 * @property add Boolean indicating if the doctor has permission to add data. Nullable for flexibility.
 * @property delete Boolean indicating if the doctor has permission to delete data. Nullable for flexibility.
 * @property edit Boolean indicating if the doctor has permission to edit data. Nullable for flexibility.
 * @property fcmToken The Firebase Cloud Messaging token for sending notifications to the doctor's device.
 */
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
    val edit: Boolean? = false,
    val fcmToken: String = ""
)