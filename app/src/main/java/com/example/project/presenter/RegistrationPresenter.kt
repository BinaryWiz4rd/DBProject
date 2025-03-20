package com.example.project.presenter

import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationPresenter(private val view: RegistrationView) {
    private val userModel = UserModel()

    fun handleRegistration(email: String, password: String, repeatPassword: String, username: String, role: String = "user") {
        // Validate that none of the fields are empty
        if (email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty() || username.isEmpty()) {
            view.showError("All fields are required!")
            return
        }

        // Validate email format using Android Patterns
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            view.showError("Invalid email format!")
            return
        }

        // Ensure the passwords match
        if (password != repeatPassword) {
            view.showError("Passwords do not match!")
            return
        }

        // Proceed with user registration
        userModel.registerUser(email, password, username, role) { success, errorMessage ->
            if (success) {
                view.navigateToLobby()
            } else {
                view.showError(errorMessage ?: "Registration failed.")
            }
        }
    }
}

interface RegistrationView {
    fun showError(message: String)
    fun navigateToLobby()
}

class UserModel {
    fun registerUser(email: String, password: String, username: String, role: String, callback: (Boolean, String?) -> Unit) {
        val auth = FirebaseAuth.getInstance()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val user = hashMapOf(
                            "email" to email,
                            "username" to username,
                            "role" to role
                        )

                        // Save user data to Firestore
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                callback(true, null)
                            }
                            .addOnFailureListener { e ->
                                callback(false, e.message)
                            }
                    } else {
                        callback(false, "User ID is null")
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Registration failed."
                    callback(false, errorMessage)
                }
            }
    }
}
