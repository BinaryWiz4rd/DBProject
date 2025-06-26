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

    private lateinit var patientsRecyclerView: RecyclerView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateTextView: TextView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var sortBySpinner: Spinner
    private lateinit var addPatientFab: FloatingActionButton

    private lateinit var patientAdapter: AdminPatientAdapter

    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

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

        firestoreHelper = FirestoreHelper()
        db = firestoreHelper.getDbInstance()
        auth = FirebaseAuth.getInstance()

        initializeViews(view)

        setupRecyclerView()

        setupSearch()
        setupSortSpinner()

        setupAddPatientFab()

        loadPatients()
    }

    /**
     * Initializes UI components by finding their respective views in the layout.
     * @param view The parent view containing the UI components.
     */
    private fun initializeViews(view: View) {
        patientsRecyclerView = view.findViewById(R.id.patientsRecyclerView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        searchEditText = view.findViewById(R.id.searchEditText)
        sortBySpinner = view.findViewById(R.id.sortBySpinner)
        addPatientFab = view.findViewById(R.id.addPatientFab)
    }

    /**
     * Sets up the RecyclerView with its adapter and layout manager.
     * It also defines the click listeners for edit, delete, and view appointments actions.
     */
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

    /**
     * Sets up the text change listener for the search EditText.
     * Filters the patient list in the adapter based on the search query.
     */
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                patientAdapter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Configures the sort options spinner.
     * When a sort option is selected, it updates the [currentSortOption] and re-sorts the patient list.
     */
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

    /**
     * Sets up the click listener for the Floating Action Button (FAB) to add a new patient.
     * When clicked, it displays the add patient dialog.
     */
    private fun setupAddPatientFab() {
        addPatientFab.setOnClickListener {
            showAddPatientDialog()
        }
    }

    /**
     * Loads patient data from Firestore.
     * Displays a loading indicator while fetching data and handles success and failure cases.
     * Updates the UI based on whether patients are found.
     */
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

    /**
     * Sorts the [patientList] based on the [currentSortOption] and updates the adapter.
     */
    private fun sortPatients() {
        val sortedList = when (currentSortOption) {
            SortOption.NAME_ASC -> patientList.sortedWith(compareBy { it.firstName + " " + it.lastName })
            SortOption.NAME_DESC -> patientList.sortedWith(compareByDescending { it.firstName + " " + it.lastName })
            SortOption.EMAIL -> patientList.sortedBy { it.email }
            SortOption.DOB -> patientList.sortedBy { it.dateOfBirth }
        }

        patientAdapter.updatePatients(sortedList)
    }

    /**
     * Shows or hides the loading indicator and adjusts the visibility of the RecyclerView and empty state text.
     * @param show A boolean indicating whether to show the loading indicator.
     */
    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            patientsRecyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.GONE
        }
    }

    /**
     * Shows or hides the empty state message.
     * @param show A boolean indicating whether to show the empty state message.
     * @param message The message to display when the state is empty. Defaults to "No patients found".
     */
    private fun showEmptyState(show: Boolean, message: String = "No patients found") {
        emptyStateTextView.text = message
        emptyStateTextView.visibility = if (show) View.VISIBLE else View.GONE
        patientsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    /**
     * Displays a dialog for adding a new patient.
     * Includes input fields for patient details and permission checkboxes.
     * Validates input and saves the new patient to Firestore upon successful validation.
     */
    private fun showAddPatientDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_patient, null)

        dialogView.findViewById<TextView>(R.id.dialogTitleTextView).text = "Add New Patient"

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

        setupDatePicker(dobEditText)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            firstNameLayout.error = null
            lastNameLayout.error = null
            emailLayout.error = null

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
                val newPatient = Patient(
                    uid = "",
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    dateOfBirth = dob,
                    role = "patient",
                    add = addPermissionCheckbox.isChecked,
                    edit = editPermissionCheckbox.isChecked,
                    delete = deletePermissionCheckbox.isChecked
                )

                saveNewPatient(newPatient, dialog)
            }
        }
    }

    /**
     * Saves a new patient to Firestore.
     * Updates the patient's UID with the document ID generated by Firestore.
     * Handles success and failure of the save operation and updates the UI accordingly.
     * @param patient The [Patient] object to be saved.
     * @param dialog The [AlertDialog] to dismiss after saving.
     */
    private fun saveNewPatient(patient: Patient, dialog: AlertDialog) {
        showLoading(true)

        db.collection("patients")
            .add(patient)
            .addOnSuccessListener { documentReference ->
                db.collection("patients")
                    .document(documentReference.id)
                    .update("uid", documentReference.id)
                    .addOnSuccessListener {
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

    /**
     * Displays a dialog for editing an existing patient's details.
     * Populates the input fields with the patient's current information.
     * Validates input and updates the patient in Firestore upon successful validation.
     * @param patient The [Patient] object to be edited.
     */
    private fun showEditPatientDialog(patient: Patient) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_patient, null)

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

        firstNameEditText.setText(patient.firstName)
        lastNameEditText.setText(patient.lastName)
        emailEditText.setText(patient.email)
        dobEditText.setText(patient.dateOfBirth)
        addPermissionCheckbox.isChecked = patient.add ?: false
        editPermissionCheckbox.isChecked = patient.edit ?: false
        deletePermissionCheckbox.isChecked = patient.delete ?: false

        setupDatePicker(dobEditText)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            firstNameLayout.error = null
            lastNameLayout.error = null
            emailLayout.error = null

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

                updatePatient(updatedPatient, dialog)
            }
        }
    }

    /**
     * Updates an existing patient's details in Firestore.
     * Handles success and failure of the update operation and updates the local list and UI.
     * @param patient The [Patient] object with updated information.
     * @param dialog The [AlertDialog] to dismiss after updating.
     */
    private fun updatePatient(patient: Patient, dialog: AlertDialog) {
        showLoading(true)

        val updates = mapOf(
            "firstName" to patient.firstName,
            "lastName" to patient.lastName,
            "email" to patient.email,
            "dateOfBirth" to patient.dateOfBirth,
            "add" to patient.add,
            "edit" to patient.edit,
            "delete" to patient.delete
        )

        db.collection("patients")
            .document(patient.uid)
            .update(updates)
            .addOnSuccessListener {
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

    /**
     * Displays a confirmation dialog before deleting a patient.
     * If confirmed, calls [deletePatient] to remove the patient from Firestore.
     * @param patient The [Patient] object to be deleted.
     */
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

    /**
     * Deletes a patient from Firestore.
     * Handles success and failure of the delete operation and updates the local list and UI.
     * @param patient The [Patient] object to be deleted.
     */
    private fun deletePatient(patient: Patient) {
        showLoading(true)

        db.collection("patients")
            .document(patient.uid)
            .delete()
            .addOnSuccessListener {
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

    /**
     * Handles the action to view a patient's appointments.
     * Currently displays a Toast message and an AlertDialog as a placeholder.
     * In a full implementation, this would navigate to a dedicated appointments screen.
     * @param patient The [Patient] whose appointments are to be viewed.
     */
    private fun viewPatientAppointments(patient: Patient) {
        Toast.makeText(context, "Viewing appointments for ${patient.firstName} ${patient.lastName}", Toast.LENGTH_SHORT).show()

        AlertDialog.Builder(requireContext())
            .setTitle("Patient Appointments")
            .setMessage("This would show all appointments for ${patient.firstName} ${patient.lastName}.")
            .setPositiveButton("OK", null)
            .create()
            .show()
    }

    /**
     * Sets up a DatePickerDialog for the given TextInputEditText.
     * When the EditText is clicked, a date picker dialog appears, allowing the user to select a date.
     * The selected date is then formatted and set as the text of the EditText.
     * @param dobEditText The TextInputEditText to attach the date picker to.
     */
    private fun setupDatePicker(dobEditText: TextInputEditText) {
        dobEditText.setOnClickListener {
            val calendar = Calendar.getInstance()

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