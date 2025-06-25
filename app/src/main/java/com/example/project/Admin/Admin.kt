package com.example.project.Admin

/**
 * Represents an administrator with their details and permissions.
 *
 * @property uid The unique identifier of the admin.
 * @property email The email of the admin.
 * @property firstName The first name of the admin.
 * @property lastName The last name of the admin.
 * @property role The role of the user, which is "admin".
 * @property add Permission to add content.
 * @property delete Permission to delete content.
 * @property edit Permission to edit content.
 */
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