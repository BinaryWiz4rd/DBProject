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
import com.example.project.R
import com.example.project.Service
import com.example.project.util.FirestoreHelper

class PatientBrowseServicesFragment : Fragment() {

    private lateinit var servicesListView: ListView
    private val servicesList = mutableListOf<Service>()
    private val servicesDisplayList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_patient_browse_services, container, false)

        servicesListView = view.findViewById(R.id.patientServicesListView)
        firestoreHelper = FirestoreHelper()

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, servicesDisplayList)
        servicesListView.adapter = adapter

        loadServices()

        servicesListView.setOnItemClickListener { _, _, position, _ ->
            if (position < servicesList.size) {
                val selectedService = servicesList[position]
                // Navigate to a new fragment to show availability for this service
                // Pass serviceId and doctorId
                val action = PatientBrowseServicesFragmentDirections.actionPatientBrowseServicesFragmentToPatientServiceAvailabilityFragment(
                    selectedService.id,
                    selectedService.doctor_id
                )
                findNavController().navigate(action)
            }
        }

        return view
    }

    private fun loadServices() {
        firestoreHelper.getAllServices() // Assuming a method to get all services from all doctors
            .addOnSuccessListener { result ->
                servicesList.clear()
                servicesDisplayList.clear()
                if (result.isEmpty) {
                    servicesDisplayList.add("No services found.")
                } else {
                    for (document in result) {
                        val service = document.toObject(Service::class.java).copy(id = document.id)
                        servicesList.add(service)
                        // Optionally, fetch doctor's name to display alongside the service
                        servicesDisplayList.add("${service.name} - Price: ${service.price} (Dr. ID: ${service.doctor_id})")
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("PatientBrowseServices", "Error loading services", e)
                servicesDisplayList.clear()
                servicesDisplayList.add("Error loading services.")
                adapter.notifyDataSetChanged()
                Toast.makeText(context, "Failed to load services.", Toast.LENGTH_SHORT).show()
            }
    }
}
