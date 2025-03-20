package com.example.project.view

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R

class PatientListActivity : AppCompatActivity() {

    private lateinit var patientListView: ListView
    private val patientList = listOf(
        Patient("John", "Doe", "12345678901", "123 Main St", "123-456-789"),
        Patient("Alice", "Smith", "98765432109", "456 Elm St", "987-654-321")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_list)

        patientListView = findViewById(R.id.patientListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, patientList.map { it.name + " " + it.surname })
        patientListView.adapter = adapter

        patientListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedPatient = patientList[position]
            val intent = Intent(this, PatientDetailsActivity::class.java).apply {
                putExtra("NAME", selectedPatient.name)
                putExtra("SURNAME", selectedPatient.surname)
                putExtra("PESEL", selectedPatient.pesel)
                putExtra("ADDRESS", selectedPatient.address)
                putExtra("PHONE", selectedPatient.phone)
            }
            startActivity(intent)
        }
    }
}

data class Patient(
    val name: String,
    val surname: String,
    val pesel: String,
    val address: String,
    val phone: String
)
