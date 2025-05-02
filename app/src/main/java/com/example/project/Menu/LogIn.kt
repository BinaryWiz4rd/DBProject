package com.example.project.Menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.Admin.MainAdminActivity
import com.example.project.Doctor.MainDoctorActivity
import com.example.project.Patient.MainPatientActivity
import com.example.project.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LogIn : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.btnLogIn)
        btnRegister = findViewById(R.id.registerText)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            loginUser(email, password)
        }

        btnRegister.setOnClickListener {
            val intent = Intent(this, RoleSectionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                    fetchUserDataAndNavigate()
                } else {
                    Toast.makeText(this, "Login Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchUserDataAndNavigate() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val patientDocument = db.collection("patients").document(userId).get().await()
                    if (patientDocument.exists()) {
                        withContext(Dispatchers.Main) {
                            navigateToMainPatientActivity()
                        }
                        return@launch
                    }

                    val doctorDocument = db.collection("doctors").document(userId).get().await()
                    if (doctorDocument.exists()) {
                        withContext(Dispatchers.Main) {
                            navigateToMainDoctorActivity()
                        }
                        return@launch
                    }

                    val adminDocument = db.collection("admins").document(userId).get().await()
                    if (adminDocument.exists()) {
                        withContext(Dispatchers.Main) {
                            navigateToMainAdminActivity()
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LogIn, "User role not found.", Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LogIn, "Error fetching user data: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun navigateToMainPatientActivity() {
        val intent = Intent(this, MainPatientActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToMainDoctorActivity() {
        val intent = Intent(this, MainDoctorActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToMainAdminActivity() {
        val intent = Intent(this, MainAdminActivity::class.java)
        startActivity(intent)
        finish()
    }
}