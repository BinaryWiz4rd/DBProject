package com.example.project.Doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.project.R

class DoctorScheduleFragment : Fragment() {

    private lateinit var patientsListView: ListView
    private lateinit var patientsTitleTextView: TextView

    private val patientsList = listOf(
        "Anna Nowak",
        "Jan Kowalski",
        "Adam Koala",
        "Emma Watson",
        "David Goggins"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_schedule, container, false)

        patientsListView = view.findViewById(R.id.patientsListView)
        patientsTitleTextView = view.findViewById(R.id.patientsTitleTextView)

        patientsTitleTextView.text = "Patients for Today"

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            patientsList
        )
        patientsListView.adapter = adapter

        patientsListView.setOnItemClickListener { parent, view, position, id ->
            val selectedPatient = parent.getItemAtPosition(position) as String
            //tu otwórz medical card info, czyli szczegóły o pacjencie
        }

        return view
    }
}
