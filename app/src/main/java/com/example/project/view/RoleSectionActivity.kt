package com.example.project.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R

class RoleSectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_section)

        val btnDoctor = findViewById<Button>(R.id.btnDoctorRegister)
        val btnAdmin = findViewById<Button>(R.id.btnAdminRegister)
        val btnPatient = findViewById<Button>(R.id.btnPatient)

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

    private fun navigateToAuth(role: String) {
        val intent = Intent(this, RegistrationDoctorActivity::class.java)
        intent.putExtra("USER_ROLE", role)
        startActivity(intent)
    }
}
