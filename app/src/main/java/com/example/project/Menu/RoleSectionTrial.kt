package com.example.project.Menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.project.Admin.MainAdminActivity
import com.example.project.Doctor.MainDoctorActivity
import com.example.project.Patient.MainPatientActivity
import com.example.project.R

class RoleSectionTrial : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_section_trial)

        // Przyciski bez "Register"
        val btnDoctor = findViewById<Button>(R.id.btnDoctor)
        val btnAdmin = findViewById<Button>(R.id.btnAdmin)
        val btnPatient = findViewById<Button>(R.id.btnPatient)

        btnDoctor.setOnClickListener {
            navigateToRole("doctor")
        }

        btnAdmin.setOnClickListener {
            navigateToRole("admin")
        }

        btnPatient.setOnClickListener {
            navigateToRole("patient")
        }
    }

    private fun navigateToRole(role: String) {
        when (role) {
            "doctor" -> {
                val intent = Intent(this, MainDoctorActivity::class.java)
                startActivity(intent)
            }
            "admin" -> {
                val intent = Intent(this, MainAdminActivity::class.java)
                startActivity(intent)
            }
            "patient" -> {
                val intent = Intent(this, MainPatientActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
