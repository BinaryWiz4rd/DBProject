package com.example.project.Doctor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.project.R
import com.google.firebase.firestore.FirebaseFirestore

class DoctorScheduleFragment : Fragment() {

    private lateinit var patientsListView: ListView
    private lateinit var patientsTitleTextView: TextView
    private val patientsList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_schedule, container, false)

        patientsListView = view.findViewById(R.id.patientsListView)
        patientsTitleTextView = view.findViewById(R.id.patientsTitleTextView)

        patientsTitleTextView.text = "Patients for Today"

        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            patientsList
        )
        patientsListView.adapter = adapter

        fetchPatientList()

        patientsListView.setOnItemClickListener { parent, view, position, id ->
            val selectedPatientId = parent.getItemAtPosition(position) as String
            //zastanawiam sie nad dodaniem id pacjenta, bo co jesli beda dwa Anna Nowak?
            Log.d("DoctorSchedule", "Selected patient: $selectedPatientId")
            //TODO: po kliknieciu niech sie otworzy medical card info
        }

        return view
    }

    private fun fetchPatientList() {
        db.collection("patients")
            .get()
            .addOnSuccessListener { result ->
                patientsList.clear()
                for (document in result) {
                    val firstName = document.getString("firstName")
                    val lastName = document.getString("lastName")
                    if (firstName != null && lastName != null) {
                        patientsList.add("$firstName $lastName")
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.w("DoctorSchedule", "Error getting documents.", e)
            }
    }
}