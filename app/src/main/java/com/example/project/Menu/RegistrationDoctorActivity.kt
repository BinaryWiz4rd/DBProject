/** package com.example.project.Menu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationDoctorActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Pola podstawowe
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnAlreadyHaveAccount: TextView

    // Dodatkowe pola dla doktora
    private lateinit var etDoctorFirstName: EditText
    private lateinit var etDoctorLastName: EditText
    private lateinit var etPWZ: EditText
    private lateinit var spinnerSpecialization: Spinner

    private lateinit var userRole: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Zakładamy, że ten layout został przygotowany dla rejestracji doktorów
        setContentView(R.layout.activity_registration_doctor)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicjalizacja widoków
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnAlreadyHaveAccount = findViewById(R.id.btnLogIn)

        // Inicjalizacja dodatkowych pól specyficznych dla doktora
        etDoctorFirstName = findViewById(R.id.etDoctorFirstName)
        etDoctorLastName = findViewById(R.id.etDoctorLastName)
        etPWZ = findViewById(R.id.etPWZ)
        spinnerSpecialization = findViewById(R.id.spinnerSpecialization)

        // Pobranie roli użytkownika (powinna być "doctor" tylko dla przycisku doktor w RoleSelectionActivity)
        userRole = intent.getStringExtra("USER_ROLE") ?: "doctor"

        // Jeśli rejestrujemy doktora, ustaw Spinner z listą specjalizacji
        if (userRole == "doctor") {
            val specializations = listOf("Anestezjolog", "Laryngolog", "Kardiolog", "Neurolog", "Dermatolog")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, specializations)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSpecialization.adapter = adapter
        } else {
            // W przypadku innej roli (jeśli to miałoby się zdarzyć) ukryj Spinner
            spinnerSpecialization.visibility = View.GONE
        }

        btnRegister.setOnClickListener {
            registerUser()
        }

        btnAlreadyHaveAccount.setOnClickListener {
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val firstName = etDoctorFirstName.text.toString().trim()
        val lastName = etDoctorLastName.text.toString().trim()
        val pwz = etPWZ.text.toString().trim()
        // Pobierz wybraną specjalizację
        val specialization = if (userRole == "doctor") spinnerSpecialization.selectedItem.toString() else ""

        // Walidacja – upewnij się, że wszystkie pola są wypełnione
        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || pwz.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        // Tworzymy obiekt użytkownika zawierający dodatkowe dane
                        val user = hashMapOf(
                            "email" to email,
                            "role" to userRole,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "pwz" to pwz,
                            "specialization" to specialization
                        )

                        db.collection("users").document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
*/