package com.example.project.Admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment to display appointments for a specific user (doctor or patient)
 */
class AdminAppointmentsListFragment : Fragment() {

    private val TAG = "AdminAppointmentsList"
    
    // UI components
    private lateinit var appointmentsRecyclerView: RecyclerView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateTextView: TextView
    private lateinit var titleTextView: TextView
    
    // Data
    private lateinit var appointmentAdapter: AdminAppointmentAdapter
    private val appointmentList = mutableListOf<AdminAppointmentItem>()
    
    // Firebase
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var db: FirebaseFirestore
    
    // Arguments
    private var userId: String = ""
    private var userType: String = "" // "doctor" or "patient"
    private var userName: String = ""

    companion object {
        fun newInstance(userId: String, userType: String, userName: String): AdminAppointmentsListFragment {
            val fragment = AdminAppointmentsListFragment()
            val args = Bundle()
            args.putString("userId", userId)
            args.putString("userType", userType)
            args.putString("userName", userName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("userId", "")
            userType = it.getString("userType", "")
            userName = it.getString("userName", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_appointments_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        firestoreHelper = FirestoreHelper()
        db = firestoreHelper.getDbInstance()
        
        // Initialize UI components
        appointmentsRecyclerView = view.findViewById(R.id.appointmentsListRecyclerView)
        loadingIndicator = view.findViewById(R.id.appointmentsListLoadingIndicator)
        emptyStateTextView = view.findViewById(R.id.appointmentsListEmptyStateTextView)
        titleTextView = view.findViewById(R.id.appointmentsListTitleTextView)
        
        // Set title based on user type
        val title = when (userType) {
            "doctor" -> "Appointments for $userName"
            "patient" -> "Appointments for $userName"
            else -> "Appointments"
        }
        titleTextView.text = title
        
        // Setup RecyclerView
        appointmentAdapter = AdminAppointmentAdapter(
            onEditClick = { appointment -> 
                // Handle edit - you can reuse the edit dialog from AdminAppointmentsFragment
                // or navigate to a dedicated edit fragment
                // For now, we'll just log it
                Log.d(TAG, "Edit appointment: ${appointment.id}")
            },
            onDeleteClick = { appointment -> 
                // Handle delete - you can reuse the delete confirmation dialog 
                // from AdminAppointmentsFragment
                // For now, we'll just log it
                Log.d(TAG, "Delete appointment: ${appointment.id}")
            }
        )
        
        appointmentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = appointmentAdapter
        }
        
        // Load appointments
        loadAppointments()
    }
    
    private fun loadAppointments() {
        showLoading(true)
        
        // Query depends on user type
        val query = when (userType) {
            "doctor" -> db.collection("bookings").whereEqualTo("doctor_id", userId)
            "patient" -> db.collection("bookings").whereEqualTo("patient_name", userId)
            else -> db.collection("bookings") // Fallback to all bookings
        }
        
        query.get()
            .addOnSuccessListener { result ->
                appointmentList.clear()
                
                if (result.isEmpty) {
                    showEmptyState(true, "No appointments found for $userName")
                    showLoading(false)
                    return@addOnSuccessListener
                }
                
                // Process appointments
                for (document in result) {
                    val id = document.id
                    val doctorId = document.getString("doctor_id") ?: ""
                    val patientId = document.getString("patient_name") ?: ""
                    val serviceId = document.getString("service_id") ?: ""
                    val date = document.getString("date") ?: ""
                    val startTime = document.getString("start_time") ?: ""
                    val endTime = document.getString("end_time") ?: ""
                    val status = document.getString("status") ?: "confirmed"
                    
                    // Create a simplified appointment item - we'll load doctor and patient names, and service details separately
                    val appointmentItem = AdminAppointmentItem(
                        id = id,
                        doctorId = doctorId,
                        doctorName = "Loading...", // Will be populated
                        patientId = patientId,
                        patientName = "Loading...", // Will be populated
                        serviceId = serviceId,
                        serviceName = "Loading...", // Will be populated
                        servicePrice = 0, // Will be populated
                        serviceDuration = 0, // Will be populated
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        status = status
                    )
                    
                    appointmentList.add(appointmentItem)
                    
                    // Load doctor name
                    if (doctorId.isNotEmpty()) {
                        db.collection("doctors").document(doctorId).get()
                            .addOnSuccessListener { doctorDoc ->
                                if (doctorDoc.exists()) {
                                    val firstName = doctorDoc.getString("firstName") ?: ""
                                    val lastName = doctorDoc.getString("lastName") ?: ""
                                    val doctorName = "Dr. $firstName $lastName"
                                    
                                    // Update appointment item
                                    val index = appointmentList.indexOfFirst { it.id == id }
                                    if (index >= 0) {
                                        appointmentList[index] = appointmentList[index].copy(doctorName = doctorName)
                                        appointmentAdapter.updateAppointments(appointmentList)
                                    }
                                }
                            }
                    }
                    
                    // Load patient name if it's an email or ID
                    if (patientId.isNotEmpty() && !patientId.contains(" ")) {
                        db.collection("patients").document(patientId).get()
                            .addOnSuccessListener { patientDoc ->
                                if (patientDoc.exists()) {
                                    val firstName = patientDoc.getString("firstName") ?: ""
                                    val lastName = patientDoc.getString("lastName") ?: ""
                                    val patientName = "$firstName $lastName"
                                    
                                    // Update appointment item
                                    val index = appointmentList.indexOfFirst { it.id == id }
                                    if (index >= 0) {
                                        appointmentList[index] = appointmentList[index].copy(patientName = patientName)
                                        appointmentAdapter.updateAppointments(appointmentList)
                                    }
                                } else {
                                    // Try to find by email
                                    db.collection("patients")
                                        .whereEqualTo("email", patientId)
                                        .get()
                                        .addOnSuccessListener { querySnapshot ->
                                            if (!querySnapshot.isEmpty) {
                                                val patientDoc = querySnapshot.documents[0]
                                                val firstName = patientDoc.getString("firstName") ?: ""
                                                val lastName = patientDoc.getString("lastName") ?: ""
                                                val patientName = "$firstName $lastName"
                                                
                                                // Update appointment item
                                                val index = appointmentList.indexOfFirst { it.id == id }
                                                if (index >= 0) {
                                                    appointmentList[index] = appointmentList[index].copy(patientName = patientName)
                                                    appointmentAdapter.updateAppointments(appointmentList)
                                                }
                                            } else {
                                                // If we can't find the patient, just use the ID as name
                                                val index = appointmentList.indexOfFirst { it.id == id }
                                                if (index >= 0) {
                                                    appointmentList[index] = appointmentList[index].copy(patientName = patientId)
                                                    appointmentAdapter.updateAppointments(appointmentList)
                                                }
                                            }
                                        }
                                }
                            }
                    } else {
                        // If it already looks like a name, just use it
                        val index = appointmentList.indexOfFirst { it.id == id }
                        if (index >= 0) {
                            appointmentList[index] = appointmentList[index].copy(patientName = patientId)
                            appointmentAdapter.updateAppointments(appointmentList)
                        }
                    }
                    
                    // Load service details
                    if (serviceId.isNotEmpty()) {
                        db.collection("services").document(serviceId).get()
                            .addOnSuccessListener { serviceDoc ->
                                if (serviceDoc.exists()) {
                                    val serviceName = serviceDoc.getString("name") ?: "Unknown Service"
                                    val servicePrice = serviceDoc.getLong("price")?.toInt() ?: 0
                                    val serviceDuration = serviceDoc.getLong("duration_minutes")?.toInt() ?: 0
                                    
                                    // Update appointment item
                                    val index = appointmentList.indexOfFirst { it.id == id }
                                    if (index >= 0) {
                                        appointmentList[index] = appointmentList[index].copy(
                                            serviceName = serviceName,
                                            servicePrice = servicePrice,
                                            serviceDuration = serviceDuration
                                        )
                                        appointmentAdapter.updateAppointments(appointmentList)
                                    }
                                } else {
                                    // If service not found, use default values
                                    val index = appointmentList.indexOfFirst { it.id == id }
                                    if (index >= 0) {
                                        appointmentList[index] = appointmentList[index].copy(serviceName = "Unknown Service ($serviceId)")
                                        appointmentAdapter.updateAppointments(appointmentList)
                                    }
                                }
                            }
                    }
                }
                
                // Initial update with placeholder data
                appointmentAdapter.updateAppointments(appointmentList)
                showEmptyState(appointmentList.isEmpty())
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading appointments", e)
                showEmptyState(true, "Error loading appointments: ${e.message}")
                showLoading(false)
            }
    }
    
    private fun showEmptyState(show: Boolean, message: String = "No appointments found") {
        emptyStateTextView.text = message
        emptyStateTextView.visibility = if (show) View.VISIBLE else View.GONE
        appointmentsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            appointmentsRecyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.GONE
        }
    }
}