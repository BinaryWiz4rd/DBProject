package com.example.project.Admin

import android.app.AlertDialog
import android.app.DatePickerDialog
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
import com.example.project.Admin.Patient
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminPatientsFragment : Fragment() {

    private val TAG = "AdminPatientsFragment"
    
    // UI components
    private lateinit var patientsRecyclerView: RecyclerView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateTextView: TextView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var sortBySpinner: Spinner
    private lateinit var addPatientFab: FloatingActionButton
    
    // Adapter
    private lateinit var patientAdapter: AdminPatientAdapter
    
    // Firebase
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    // Sort options enum
    private enum class SortOption {
        NAME_ASC, NAME_DESC, EMAIL, DOB
    }
    
    private var currentSortOption = SortOption.NAME_ASC
    private val patientList = mutableListOf<Patient>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_patients, container, false)
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
        
        // Setup FAB for adding new patients
        setupAddPatientFab()
        
        // Load patient data
        loadPatients()
    }

    private fun initializeViews(view: View) {
        patientsRecyclerView = view.findViewById(R.id.patientsRecyclerView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        searchEditText = view.findViewById(R.id.searchEditText)
        sortBySpinner = view.findViewById(R.id.sortBySpinner)
        addPatientFab = view.findViewById(R.id.addPatientFab)
    }

    private fun setupRecyclerView() {
        patientAdapter = AdminPatientAdapter(
            onEditClick = { patient -> showEditPatientDialog(patient) },
            onDeleteClick = { patient -> showDeleteConfirmationDialog(patient) },
            onViewAppointmentsClick = { patient -> viewPatientAppointments(patient) }
        )
        
        patientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = patientAdapter
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                patientAdapter.filter(s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Name (A-Z)", "Name (Z-A)", "Email", "Date of Birth")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        sortBySpinner.adapter = adapter
        sortBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortOption = when (position) {
                    0 -> SortOption.NAME_ASC
                    1 -> SortOption.NAME_DESC
                    2 -> SortOption.EMAIL
                    3 -> SortOption.DOB
                    else -> SortOption.NAME_ASC
                }
                sortPatients()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentSortOption = SortOption.NAME_ASC
            }
        }
    }

    private fun setupAddPatientFab() {
        addPatientFab.setOnClickListener {
            showAddPatientDialog()
        }
    }

    private fun loadPatients() {
        showLoading(true)
        
        db.collection("patients")
            .get()
            .addOnSuccessListener { result ->
                patientList.clear()
                
                for (document in result) {
                    try {
                        val patient = Patient(
                            uid = document.id,
                            email = document.getString("email") ?: "",
                            firstName = document.getString("firstName") ?: "",
                            lastName = document.getString("lastName") ?: "",
                            dateOfBirth = document.getString("dateOfBirth") ?: "",
                            role = document.getString("role") ?: "patient",
                            add = document.getBoolean("add") ?: false,
                            edit = document.getBoolean("edit") ?: false,
                            delete = document.getBoolean("delete") ?: false
                        )
                        patientList.add(patient)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing patient document: ${e.message}")
                    }
                }
                
                sortPatients()
                showLoading(false)
                showEmptyState(patientList.isEmpty())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading patients", e)
                showLoading(false)
                showEmptyState(true, "Error loading patients: ${e.message}")
            }
    }

    private fun sortPatients() {
        val sortedList = when (currentSortOption) {
            SortOption.NAME_ASC -> patientList.sortedWith(compareBy { it.firstName + " " + it.lastName })
            SortOption.NAME_DESC -> patientList.sortedWith(compareByDescending { it.firstName + " " + it.lastName })
            SortOption.EMAIL -> patientList.sortedBy { it.email }
            SortOption.DOB -> patientList.sortedBy { it.dateOfBirth }
        }
        
        patientAdapter.updatePatients(sortedList)
    }

    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            patientsRecyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.GONE
        }
    }

    private fun showEmptyState(show: Boolean, message: String = "No patients found") {
        emptyStateTextView.text = message
        emptyStateTextView.visibility = if (show) View.VISIBLE else View.GONE
        patientsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showAddPatientDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_patient, null)
        
        // Update dialog title
        dialogView.findViewById<TextView>(R.id.dialogTitleTextView).text = "Add New Patient"
        
        // Initialize dialog components
        val firstNameEditText = dialogView.findViewById<TextInputEditText>(R.id.firstNameEditText)
        val lastNameEditText = dialogView.findViewById<TextInputEditText>(R.id.lastNameEditText)
        val emailEditText = dialogView.findViewById<TextInputEditText>(R.id.emailEditText)
        val dobEditText = dialogView.findViewById<TextInputEditText>(R.id.dobEditText)
        val addPermissionCheckbox = dialogView.findViewById<CheckBox>(R.id.addPermissionCheckbox)
        val editPermissionCheckbox = dialogView.findViewById<CheckBox>(R.id.editPermissionCheckbox)
        val deletePermissionCheckbox = dialogView.findViewById<CheckBox>(R.id.deletePermissionCheckbox)
        
        val firstNameLayout = dialogView.findViewById<TextInputLayout>(R.id.firstNameLayout)
        val lastNameLayout = dialogView.findViewById<TextInputLayout>(R.id.lastNameLayout)
        val emailLayout = dialogView.findViewById<TextInputLayout>(R.id.emailLayout)
        
        // Setup date picker
        setupDatePicker(dobEditText)
        
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
            
            val dob = dobEditText.text.toString().trim()
            
            if (isValid) {
                // Create new patient object
                val newPatient = Patient(
                    uid = "", // Will be set by Firestore
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    dateOfBirth = dob,
                    role = "patient",
                    add = addPermissionCheckbox.isChecked,
                    edit = editPermissionCheckbox.isChecked,
                    delete = deletePermissionCheckbox.isChecked
                )
                
                // Save to Firestore
                saveNewPatient(newPatient, dialog)
            }
        }
    }

    private fun saveNewPatient(patient: Patient, dialog: AlertDialog) {
        showLoading(true)
        
        // Create a new document in the patients collection
        db.collection("patients")
            .add(patient)
            .addOnSuccessListener { documentReference ->
                // Update the patient's uid with the Firestore document ID
                db.collection("patients")
                    .document(documentReference.id)
                    .update("uid", documentReference.id)
                    .addOnSuccessListener {
                        // Add the new patient to our list with the generated ID
                        val newPatient = patient.copy(uid = documentReference.id)
                        patientList.add(newPatient)
                        sortPatients()
                        
                        Toast.makeText(context, "Patient added successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        showLoading(false)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating patient ID", e)
                        showLoading(false)
                        Toast.makeText(context, "Error updating patient ID: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding patient", e)
                showLoading(false)
                Toast.makeText(context, "Error adding patient: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditPatientDialog(patient: Patient) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_patient, null)
        
        // Initialize dialog components
        val firstNameEditText = dialogView.findViewById<TextInputEditText>(R.id.firstNameEditText)
        val lastNameEditText = dialogView.findViewById<TextInputEditText>(R.id.lastNameEditText)
        val emailEditText = dialogView.findViewById<TextInputEditText>(R.id.emailEditText)
        val dobEditText = dialogView.findViewById<TextInputEditText>(R.id.dobEditText)
        val addPermissionCheckbox = dialogView.findViewById<CheckBox>(R.id.addPermissionCheckbox)
        val editPermissionCheckbox = dialogView.findViewById<CheckBox>(R.id.editPermissionCheckbox)
        val deletePermissionCheckbox = dialogView.findViewById<CheckBox>(R.id.deletePermissionCheckbox)
        
        val firstNameLayout = dialogView.findViewById<TextInputLayout>(R.id.firstNameLayout)
        val lastNameLayout = dialogView.findViewById<TextInputLayout>(R.id.lastNameLayout)
        val emailLayout = dialogView.findViewById<TextInputLayout>(R.id.emailLayout)
        
        // Set current values
        firstNameEditText.setText(patient.firstName)
        lastNameEditText.setText(patient.lastName)
        emailEditText.setText(patient.email)
        dobEditText.setText(patient.dateOfBirth)
        addPermissionCheckbox.isChecked = patient.add ?: false
        editPermissionCheckbox.isChecked = patient.edit ?: false
        deletePermissionCheckbox.isChecked = patient.delete ?: false
        
        // Setup date picker
        setupDatePicker(dobEditText)
        
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
            
            val dob = dobEditText.text.toString().trim()
            
            if (isValid) {
                // Update patient object
                val updatedPatient = Patient(
                    uid = patient.uid,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    dateOfBirth = dob,
                    role = patient.role,
                    add = addPermissionCheckbox.isChecked,
                    edit = editPermissionCheckbox.isChecked,
                    delete = deletePermissionCheckbox.isChecked
                )
                
                // Update in Firestore
                updatePatient(updatedPatient, dialog)
            }
        }
    }

    private fun updatePatient(patient: Patient, dialog: AlertDialog) {
        showLoading(true)
        
        // Create a map with the fields to update
        val updates = mapOf(
            "firstName" to patient.firstName,
            "lastName" to patient.lastName,
            "email" to patient.email,
            "dateOfBirth" to patient.dateOfBirth,
            "add" to patient.add,
            "edit" to patient.edit,
            "delete" to patient.delete
        )
        
        // Update the patient document in Firestore
        db.collection("patients")
            .document(patient.uid)
            .update(updates)
            .addOnSuccessListener {
                // Update our local list
                val index = patientList.indexOfFirst { it.uid == patient.uid }
                if (index >= 0) {
                    patientList[index] = patient
                    sortPatients()
                }
                
                Toast.makeText(context, "Patient updated successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating patient", e)
                showLoading(false)
                Toast.makeText(context, "Error updating patient: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(patient: Patient) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Patient")
            .setMessage("Are you sure you want to delete ${patient.firstName} ${patient.lastName}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePatient(patient)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun deletePatient(patient: Patient) {
        showLoading(true)
        
        db.collection("patients")
            .document(patient.uid)
            .delete()
            .addOnSuccessListener {
                // Remove from our local list
                val index = patientList.indexOfFirst { it.uid == patient.uid }
                if (index >= 0) {
                    patientList.removeAt(index)
                    sortPatients()
                }
                
                Toast.makeText(context, "Patient deleted successfully", Toast.LENGTH_SHORT).show()
                showLoading(false)
                showEmptyState(patientList.isEmpty())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting patient", e)
                showLoading(false)
                Toast.makeText(context, "Error deleting patient: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun viewPatientAppointments(patient: Patient) {
        // Navigate to a screen showing this patient's appointments
        Toast.makeText(context, "Viewing appointments for ${patient.firstName} ${patient.lastName}", Toast.LENGTH_SHORT).show()
        
        // This would typically navigate to another fragment or activity showing the patient's appointments
        // For now, we'll just show a dialog with a message
        AlertDialog.Builder(requireContext())
            .setTitle("Patient Appointments")
            .setMessage("This would show all appointments for ${patient.firstName} ${patient.lastName}.")
            .setPositiveButton("OK", null)
            .create()
            .show()
    }

    private fun setupDatePicker(dobEditText: TextInputEditText) {
        dobEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            
            // Parse existing date if present
            if (dobEditText.text?.isNotEmpty() == true) {
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = dateFormat.parse(dobEditText.text.toString())
                    if (date != null) {
                        calendar.time = date
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date", e)
                }
            }
            
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dobEditText.setText(dateFormat.format(selectedDate.time))
            }, year, month, day).show()
        }
    }
}