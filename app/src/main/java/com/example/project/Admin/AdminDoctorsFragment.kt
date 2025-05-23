package com.example.project.Admin

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.doctor.model.Doctor
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDoctorsFragment : Fragment() {

    private val TAG = "AdminDoctorsFragment"
    
    // UI components
    private lateinit var doctorsRecyclerView: RecyclerView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateTextView: TextView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var sortBySpinner: Spinner
    private lateinit var addDoctorFab: FloatingActionButton
    
    // Adapter
    private lateinit var doctorAdapter: AdminDoctorAdapter
    
    // Firebase
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    // Sort options enum
    private enum class SortOption {
        NAME_ASC, NAME_DESC, SPECIALIZATION, EMAIL
    }
    
    private var currentSortOption = SortOption.NAME_ASC
    private val doctorList = mutableListOf<Doctor>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_doctors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        firestoreHelper = FirestoreHelper()
        db = firestoreHelper.getDbInstance()
        auth = FirebaseAuth.getInstance()
        
        // Initialize UI components
        initializeViews(view)
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup search and sort
        setupSearch()
        setupSortSpinner()
        
        // Setup FAB for redirect to add doctor fragment
        setupAddDoctorFab()
        
        // Load doctor data
        loadDoctors()
    }

    private fun initializeViews(view: View) {
        doctorsRecyclerView = view.findViewById(R.id.doctorsRecyclerView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        searchEditText = view.findViewById(R.id.searchEditText)
        sortBySpinner = view.findViewById(R.id.sortBySpinner)
        addDoctorFab = view.findViewById(R.id.addDoctorFab)
    }

    private fun setupRecyclerView() {
        doctorAdapter = AdminDoctorAdapter(
            onEditClick = { doctor -> showEditDoctorDialog(doctor) },
            onDeleteClick = { doctor -> showDeleteConfirmationDialog(doctor) },
            onViewAppointmentsClick = { doctor -> navigateToAppointmentsView(doctor) },
            onViewServicesClick = { doctor -> navigateToServicesView(doctor) }
        )
        
        doctorsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = doctorAdapter
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                doctorAdapter.filter(s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Name (A-Z)", "Name (Z-A)", "Specialization", "Email")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        sortBySpinner.adapter = adapter
        sortBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortOption = when (position) {
                    0 -> SortOption.NAME_ASC
                    1 -> SortOption.NAME_DESC
                    2 -> SortOption.SPECIALIZATION
                    3 -> SortOption.EMAIL
                    else -> SortOption.NAME_ASC
                }
                sortDoctors()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentSortOption = SortOption.NAME_ASC
            }
        }
    }

    private fun setupAddDoctorFab() {
        addDoctorFab.setOnClickListener {
            // Navigate to AdminCreateDoctorFragment
            val activity = requireActivity() as MainAdminActivity
            activity.navigateToFragment(AdminCreateDoctorFragment())
        }
    }

    private fun loadDoctors() {
        showLoading(true)
        
        db.collection("doctors")
            .get()
            .addOnSuccessListener { result ->
                doctorList.clear()
                
                for (document in result) {
                    try {
                        val doctor = Doctor(
                            uid = document.id,
                            firstName = document.getString("firstName") ?: "",
                            lastName = document.getString("lastName") ?: "",
                            email = document.getString("email") ?: "",
                            specialization = document.getString("specialization") ?: "",
                            pwzNumber = document.getString("pwzNumber") ?: "",
                            phoneNumber = document.getString("phoneNumber") ?: "",
                            role = document.getString("role") ?: "doctor"
                        )
                        doctorList.add(doctor)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing doctor document: ${e.message}")
                    }
                }
                
                sortDoctors()
                showLoading(false)
                showEmptyState(doctorList.isEmpty())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading doctors", e)
                showLoading(false)
                showEmptyState(true, "Error loading doctors: ${e.message}")
            }
    }

    private fun sortDoctors() {
        val sortedList = when (currentSortOption) {
            SortOption.NAME_ASC -> doctorList.sortedWith(compareBy { it.firstName + " " + it.lastName })
            SortOption.NAME_DESC -> doctorList.sortedWith(compareByDescending { it.firstName + " " + it.lastName })
            SortOption.SPECIALIZATION -> doctorList.sortedBy { it.specialization }
            SortOption.EMAIL -> doctorList.sortedBy { it.email }
        }
        
        doctorAdapter.updateDoctors(sortedList)
    }

    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            doctorsRecyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.GONE
        }
    }

    private fun showEmptyState(show: Boolean, message: String = "No doctors found") {
        emptyStateTextView.text = message
        emptyStateTextView.visibility = if (show) View.VISIBLE else View.GONE
        doctorsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEditDoctorDialog(doctor: Doctor) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_doctor, null)
        
        // Initialize dialog components
        val firstNameEditText = dialogView.findViewById<TextInputEditText>(R.id.firstNameEditText)
        val lastNameEditText = dialogView.findViewById<TextInputEditText>(R.id.lastNameEditText)
        val emailEditText = dialogView.findViewById<TextInputEditText>(R.id.emailEditText)
        val specializationEditText = dialogView.findViewById<TextInputEditText>(R.id.specializationEditText)
        val pwzNumberEditText = dialogView.findViewById<TextInputEditText>(R.id.pwzNumberEditText)
        val phoneNumberEditText = dialogView.findViewById<TextInputEditText>(R.id.phoneNumberEditText)
        
        val firstNameLayout = dialogView.findViewById<TextInputLayout>(R.id.firstNameLayout)
        val lastNameLayout = dialogView.findViewById<TextInputLayout>(R.id.lastNameLayout)
        val emailLayout = dialogView.findViewById<TextInputLayout>(R.id.emailLayout)
        val specializationLayout = dialogView.findViewById<TextInputLayout>(R.id.specializationLayout)
        val pwzNumberLayout = dialogView.findViewById<TextInputLayout>(R.id.pwzNumberLayout)
        val phoneNumberLayout = dialogView.findViewById<TextInputLayout>(R.id.phoneNumberLayout)
        
        // Set current values
        firstNameEditText.setText(doctor.firstName)
        lastNameEditText.setText(doctor.lastName)
        emailEditText.setText(doctor.email)
        specializationEditText.setText(doctor.specialization)
        pwzNumberEditText.setText(doctor.pwzNumber)
        phoneNumberEditText.setText(doctor.phoneNumber)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save", null) // We'll override this later
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Override the positive button to handle validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            // Reset error messages
            firstNameLayout.error = null
            lastNameLayout.error = null
            emailLayout.error = null
            specializationLayout.error = null
            pwzNumberLayout.error = null
            phoneNumberLayout.error = null
            
            // Validate inputs
            var isValid = true
            
            val firstName = firstNameEditText.text.toString().trim()
            if (firstName.isEmpty()) {
                firstNameLayout.error = "First name is required"
                isValid = false
            }
            
            val lastName = lastNameEditText.text.toString().trim()
            if (lastName.isEmpty()) {
                lastNameLayout.error = "Last name is required"
                isValid = false
            }
            
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                emailLayout.error = "Email is required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Please enter a valid email address"
                isValid = false
            }
            
            val specialization = specializationEditText.text.toString().trim()
            if (specialization.isEmpty()) {
                specializationLayout.error = "Specialization is required"
                isValid = false
            }
            
            val pwzNumber = pwzNumberEditText.text.toString().trim()
            if (pwzNumber.isEmpty()) {
                pwzNumberLayout.error = "PWZ number is required"
                isValid = false
            } else if (pwzNumber.length < 5) {
                pwzNumberLayout.error = "Please enter a valid PWZ number"
                isValid = false
            }
            
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            if (phoneNumber.isEmpty()) {
                phoneNumberLayout.error = "Phone number is required"
                isValid = false
            }
            
            if (isValid) {
                // Update doctor object
                val updatedDoctor = Doctor(
                    uid = doctor.uid,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    specialization = specialization,
                    pwzNumber = pwzNumber,
                    phoneNumber = phoneNumber,
                    role = doctor.role
                )
                
                // Update in Firestore
                updateDoctor(updatedDoctor, dialog)
            }
        }
    }

    private fun updateDoctor(doctor: Doctor, dialog: AlertDialog) {
        showLoading(true)
        
        // Create a map with the fields to update
        val updates = mapOf(
            "firstName" to doctor.firstName,
            "lastName" to doctor.lastName,
            "email" to doctor.email,
            "specialization" to doctor.specialization,
            "pwzNumber" to doctor.pwzNumber,
            "phoneNumber" to doctor.phoneNumber
        )
        
        // Update the doctor document in Firestore
        db.collection("doctors")
            .document(doctor.uid)
            .update(updates)
            .addOnSuccessListener {
                // Update our local list
                val index = doctorList.indexOfFirst { it.uid == doctor.uid }
                if (index >= 0) {
                    doctorList[index] = doctor
                    sortDoctors()
                }
                
                Toast.makeText(context, "Doctor updated successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating doctor", e)
                showLoading(false)
                Toast.makeText(context, "Error updating doctor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(doctor: Doctor) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Doctor")
            .setMessage("Are you sure you want to delete Dr. ${doctor.firstName} ${doctor.lastName}? All associated data (appointments, services) will also be deleted. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteDoctor(doctor)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun deleteDoctor(doctor: Doctor) {
        showLoading(true)
        
        // First, get all the doctor's services to delete them
        db.collection("services")
            .whereEqualTo("doctor_id", doctor.uid)
            .get()
            .addOnSuccessListener { servicesResult ->
                // Start a batch to delete multiple documents
                val batch = db.batch()
                
                // Add service deletion to batch
                for (document in servicesResult) {
                    batch.delete(db.collection("services").document(document.id))
                }
                
                // Get and delete all the doctor's appointments
                db.collection("bookings")
                    .whereEqualTo("doctor_id", doctor.uid)
                    .get()
                    .addOnSuccessListener { bookingsResult ->
                        // Add booking deletion to batch
                        for (document in bookingsResult) {
                            batch.delete(db.collection("bookings").document(document.id))
                        }
                        
                        // Delete the doctor's settings
                        batch.delete(db.collection("doctorSettings").document(doctor.uid))
                        
                        // Delete the doctor's calendar
                        // Note: This is a simplification. In a real app, you'd need to recursively delete 
                        // all documents in the doctor's calendar subcollections
                        batch.delete(db.collection("doctorCalendars").document(doctor.uid))
                        
                        // Finally, delete the doctor document
                        batch.delete(db.collection("doctors").document(doctor.uid))
                        
                        // Commit the batch
                        batch.commit()
                            .addOnSuccessListener {
                                // Remove from our local list
                                val index = doctorList.indexOfFirst { it.uid == doctor.uid }
                                if (index >= 0) {
                                    doctorList.removeAt(index)
                                    sortDoctors()
                                }
                                
                                Toast.makeText(context, "Doctor deleted successfully", Toast.LENGTH_SHORT).show()
                                showLoading(false)
                                showEmptyState(doctorList.isEmpty())
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error in batch deletion", e)
                                showLoading(false)
                                Toast.makeText(context, "Error deleting doctor data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting doctor's bookings", e)
                        showLoading(false)
                        Toast.makeText(context, "Error getting doctor's bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting doctor's services", e)
                showLoading(false)
                Toast.makeText(context, "Error getting doctor's services: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToAppointmentsView(doctor: Doctor) {
        val appointmentsFragment = AdminAppointmentsListFragment.newInstance(
            userId = doctor.uid,
            userType = "doctor",
            userName = "Dr. ${doctor.firstName} ${doctor.lastName}"
        )
        
        val activity = requireActivity() as MainAdminActivity
        activity.navigateToFragment(appointmentsFragment)
    }

    private fun navigateToServicesView(doctor: Doctor) {
        val servicesFragment = AdminServicesFragment.newInstance(doctorId = doctor.uid)
        
        val activity = requireActivity() as MainAdminActivity
        activity.navigateToFragment(servicesFragment)
    }
}