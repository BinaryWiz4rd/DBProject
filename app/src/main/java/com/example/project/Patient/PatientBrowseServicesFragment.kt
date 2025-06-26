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

/**
 * A [Fragment] for patients to browse available services.
 *
 * This fragment displays a list of medical services, optionally filtered by a specific doctor.
 * Each service item shows its name, price, duration, and the name of the doctor providing it.
 * Tapping a service navigates to the [PatientServiceAvailabilityFragment] to book an appointment.
 */
class PatientBrowseServicesFragment : Fragment() {

    private lateinit var servicesListView: ListView
    private val servicesList = mutableListOf<Service>()
    private val servicesDisplayList = mutableListOf<String>()
    private val doctorNames = mutableMapOf<String, String>() // Cache for doctor names
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var firestoreHelper: FirestoreHelper

    /**
     * The ID of the doctor for whom services should be displayed.
     * If null, all services are displayed.
     */
    private var selectedDoctorId: String? = null

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_patient_browse_services, container, false)

        selectedDoctorId = arguments?.getString("selected_doctor_id")

        servicesListView = view.findViewById(R.id.patientServicesListView)
        firestoreHelper = FirestoreHelper()

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, servicesDisplayList)
        servicesListView.adapter = adapter

        loadServices()

        servicesListView.setOnItemClickListener { _, _, position, _ ->
            if (position < servicesList.size) {
                val selectedService = servicesList[position]
                navigateToServiceAvailability(selectedService.id, selectedService.doctor_id)
            }
        }

        return view
    }

    /**
     * Loads services from Firestore.
     *
     * If [selectedDoctorId] is set, it loads services only for that specific doctor.
     * Otherwise, it loads all available services. Upon successful retrieval, it populates
     * [servicesList] and then proceeds to load doctor names for display.
     */
    private fun loadServices() {
        val query = if (selectedDoctorId != null) {
            firestoreHelper.getServicesForDoctor(selectedDoctorId!!)
        } else {
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

    /**
     * Loads doctor names for a given list of services and updates the display.
     *
     * This function fetches doctor names from Firestore based on unique doctor IDs
     * present in the provided service list. It caches the doctor names to avoid
     * redundant fetches and updates the UI only after all necessary doctor names are loaded.
     *
     * @param services The list of [Service] objects for which doctor names need to be loaded.
     */
    private fun loadDoctorNamesAndUpdateDisplay(services: List<Service>) {
        if (services.isEmpty()) {
            adapter.notifyDataSetChanged()
            return
        }

        val doctorIds = services.map { it.doctor_id }.distinct()
        var loadedCount = 0

        doctorIds.forEach { doctorId ->
            if (doctorNames.containsKey(doctorId)) {
                loadedCount++
                if (loadedCount == doctorIds.size) {
                    updateServicesDisplay()
                }
            } else {
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

    /**
     * Updates the [servicesDisplayList] with formatted service information,
     * including doctor names if all services are being displayed (not filtered by doctor).
     *
     * Notifies the [adapter] to refresh the ListView.
     */
    private fun updateServicesDisplay() {
        servicesDisplayList.clear()
        for (service in servicesList) {
            val doctorName = doctorNames[service.doctor_id] ?: "Unknown Doctor"
            val displayText = if (selectedDoctorId != null) {
                "${service.name}\nPrice: $${service.price} • Duration: ${service.duration_minutes} min"
            } else {
                "${service.name}\nDr. $doctorName • Price: $${service.price} • Duration: ${service.duration_minutes} min"
            }
            servicesDisplayList.add(displayText)
        }
        adapter.notifyDataSetChanged()
    }

    /**
     * Navigates to the [PatientServiceAvailabilityFragment] to allow the user to select
     * an appointment slot for the chosen service.
     *
     * @param serviceId The ID of the selected service.
     * @param doctorId The ID of the doctor providing the selected service.
     */
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