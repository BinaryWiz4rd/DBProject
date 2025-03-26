/** package com.example.project.Patient

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R

class PatientActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient)

        val upcomingAppointmentsButton = findViewById<Button>(R.id.upcomingAppointments_button)
        val chatButton = findViewById<Button>(R.id.chat_button)
        val doctorsButton = findViewById<Button>(R.id.doctors_button)

        upcomingAppointmentsButton.setOnClickListener {
            val intent = Intent(this, UpcomingAppointmentsActivity::class.java)
            startActivity(intent)
        }

        chatButton.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        doctorsButton.setOnClickListener {
            val intent = Intent(this, DoctorListActivity::class.java)
            startActivity(intent)
        }
    }
}
*/