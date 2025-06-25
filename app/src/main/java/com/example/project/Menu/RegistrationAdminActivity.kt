package com.example.project.Menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.project.Admin.MainAdminActivity
import com.example.project.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationAdminActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    /**
     * Initializes the admin registration form and sets the register button listener.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration_admin)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnRegister.setOnClickListener {
            registerAdmin()
        }
    }

    /**
     * Registers a new admin user.
     *
     * Validates input fields and creates a new admin account in Firebase Authentication.
     * If the registration is successful, navigates to the MainAdminActivity.
     * If there are any errors, displays appropriate messages to the user.
     */
    private fun registerAdmin() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            etPassword.error = "Passwords do not match"
            etConfirmPassword.error = "Passwords do not match"
            etEmail.error = null
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            etEmail.error = "Invalid format"
            etPassword.error = null
            etConfirmPassword.error = null
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
            etPassword.error = "Password too short"
            etEmail.error = null
            etConfirmPassword.error = null
            return
        }

        etFirstName.error = null
        etLastName.error = null
        etEmail.error = null
        etPassword.error = null
        etConfirmPassword.error = null

        Toast.makeText(this, "Attempting registration...", Toast.LENGTH_SHORT).show()
        btnRegister.isEnabled = false

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val adminId = firebaseAuth.currentUser?.uid
                    adminId?.let {
                        val admin = hashMapOf(
                            "email" to email,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "role" to "admin"
                        )

                        db.collection("admins").document(it).set(admin)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Admin registration successful!", Toast.LENGTH_LONG).show()
                                navigateToMainAdminActivity()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error saving admin data: ${e.message}", Toast.LENGTH_LONG).show()
                                btnRegister.isEnabled = true
                            }
                    } ?: run {
                        Toast.makeText(this, "Failed to get user ID after registration.", Toast.LENGTH_LONG).show()
                        btnRegister.isEnabled = true
                    }
                } else {
                    Toast.makeText(this, "Registration error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    btnRegister.isEnabled = true
                }
            }
    }

    /**
     * Navigates to the MainAdminActivity after successful registration.
     */
    private fun navigateToMainAdminActivity() {
        val intent = Intent(this, MainAdminActivity::class.java)
        startActivity(intent)
        finish()
    }
}