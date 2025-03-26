/**package com.example.project.Doctor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R

class PatientDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_details)

        val nameTextView = findViewById<TextView>(R.id.patientName)
        val surnameTextView = findViewById<TextView>(R.id.patientSurname)
        val peselTextView = findViewById<TextView>(R.id.patientPesel)
        val addressTextView = findViewById<TextView>(R.id.patientAddress)
        val phoneTextView = findViewById<TextView>(R.id.patientPhone)
        val medicalCardButton = findViewById<Button>(R.id.viewMedicalCardButton)

        val name = intent.getStringExtra("NAME") ?: "Unknown"
        val surname = intent.getStringExtra("SURNAME") ?: "Unknown"
        val pesel = intent.getStringExtra("PESEL") ?: "Unknown"
        val address = intent.getStringExtra("ADDRESS") ?: "Unknown"
        val phone = intent.getStringExtra("PHONE") ?: "Unknown"

        nameTextView.text = "Name: $name"
        surnameTextView.text = "Surname: $surname"
        peselTextView.text = "PESEL: $pesel"
        addressTextView.text = "Address: $address"
        phoneTextView.text = "Phone: $phone"

        medicalCardButton.setOnClickListener {
            val intent = Intent(this, MedicalCardActivity::class.java).apply {
                putExtra("NAME", name)
                putExtra("SURNAME", surname)
                putExtra("PESEL", pesel)
            }
            startActivity(intent)
        }
    }
}
*/