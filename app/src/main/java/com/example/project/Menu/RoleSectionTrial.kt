/** package com.example.project.Menu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.project.Patient.PatientActivity
import com.example.project.R
import com.example.project.Doctor.DoctorActivity

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
                val intent = Intent(this, DoctorActivity::class.java)
                startActivity(intent)
            }
            "admin" -> {
                val intent = Intent(this, AdminActivity::class.java)
                startActivity(intent)
            }
            "patient" -> {
                val intent = Intent(this, PatientActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
*/