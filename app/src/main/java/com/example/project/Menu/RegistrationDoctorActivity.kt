package com.example.project.Menu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.project.doctor.ui.MainDoctorActivity
import com.example.project.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegistrationDoctorActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnAlreadyHaveAccount: TextView

    private lateinit var etDoctorFirstName: EditText
    private lateinit var etDoctorLastName: EditText
    private lateinit var etPWZ: EditText
    private lateinit var spinnerSpecialization: Spinner

    private var userRole: String = "doctor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_doctor)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnAlreadyHaveAccount = findViewById(R.id.btnLogIn)

        etDoctorFirstName = findViewById(R.id.etDoctorFirstName)
        etDoctorLastName = findViewById(R.id.etDoctorLastName)
        etPWZ = findViewById(R.id.etPWZ)
        spinnerSpecialization = findViewById(R.id.spinnerSpecialization)

        userRole = intent.getStringExtra("USER_ROLE") ?: "doctor"

        if (userRole == "doctor") {
            val specializations = listOf("Surgeon", "Dermatologist", "Orthopedist", "Urologist",
                "Neurologist", "Orthodontist", "Anesthesiologist", "Cardiologist")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, specializations)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSpecialization.adapter = adapter
        } else {
            spinnerSpecialization.visibility = View.GONE
        }

        btnRegister.setOnClickListener {
            registerDoctor()
        }

        btnAlreadyHaveAccount.setOnClickListener {
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }
    }

    /**
     * Registers a new doctor user with email, password, and profile details.
     * Validates inputs, creates Firebase Auth user, and stores doctor info in Firestore.
     */
    private fun registerDoctor() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val firstName = etDoctorFirstName.text.toString().trim()
        val lastName = etDoctorLastName.text.toString().trim()
        val pwz = etPWZ.text.toString().trim()
        val specialization = spinnerSpecialization.selectedItem.toString()

        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || pwz.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (!pwz.matches(Regex("\\d{7,}"))) {
            Toast.makeText(this, "Wrong PWZ number (minimum of 7 digits)", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val doctorId = auth.currentUser ?.uid
                    if (doctorId != null) {
                        val doctor = hashMapOf(
                            "email" to email,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "pwz" to pwz,
                            "specialization" to specialization,
                            "role" to "doctor",
                            "add" to true,
                            "delete" to false,
                            "edit" to false
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                db.collection("doctors").document(doctorId)
                                    .set(doctor)
                                    .await()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@RegistrationDoctorActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    navigateToMainDoctorActivity()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@RegistrationDoctorActivity, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Navigates to the main doctor activity after successful registration.
     */
    private fun navigateToMainDoctorActivity() {
        val intent = Intent(this, MainDoctorActivity::class.java)
        startActivity(intent)
        finish()
    }
}