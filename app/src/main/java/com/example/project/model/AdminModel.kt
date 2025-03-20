package com.example.project.model

import com.google.firebase.database.FirebaseDatabase

// Admin Model
class AdminModel {
    private val database = FirebaseDatabase.getInstance().reference

    data class Administrator(
        val userId: String = "",
        val username: String = "",
        val email: String = "",
        val adminPermissions: Boolean = true
    )

    fun saveAdminData(admin: Administrator, callback: (Boolean, String?) -> Unit) {
        database.child("admins").child(admin.userId).setValue(admin)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(true, null)
                else callback(false, task.exception?.message)
            }
    }

    fun getAdminData(userId: String, callback: (Administrator?) -> Unit) {
        database.child("admins").child(userId).get().addOnSuccessListener { snapshot ->
            val admin = snapshot.getValue(Administrator::class.java)
            callback(admin)
        }.addOnFailureListener {
            callback(null)
        }
    }
}
