package com.example.project.Patient

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.project.Availability
import com.example.project.R
import com.example.project.util.FirestoreHelper

class PatientServiceAvailabilityFragment : Fragment() {

    private lateinit var availabilityListView: ListView
    private val availabilityList = mutableListOf<Availability>()
    private val availabilityDisplayList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var firestoreHelper: FirestoreHelper
    private val args: PatientServiceAvailabilityFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_patient_service_availability, container, false)

        availabilityListView = view.findViewById(R.id.patientAvailabilityListView)
        firestoreHelper = FirestoreHelper()

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, availabilityDisplayList)
        availabilityListView.adapter = adapter

        Log.d("PatientServiceAvailability", "Service ID: ${args.serviceId}, Doctor ID: ${args.doctorId}")

        loadAvailability(args.doctorId)

        availabilityListView.setOnItemClickListener { _, _, position, _ ->
            if (position < availabilityList.size) {
                val selectedAvailability = availabilityList[position]
                val action = PatientServiceAvailabilityFragmentDirections
                    .actionPatientServiceAvailabilityFragmentToConfirmBookingFragment(
                        serviceId = args.serviceId,
                        doctorId = args.doctorId,
                        availabilityId = selectedAvailability.id,
                        date = selectedAvailability.date,
                        startTime = selectedAvailability.start_time,
                        endTime = selectedAvailability.end_time
                    )
                findNavController().navigate(action)
            }
        }

        return view
    }

    private fun loadAvailability(doctorId: String) {
        firestoreHelper.getAllAvailabilityForDoctor(doctorId)
            .get()
            .addOnSuccessListener { result ->
                availabilityList.clear()
                availabilityDisplayList.clear()
                if (result.isEmpty) {
                    availabilityDisplayList.add("No availability found for this doctor.")
                } else {
                    for (document in result) {
                        val availability = document.toObject(Availability::class.java).copy(id = document.id)
                        availabilityList.add(availability)
                        availabilityDisplayList.add("Date: ${availability.date} - From: ${availability.start_time} To: ${availability.end_time}")
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("PatientServiceAvailability", "Error loading availability", e)
                availabilityDisplayList.clear()
                availabilityDisplayList.add("Error loading availability.")
                adapter.notifyDataSetChanged()
                Toast.makeText(context, "Failed to load availability.", Toast.LENGTH_SHORT).show()
            }
    }
}
