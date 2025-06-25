package com.example.project.doctor.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.Service
import com.example.project.util.FirestoreHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

/**
 * A [Fragment] for doctors to manage their services.
 * It displays a list of services and allows adding, editing, and deleting them.
 */
class DoctorServicesFragment : Fragment() {
    private lateinit var servicesRecyclerView: RecyclerView
    private lateinit var fabAddService: FloatingActionButton // Keep for now, but its click listener will be removed
    private lateinit var emptyServicesTextView: TextView
    private val servicesList = mutableListOf<Service>()
    private lateinit var adapter: ServiceAdapter
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth
    private var currentDoctorId: String? = null

    /**
     * Inflates the layout, initializes UI components and Firebase, and sets up the RecyclerView adapter.
     * It also loads the initial list of services.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_services, container, false)

        servicesRecyclerView = view.findViewById(R.id.servicesRecyclerView)
        fabAddService = view.findViewById(R.id.fabAddService)
        emptyServicesTextView = view.findViewById(R.id.emptyServicesTextView)

        firestoreHelper = FirestoreHelper()
        auth = FirebaseAuth.getInstance()
        currentDoctorId = auth.currentUser?.uid

        // Initialize adapter with click handlers
        adapter = ServiceAdapter(
            requireContext(),
            servicesList, // Initially empty, adapter will make a copy
            true, // showAddItem
            onItemClick = { service ->
                showEditDeleteServiceDialog(service)
            },
            onAddItemClick = {
                if (currentDoctorId != null) {
                    showAddServiceDialog()
                } else {
                    Toast.makeText(context, "Cannot add service: Doctor not logged in.", Toast.LENGTH_LONG).show()
                }
            }
        )
        servicesRecyclerView.adapter = adapter

        if (currentDoctorId == null) {
            Log.w("DoctorServices", "Doctor ID is null. User might not be logged in.")
            Toast.makeText(context, "Error: Doctor not logged in.", Toast.LENGTH_LONG).show()
            showEmptyState("Not logged in or Doctor ID not found.")
        } else {
            loadServices()
        }

        // The FAB's click listener is removed as the "add item" in the list handles this.
        // fabAddService.setOnClickListener { ... } // Removed
        fabAddService.visibility = View.GONE // This remains correct

        return view
    }

    /**
     * Loads the services for the current doctor from Firestore.
     */
    private fun loadServices() {
        val doctorId = currentDoctorId
        if (doctorId.isNullOrBlank()) {
            Log.w("DoctorServices", "Doctor ID is not set.")
            showEmptyState("Doctor ID not available.")
            return
        }
        firestoreHelper.getServicesForDoctor(doctorId)
            .addOnSuccessListener { result ->
                val newServiceList = mutableListOf<Service>()
                if (result.isEmpty) {
                    servicesRecyclerView.visibility = View.VISIBLE
                    emptyServicesTextView.visibility = View.GONE
                } else {
                    emptyServicesTextView.visibility = View.GONE
                    servicesRecyclerView.visibility = View.VISIBLE
                    for (document in result) {
                        val service = document.toObject(Service::class.java).copy(id = document.id)
                        newServiceList.add(service)
                    }
                }
                
                // Update fragment's list and then the adapter
                servicesList.clear()
                servicesList.addAll(newServiceList)
                adapter.updateServices(newServiceList) // Update adapter data
            }
            .addOnFailureListener { e ->
                Log.e("DoctorServices", "Error loading services", e)
                showEmptyState("Error loading services.")
                Toast.makeText(context, "Failed to load services.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Shows an empty state message when there are no services to display.
     * @param message The message to be displayed.
     */
    private fun showEmptyState(message: String) {
        servicesRecyclerView.visibility = View.GONE
        emptyServicesTextView.visibility = View.VISIBLE
        emptyServicesTextView.text = message
    }

    /**
     * Shows a dialog to add a new service.
     */
    private fun showAddServiceDialog() {
        val doctorId = currentDoctorId ?: return // Should not be null if this function is called
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_service_doctor, null)
        val serviceNameEditText = dialogView.findViewById<EditText>(R.id.serviceNameEditText)
        val servicePriceEditText = dialogView.findViewById<EditText>(R.id.servicePriceEditText)
        val serviceDurationEditText = dialogView.findViewById<EditText>(R.id.serviceDurationEditText)

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Service")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = serviceNameEditText.text.toString()
                val price = servicePriceEditText.text.toString().toIntOrNull()
                val duration = serviceDurationEditText.text.toString().toIntOrNull()

                if (name.isNotBlank() && price != null && duration != null) {
                    val newService = Service(doctor_id = doctorId, name = name, price = price, duration_minutes = duration)
                    firestoreHelper.addService(newService)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Service added", Toast.LENGTH_SHORT).show()
                            loadServices()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to add service: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Shows a dialog to edit or delete an existing service.
     * @param service The service to be edited or deleted.
     */
    private fun showEditDeleteServiceDialog(service: Service) {
        val serviceDocumentId = service.id

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_service_doctor, null)
        val serviceNameEditText = dialogView.findViewById<EditText>(R.id.serviceNameEditText)
        val servicePriceEditText = dialogView.findViewById<EditText>(R.id.servicePriceEditText)
        val serviceDurationEditText = dialogView.findViewById<EditText>(R.id.serviceDurationEditText)

        serviceNameEditText.setText(service.name)
        servicePriceEditText.setText(service.price.toString())
        serviceDurationEditText.setText(service.duration_minutes.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Service")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = serviceNameEditText.text.toString()
                val price = servicePriceEditText.text.toString().toIntOrNull()
                val duration = serviceDurationEditText.text.toString().toIntOrNull()

                if (name.isNotBlank() && price != null && duration != null) {
                    val updatedServiceData = mapOf(
                        "name" to name,
                        "price" to price,
                        "duration_minutes" to duration
                    )
                    firestoreHelper.updateService(serviceDocumentId, updatedServiceData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Service updated", Toast.LENGTH_SHORT).show()
                            loadServices()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to update service: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Service")
                    .setMessage("Are you sure you want to delete '${service.name}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        firestoreHelper.deleteService(serviceDocumentId)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Service deleted", Toast.LENGTH_SHORT).show()
                                loadServices()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to delete service: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .show()
    }
}