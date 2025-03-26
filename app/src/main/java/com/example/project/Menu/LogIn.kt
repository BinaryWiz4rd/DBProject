/**package com.example.project.Menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.Doctor.DoctorActivity
import com.google.firebase.auth.FirebaseAuth

class LogIn : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnAlreadyHaveAccount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.btnLogIn)

        // Zmień na właściwy ID (np. registerText) zamiast btnLogIn
        btnAlreadyHaveAccount = findViewById(R.id.registerText)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            loginUser(email, password)
        }

        btnAlreadyHaveAccount.setOnClickListener {
            // Przykładowe przejście do ekranu rejestracji
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

                    // Po zalogowaniu przenosimy do DoctorActivity
                    val intent = Intent(this, DoctorActivity::class.java)
                    startActivity(intent)
                    finish() // Zamknij aktywność logowania, aby nie wracać do niej przy cofnięciu
                } else {
                    Toast.makeText(this, "Login Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /* POTEM TA KLASA MOZE WYGLADAC TAK
    private fun loginUser (email: String, password: String) {
    if (email.isEmpty() || password.isEmpty()) {
        Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        return
    }

    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                //fetchujemy info z bazy danych z firebase
                val userId = auth.currentUser ?.uid
                if (userId != null) {
                    val userModel = UserModel()
                    userModel.getUser Data(userId) { user ->
                        if (user != null) {
                            if (user.role == "doctor") {
                                //start MainDoctorActivity
                                val intent = Intent(this, MainDoctorActivity::class.java)
                                startActivity(intent)
                            } else {
                                przypadki z pacjentem i adminem
                            }
                        }
                    }
                }
                finish()
            } else {
                Toast.makeText(this, "Login Failed!", Toast.LENGTH_SHORT).show()
            }
        }
}

}
*/