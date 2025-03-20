package com.example.project.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UserModel {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    data class User(
        val userId: String = "",
        val username: String = "",
        val email: String = "",
        val role: String = "Patient",
        val contactNumber: String = ""
    )

    fun registerUser(
        email: String,
        password: String,
        username: String,
        role: String,
        contactNumber: String = "",
        callback: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val newUser = User(
                        userId = userId ?: "",
                        username = username,
                        email = email,
                        role = role,
                        contactNumber = contactNumber
                    )

                    userId?.let {
                        database.child("users").child(it).setValue(newUser)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    callback(true, null)
                                } else {
                                    callback(false, dbTask.exception?.message)
                                }
                            }
                    }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun loginUser(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun getUserData(userId: String, callback: (User?) -> Unit) {
        database.child("users").child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            callback(user)
        }.addOnFailureListener {
            callback(null)
        }
    }

    fun updateUser(userId: String, updatedUser: User, callback: (Boolean, String?) -> Unit) {
        database.child("users").child(userId).setValue(updatedUser)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun logoutUser() {
        auth.signOut()
    }
}
