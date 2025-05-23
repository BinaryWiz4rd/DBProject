package com.example.project.Admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.project.R
import com.example.project.Service
import com.example.project.util.FirestoreHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment to display and manage services for a specific doctor
 */
class AdminServicesFragment : Fragment() {

    private val TAG = "AdminServicesFragment"
    
    // UI components
    private lateinit var servicesListView: ListView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var fabAddService: FloatingActionButton
    
    // Data
    private val servicesList = mutableListOf<Service>()
    private lateinit var adapter: ServiceAdapter
    
    // Firebase
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var db: FirebaseFirestore
    
    // Arguments
    private var doctorId: String = ""
    private var doctorName: String = ""

    companion object {
        fun newInstance(doctorId: String): AdminServicesFragment {
            val fragment = AdminServicesFragment()
            val args = Bundle()
            args.putString("doctorId", doctorId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            doctorId = it.getString("doctorId", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_services, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        firestoreHelper = FirestoreHelper()
        db = firestoreHelper.getDbInstance()
        
        // Initialize UI components
        servicesListView = view.findViewById(R.id.adminServicesListView)
        loadingIndicator = view.findViewById(R.id.adminServicesLoadingIndicator)
        emptyStateTextView = view.findViewById(R.id.adminServicesEmptyStateTextView)
        titleTextView = view.findViewById(R.id.adminServicesTitleTextView)
        fabAddService = view.findViewById(R.id.fabAddService)
        
        // First, get the doctor's name
        loadDoctorInfo()
        
        // Setup adapter
        adapter = ServiceAdapter(
            requireContext(), 
            servicesList, 
            true, 
            onEditClick = { service -> showEditServiceDialog(service) },
            onDeleteClick = { service -> showDeleteServiceDialog(service) }
        )
        servicesListView.adapter = adapter
        
        // Setup add service button
        fabAddService.setOnClickListener {
            showAddServiceDialog()
        }
        
        // Load services
        loadServices()
    }
    
    private fun loadDoctorInfo() {
        if (doctorId.isEmpty()) {
            titleTextView.text = "Doctor Services"
            return
        }
        
        db.collection("doctors").document(doctorId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    doctorName = "Dr. $firstName $lastName"
                    titleTextView.text = "Services for $doctorName"
                } else {
                    titleTextView.text = "Services for Doctor ID: $doctorId"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading doctor info", e)
                titleTextView.text = "Doctor Services"
            }
    }
    
    private fun loadServices() {
        showLoading(true)
        
        // Create query based on whether we have a doctor ID
        val query = if (doctorId.isNotEmpty()) {
            db.collection("services").whereEqualTo("doctor_id", doctorId)
        } else {
            db.collection("services")
        }
        
        query.get()
            .addOnSuccessListener { result ->
                servicesList.clear()
                
                if (result.isEmpty) {
                    val message = if (doctorId.isNotEmpty()) {
                        "No services found for this doctor"
                    } else {
                        "No services found"
                    }
                    showEmptyState(true, message)
                    showLoading(false)
                    return@addOnSuccessListener
                }
                
                for (document in result) {
                    val service = document.toObject(Service::class.java).copy(id = document.id)
                    servicesList.add(service)
                }
                
                // Sort services by name
                servicesList.sortBy { it.name }
                
                adapter.notifyDataSetChanged()
                showEmptyState(servicesList.isEmpty())
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading services", e)
                showEmptyState(true, "Error loading services: ${e.message}")
                showLoading(false)
            }
    }
    
    private fun showAddServiceDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_service, null)
        
        val serviceNameEditText = dialogView.findViewById<EditText>(R.id.serviceNameEditText)
        val servicePriceEditText = dialogView.findViewById<EditText>(R.id.servicePriceEditText)
        val serviceDurationEditText = dialogView.findViewById<EditText>(R.id.serviceDurationEditText)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add New Service")
            .setView(dialogView)
            .setPositiveButton("Add", null) // We'll override this later
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Override positive button to handle validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = serviceNameEditText.text.toString().trim()
            val priceText = servicePriceEditText.text.toString().trim()
            val durationText = serviceDurationEditText.text.toString().trim()
            
            // Validate inputs
            var isValid = true
            
            if (name.isEmpty()) {
                serviceNameEditText.error = "Service name is required"
                isValid = false
            }
            
            val price = try {
                priceText.toInt()
            } catch (e: NumberFormatException) {
                servicePriceEditText.error = "Please enter a valid price"
                isValid = false
                0
            }
            
            val duration = try {
                durationText.toInt()
            } catch (e: NumberFormatException) {
                serviceDurationEditText.error = "Please enter a valid duration"
                isValid = false
                0
            }
            
            if (isValid) {
                // Create and add the service
                val service = Service(
                    id = "", // Will be set by Firestore
                    doctor_id = doctorId,
                    name = name,
                    price = price,
                    duration_minutes = duration
                )
                
                addService(service, dialog)
            }
        }
    }
    
    private fun addService(service: Service, dialog: AlertDialog) {
        showLoading(true)
        
        // Create a new document in the services collection
        db.collection("services")
            .add(service)
            .addOnSuccessListener { documentReference ->
                // Create a new service object with the Firestore ID
                val newService = service.copy(id = documentReference.id)
                
                // Add to local list and refresh adapter
                servicesList.add(newService)
                servicesList.sortBy { it.name }
                adapter.notifyDataSetChanged()
                
                Toast.makeText(context, "Service added successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showEmptyState(servicesList.isEmpty())
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding service", e)
                Toast.makeText(context, "Error adding service: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    
    private fun showEditServiceDialog(service: Service) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_service, null)
        
        val serviceNameEditText = dialogView.findViewById<EditText>(R.id.serviceNameEditText)
        val servicePriceEditText = dialogView.findViewById<EditText>(R.id.servicePriceEditText)
        val serviceDurationEditText = dialogView.findViewById<EditText>(R.id.serviceDurationEditText)
        
        // Set current values
        serviceNameEditText.setText(service.name)
        servicePriceEditText.setText(service.price.toString())
        serviceDurationEditText.setText(service.duration_minutes.toString())
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Service")
            .setView(dialogView)
            .setPositiveButton("Save", null) // We'll override this later
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Override positive button to handle validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = serviceNameEditText.text.toString().trim()
            val priceText = servicePriceEditText.text.toString().trim()
            val durationText = serviceDurationEditText.text.toString().trim()
            
            // Validate inputs
            var isValid = true
            
            if (name.isEmpty()) {
                serviceNameEditText.error = "Service name is required"
                isValid = false
            }
            
            val price = try {
                priceText.toInt()
            } catch (e: NumberFormatException) {
                servicePriceEditText.error = "Please enter a valid price"
                isValid = false
                0
            }
            
            val duration = try {
                durationText.toInt()
            } catch (e: NumberFormatException) {
                serviceDurationEditText.error = "Please enter a valid duration"
                isValid = false
                0
            }
            
            if (isValid) {
                // Create updated service
                val updatedService = Service(
                    id = service.id,
                    doctor_id = service.doctor_id,
                    name = name,
                    price = price,
                    duration_minutes = duration
                )
                
                updateService(updatedService, dialog)
            }
        }
    }
    
    private fun updateService(service: Service, dialog: AlertDialog) {
        showLoading(true)
        
        // Update the service in Firestore
        db.collection("services").document(service.id)
            .set(service)
            .addOnSuccessListener {
                // Update in local list
                val index = servicesList.indexOfFirst { it.id == service.id }
                if (index >= 0) {
                    servicesList[index] = service
                    servicesList.sortBy { it.name }
                    adapter.notifyDataSetChanged()
                }
                
                Toast.makeText(context, "Service updated successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating service", e)
                Toast.makeText(context, "Error updating service: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    
    private fun showDeleteServiceDialog(service: Service) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Service")
            .setMessage("Are you sure you want to delete the service '${service.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteService(service)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
    
    private fun deleteService(service: Service) {
        showLoading(true)
        
        // First check if there are any bookings using this service
        db.collection("bookings")
            .whereEqualTo("service_id", service.id)
            .get()
            .addOnSuccessListener { bookingsResult ->
                if (!bookingsResult.isEmpty) {
                    // There are bookings using this service
                    AlertDialog.Builder(requireContext())
                        .setTitle("Cannot Delete Service")
                        .setMessage("This service has ${bookingsResult.size()} bookings associated with it. Please delete the bookings first.")
                        .setPositiveButton("OK", null)
                        .create()
                        .show()
                    
                    showLoading(false)
                    return@addOnSuccessListener
                }
                
                // No bookings, safe to delete
                db.collection("services").document(service.id)
                    .delete()
                    .addOnSuccessListener {
                        // Remove from local list
                        val index = servicesList.indexOfFirst { it.id == service.id }
                        if (index >= 0) {
                            servicesList.removeAt(index)
                            adapter.notifyDataSetChanged()
                        }
                        
                        Toast.makeText(context, "Service deleted successfully", Toast.LENGTH_SHORT).show()
                        showEmptyState(servicesList.isEmpty())
                        showLoading(false)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error deleting service", e)
                        Toast.makeText(context, "Error deleting service: ${e.message}", Toast.LENGTH_SHORT).show()
                        showLoading(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking bookings for service", e)
                Toast.makeText(context, "Error checking bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    
    private fun showEmptyState(show: Boolean, message: String = "No services found") {
        emptyStateTextView.text = message
        emptyStateTextView.visibility = if (show) View.VISIBLE else View.GONE
        servicesListView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            servicesListView.visibility = View.GONE
            emptyStateTextView.visibility = View.GONE
        }
    }
}