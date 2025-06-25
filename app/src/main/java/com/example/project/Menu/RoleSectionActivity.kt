package com.example.project.Menu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.example.project.Menu.RegistrationDoctorActivity
import com.example.project.Menu.RegistrationAdminActivity
import com.example.project.Menu.RegistrationPatientActivity


class RoleSectionActivity : AppCompatActivity() {

    /**
     * Initializes the role selection screen and sets up button listeners for different user roles.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_section)

        // Upewnij się, że ID przycisków w R.layout.activity_role_section są poprawne
        // Zakładam, że są to: btnDoctorRegister, btnAdminRegister, btnPatientRegister
        val btnDoctor = findViewById<Button>(R.id.btnDoctorRegister)
        val btnAdmin = findViewById<Button>(R.id.btnAdminRegister)
        // Zmieniono ID dla przycisku pacjenta, jeśli tak masz w XML
        val btnPatient = findViewById<Button>(R.id.btnPatient) // Lub R.id.btnPatient jeśli tak jest w XML

        btnDoctor.setOnClickListener {
            navigateToAuth("doctor")
        }

        btnAdmin.setOnClickListener {
            navigateToAuth("admin")
        }

        btnPatient.setOnClickListener {
            navigateToAuth("patient")
        }
    }

    /**
     * Navigates to the registration activity based on the selected role.
     *
     * @param role The user role to register ("doctor", "admin", or "patient").
     */
    private fun navigateToAuth(role: String) {
        // Wybierz docelową Aktywność na podstawie roli
        val targetActivity = when (role) {
            "doctor" -> RegistrationDoctorActivity::class.java
            "admin" -> RegistrationAdminActivity::class.java // Upewnij się, że ta klasa istnieje
            "patient" -> RegistrationPatientActivity::class.java // Upewnij się, że ta klasa istnieje
            else -> {
                // Obsługa nieznanej roli - np. logowanie błędu lub domyślna aktywność
                Log.e("RoleSectionActivity", "Unknown role: $role")
                null // lub jakaś domyślna aktywność np. LoginActivity::class.java
            }
        }

        // Jeśli znaleziono odpowiednią aktywność, uruchom ją
        if (targetActivity != null) {
            val intent = Intent(this, targetActivity)
            intent.putExtra("USER_ROLE", role) // Nadal przekazujesz rolę, może się przydać
            startActivity(intent)
        }
    }
}