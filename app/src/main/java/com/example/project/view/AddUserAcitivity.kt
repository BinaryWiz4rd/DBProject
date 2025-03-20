package com.example.project.view

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// AddUserActivity.kt
class AddUserActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etSpecialty: EditText
    private lateinit var btnSave: Button

    private var role: String = "patient"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etSpecialty = findViewById(R.id.etSpecialty)
        btnSave = findViewById(R.id.btnSave)

        // Odczytujemy przekazaną rolę ("doctor" lub "patient")
        role = intent.getStringExtra("role") ?: "patient"
        if (role == "doctor") {
            etSpecialty.visibility = View.VISIBLE
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val specialty = if (role == "doctor") etSpecialty.text.toString().trim() else null

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() ||
                (role == "doctor" && specialty.isNullOrEmpty())) {
                Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tworzenie konta użytkownika w Firebase Auth
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result?.user?.uid ?: ""
                        // Zapis dodatkowych danych użytkownika w Firestore
                        val userData = hashMapOf(
                            "id" to uid,
                            "name" to name,
                            "email" to email,
                            "role" to role,
                            "specialty" to specialty
                        )
                        FirebaseFirestore.getInstance().collection("users")
                            .document(uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Użytkownik dodany pomyślnie", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Błąd przy zapisie danych: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(this, "Błąd przy tworzeniu konta: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
