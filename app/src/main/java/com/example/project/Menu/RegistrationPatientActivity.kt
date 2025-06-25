package com.example.project.Menu

import android.content.Intent
import com.example.project.R
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.Admin.MainAdminActivity
import com.example.project.Patient.MainPatientActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for patient registration.
 */
class RegistrationPatientActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etDateOfBirth: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogIn: Button

    /**
     * Initializes the patient registration form and sets click listeners.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_patient)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etDateOfBirth = findViewById(R.id.etDateOfBirth)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogIn = findViewById(R.id.btnLogIn)

        btnRegister.setOnClickListener {
            registerPatient()
        }

        btnLogIn.setOnClickListener {
            Toast.makeText(
                this,
                "Go to login",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Registers a new patient user with validated inputs and stores info in Firestore.
     */
    private fun registerPatient() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val dateOfBirthStr = etDateOfBirth.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (firstName.isEmpty() || lastName.isEmpty() || dateOfBirthStr.isEmpty() ||
            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
        ) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateOfBirth: Date
        try {
            dateOfBirth = sdf.parse(dateOfBirthStr) as Date
        } catch (e: ParseException) {
            Toast.makeText(this, "Invalid date format (use YYYY-MM-DD)", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (!isAdult(dateOfBirth)) {
            Toast.makeText(
                this,
                "You must be 18 or older to use this application",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val patientId = auth.currentUser?.uid
                    if (patientId != null) {
                        val patient = hashMapOf(
                            "email" to email,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "dateOfBirth" to dateOfBirthStr,
                            "role" to "patient"
                        )

                        db.collection("patients").document(patientId)
                            .set(patient)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registration was successful.", Toast.LENGTH_SHORT).show()
                                navigateToMainPatientActivity()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error saving user data.", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Registration failed.", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Checks if the given date of birth corresponds to an adult (18+ years).
     *
     * @param dateOfBirth Date of birth to validate.
     * @return True if the user is 18 or older, false otherwise.
     */
    private fun isAdult(dateOfBirth: Date): Boolean {
        val dob = Calendar.getInstance().apply { time = dateOfBirth }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age >= 18
    }

    /**
     * Navigates to the main patient activity after successful registration.
     */
    private fun navigateToMainPatientActivity() {
        val intent = Intent(this, MainPatientActivity::class.java)
        startActivity(intent)
        finish()
    }
}