package com.example.project.Admin

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects

class MainAdminActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var chipDoctors: Chip
    private lateinit var chipPatients: Chip
    private lateinit var chipAdmins: Chip

    private var currentAdapter: RecyclerView.Adapter<*>? = null
    private val doctorsList = mutableListOf<Doctor>()
    private val patientsList = mutableListOf<Patient>()
    private val adminsList = mutableListOf<Admin>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_admin)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        chipDoctors = findViewById(R.id.chip_doctors)
        chipPatients = findViewById(R.id.chip_patients)
        chipAdmins = findViewById(R.id.chip_admins)

        chipDoctors.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d("MainAdmin", "Doctors Chip Clicked")
                fetchDoctors()
            }
        }

        chipPatients.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d("MainAdmin", "Patients Chip Clicked")
                fetchPatients()
            }
        }

        chipAdmins.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d("MainAdmin", "Admins Chip Clicked")
                fetchAdmins()
            }
        }

        if (chipDoctors.isChecked) {
            fetchDoctors()
        }
    }

    private fun fetchDoctors() {
        db.collection("doctors")
            .get()
            .addOnSuccessListener { querySnapshot ->
                doctorsList.clear()
                for (document in querySnapshot) {
                    val doctor = document.toObject(Doctor::class.java)
                    doctorsList.add(doctor)
                }
                val doctorAdapter = DoctorAdapter(doctorsList)
                recyclerView.adapter = doctorAdapter
                currentAdapter = doctorAdapter
            }
            .addOnFailureListener { e ->
                Log.w("MainAdmin", "Error getting doctors.", e)
            }
    }

    private fun fetchPatients() {
        db.collection("patients")
            .get()
            .addOnSuccessListener { querySnapshot ->
                patientsList.clear()
                for (document in querySnapshot) {
                    val patient = document.toObject(Patient::class.java)
                    patientsList.add(patient)
                }
                val patientAdapter = PatientAdapter(patientsList)
                recyclerView.adapter = patientAdapter
                currentAdapter = patientAdapter
            }
            .addOnFailureListener { e ->
                Log.w("MainAdmin", "Error getting patients.", e)
            }
    }

    private fun fetchAdmins() {
        db.collection("admins")
            .get()
            .addOnSuccessListener { querySnapshot ->
                adminsList.clear()
                for (document in querySnapshot) {
                    val admin = document.toObject(Admin::class.java)
                    adminsList.add(admin)
                }
                val adminAdapter = AdminAdapter(adminsList)
                recyclerView.adapter = adminAdapter
                currentAdapter = adminAdapter
            }
            .addOnFailureListener { e ->
                Log.w("MainAdmin", "Error getting admins.", e)
            }
    }
}