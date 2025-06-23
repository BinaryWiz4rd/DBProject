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
import com.example.project.R
import com.example.project.Service
import com.example.project.util.FirestoreHelper

class PatientBrowseServicesFragment : Fragment() {

    private lateinit var servicesListView: ListView
    private val servicesList = mutableListOf<Service>()
    private val servicesDisplayList = mutableListOf<String>()
    private val doctorNames = mutableMapOf<String, String>() // Cache for doctor names
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var firestoreHelper: FirestoreHelper
    
    private var selectedDoctorId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_patient_browse_services, container, false)

        // Get the selected doctor ID from arguments
        selectedDoctorId = arguments?.getString("selected_doctor_id")

        servicesListView = view.findViewById(R.id.patientServicesListView)
        firestoreHelper = FirestoreHelper()

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, servicesDisplayList)
        servicesListView.adapter = adapter

        loadServices()

        servicesListView.setOnItemClickListener { _, _, position, _ ->
            if (position < servicesList.size) {
                val selectedService = servicesList[position]
                // Navigate to availability fragment using fragment transaction
                navigateToServiceAvailability(selectedService.id, selectedService.doctor_id)
            }
        }

        return view
    }

    private fun loadServices() {
        val query = if (selectedDoctorId != null) {
            // Load services for specific doctor
            firestoreHelper.getServicesForDoctor(selectedDoctorId!!)
        } else {
            // Load all services if no specific doctor selected
            firestoreHelper.getAllServices()
        }
        
        query
            .addOnSuccessListener { result ->
                servicesList.clear()
                servicesDisplayList.clear()
                
                if (result.isEmpty) {
                    val message = if (selectedDoctorId != null) {
                        "No services found for this doctor."
                    } else {
                        "No services available."
                    }
                    servicesDisplayList.add(message)
                    adapter.notifyDataSetChanged()
                } else {
                    // Process services and load doctor names
                    val services = mutableListOf<Service>()
                    for (document in result) {
                        val service = document.toObject(Service::class.java).copy(id = document.id)
                        services.add(service)
                    }
                    servicesList.addAll(services)
                    loadDoctorNamesAndUpdateDisplay(services)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PatientBrowseServices", "Error loading services", e)
                servicesDisplayList.clear()
                servicesDisplayList.add("Error loading services.")
                adapter.notifyDataSetChanged()
                Toast.makeText(context, "Failed to load services.", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadDoctorNamesAndUpdateDisplay(services: List<Service>) {
        if (services.isEmpty()) {
            adapter.notifyDataSetChanged()
            return
        }
        
        // Get unique doctor IDs
        val doctorIds = services.map { it.doctor_id }.distinct()
        var loadedCount = 0
        
        doctorIds.forEach { doctorId ->
            if (doctorNames.containsKey(doctorId)) {
                // Already cached
                loadedCount++
                if (loadedCount == doctorIds.size) {
                    updateServicesDisplay()
                }
            } else {
                // Load doctor name
                firestoreHelper.getDoctorById(doctorId)
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val doctor = document.toObject(com.example.project.Admin.Doctor::class.java)
                            doctor?.let {
                                doctorNames[doctorId] = "${it.firstName} ${it.lastName}"
                            }
                        } else {
                            doctorNames[doctorId] = "Unknown Doctor"
                        }
                        loadedCount++
                        if (loadedCount == doctorIds.size) {
                            updateServicesDisplay()
                        }
                    }
                    .addOnFailureListener {
                        doctorNames[doctorId] = "Unknown Doctor"
                        loadedCount++
                        if (loadedCount == doctorIds.size) {
                            updateServicesDisplay()
                        }
                    }
            }
        }
    }
    
    private fun updateServicesDisplay() {
        servicesDisplayList.clear()
        for (service in servicesList) {
            val doctorName = doctorNames[service.doctor_id] ?: "Unknown Doctor"
            val displayText = if (selectedDoctorId != null) {
                // When viewing a specific doctor's services, don't show doctor name
                "${service.name}\nPrice: $${service.price} • Duration: ${service.duration_minutes} min"
            } else {
                // When viewing all services, show doctor name
                "${service.name}\nDr. $doctorName • Price: $${service.price} • Duration: ${service.duration_minutes} min"
            }
            servicesDisplayList.add(displayText)
        }
        adapter.notifyDataSetChanged()
    }

    private fun navigateToServiceAvailability(serviceId: String, doctorId: String) {
        val fragment = PatientServiceAvailabilityFragment()
        val bundle = Bundle()
        bundle.putString("service_id", serviceId)
        bundle.putString("doctor_id", doctorId)
        fragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
