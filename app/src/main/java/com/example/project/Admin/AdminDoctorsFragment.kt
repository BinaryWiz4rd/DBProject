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

/**
 * A fragment for administrators to view, search, sort, add, edit, and delete doctors.
 * It displays a list of doctors in a RecyclerView and provides UI elements for management.
 */
class AdminDoctorsFragment : Fragment() {

    private val TAG = "AdminDoctorsFragment"

    private lateinit var doctorsRecyclerView: RecyclerView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateTextView: TextView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var sortBySpinner: Spinner
    private lateinit var addDoctorFab: FloatingActionButton

    private lateinit var doctorAdapter: AdminDoctorAdapter

    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    /**
     * Defines the available options for sorting the doctor list.
     */
    private enum class SortOption {
        NAME_ASC, NAME_DESC, SPECIALIZATION, EMAIL
    }

    private var currentSortOption = SortOption.NAME_ASC
    private val doctorList = mutableListOf<Doctor>()

    /**
     * Inflates the layout for this fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_doctors, container, false)
    }

    /**
     * Called immediately after `onCreateView()` has returned, but before any saved state has been restored in to the view.
     * This is where initialization of UI components, setting up listeners, and loading data occurs.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestoreHelper = FirestoreHelper()
        db = firestoreHelper.getDbInstance()
        auth = FirebaseAuth.getInstance()

        initializeViews(view)

        setupRecyclerView()

        setupSearch()
        setupSortSpinner()

        setupAddDoctorFab()

        loadDoctors()
    }

    /**
     * Initializes all UI components by finding them in the fragment's view.
     * @param view The root view of the fragment.
     */
    private fun initializeViews(view: View) {
        doctorsRecyclerView = view.findViewById(R.id.doctorsRecyclerView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        searchEditText = view.findViewById(R.id.searchEditText)
        sortBySpinner = view.findViewById(R.id.sortBySpinner)
        addDoctorFab = view.findViewById(R.id.addDoctorFab)
    }

    /**
     * Sets up the RecyclerView, including its layout manager and adapter.
     * The adapter is configured with lambdas for handling clicks on edit, delete, view appointments, and view services actions.
     */
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

    /**
     * Configures the search functionality by adding a TextWatcher to the search EditText.
     * The list of doctors is filtered in real-time as the user types.
     */
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                doctorAdapter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Populates the sort-by spinner with options and sets up a listener to handle selections.
     * When a new sort option is selected, the list is re-sorted.
     */
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

    /**
     * Sets up the click listener for the FloatingActionButton to navigate to the [AdminCreateDoctorFragment].
     */
    private fun setupAddDoctorFab() {
        addDoctorFab.setOnClickListener {
            val activity = requireActivity() as MainAdminActivity
            activity.navigateToFragment(AdminCreateDoctorFragment())
        }
    }

    /**
     * Fetches the list of doctors from the Firestore "doctors" collection.
     * It handles success and failure cases, updates the UI to show a loading indicator,
     * and displays an empty state message if no doctors are found or an error occurs.
     */
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

    /**
     * Sorts the local `doctorList` based on the `currentSortOption`
     * and updates the adapter with the sorted list.
     */
    private fun sortDoctors() {
        val sortedList = when (currentSortOption) {
            SortOption.NAME_ASC -> doctorList.sortedWith(compareBy { it.firstName + " " + it.lastName })
            SortOption.NAME_DESC -> doctorList.sortedWith(compareByDescending { it.firstName + " " + it.lastName })
            SortOption.SPECIALIZATION -> doctorList.sortedBy { it.specialization }
            SortOption.EMAIL -> doctorList.sortedBy { it.email }
        }

        doctorAdapter.updateDoctors(sortedList)
    }

    /**
     * Shows or hides the loading indicator and adjusts the visibility of other views accordingly.
     * @param show True to show the loading indicator, false to hide it.
     */
    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            doctorsRecyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.GONE
        }
    }

    /**
     * Shows or hides the empty state view.
     * @param show True to show the empty state message, false to hide it.
     * @param message The message to display in the empty state TextView.
     */
    private fun showEmptyState(show: Boolean, message: String = "No doctors found") {
        emptyStateTextView.text = message
        emptyStateTextView.visibility = if (show) View.VISIBLE else View.GONE
        doctorsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    /**
     * Displays a dialog for editing a doctor's details.
     * It pre-fills the input fields with the doctor's current data and includes input validation.
     * @param doctor The [Doctor] object to be edited.
     */
    private fun showEditDoctorDialog(doctor: Doctor) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_doctor, null)

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

        firstNameEditText.setText(doctor.firstName)
        lastNameEditText.setText(doctor.lastName)
        emailEditText.setText(doctor.email)
        specializationEditText.setText(doctor.specialization)
        pwzNumberEditText.setText(doctor.pwzNumber)
        phoneNumberEditText.setText(doctor.phoneNumber)

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
            specializationLayout.error = null
            pwzNumberLayout.error = null
            phoneNumberLayout.error = null

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

                updateDoctor(updatedDoctor, dialog)
            }
        }
    }

    /**
     * Updates a doctor's information in Firestore.
     * @param doctor The [Doctor] object with the updated information.
     * @param dialog The [AlertDialog] that is currently open, to be dismissed on success.
     */
    private fun updateDoctor(doctor: Doctor, dialog: AlertDialog) {
        showLoading(true)

        val updates = mapOf(
            "firstName" to doctor.firstName,
            "lastName" to doctor.lastName,
            "email" to doctor.email,
            "specialization" to doctor.specialization,
            "pwzNumber" to doctor.pwzNumber,
            "phoneNumber" to doctor.phoneNumber
        )

        db.collection("doctors")
            .document(doctor.uid)
            .update(updates)
            .addOnSuccessListener {
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

    /**
     * Shows a confirmation dialog before deleting a doctor.
     * @param doctor The [Doctor] to be deleted.
     */
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

    /**
     * Deletes a doctor and all their associated data from Firestore using a batch write for atomicity.
     * This includes their services, bookings, settings, and calendar entries.
     * @param doctor The [Doctor] to be deleted.
     */
    private fun deleteDoctor(doctor: Doctor) {
        showLoading(true)

        db.collection("services")
            .whereEqualTo("doctor_id", doctor.uid)
            .get()
            .addOnSuccessListener { servicesResult ->
                val batch = db.batch()

                for (document in servicesResult) {
                    batch.delete(db.collection("services").document(document.id))
                }

                db.collection("bookings")
                    .whereEqualTo("doctor_id", doctor.uid)
                    .get()
                    .addOnSuccessListener { bookingsResult ->
                        for (document in bookingsResult) {
                            batch.delete(db.collection("bookings").document(document.id))
                        }

                        batch.delete(db.collection("doctorSettings").document(doctor.uid))
                        batch.delete(db.collection("doctorCalendars").document(doctor.uid))
                        batch.delete(db.collection("doctors").document(doctor.uid))

                        batch.commit()
                            .addOnSuccessListener {
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

    /**
     * Navigates to the [AdminAppointmentsListFragment] to display appointments for the selected doctor.
     * @param doctor The doctor whose appointments are to be viewed.
     */
    private fun navigateToAppointmentsView(doctor: Doctor) {
        val appointmentsFragment = AdminAppointmentsListFragment.newInstance(
            userId = doctor.uid,
            userType = "doctor",
            userName = "Dr. ${doctor.firstName} ${doctor.lastName}"
        )

        val activity = requireActivity() as MainAdminActivity
        activity.navigateToFragment(appointmentsFragment)
    }

    /**
     * Navigates to the [AdminServicesFragment] to display services offered by the selected doctor.
     * @param doctor The doctor whose services are to be viewed.
     */
    private fun navigateToServicesView(doctor: Doctor) {
        val servicesFragment = AdminServicesFragment.newInstance(doctorId = doctor.uid)

        val activity = requireActivity() as MainAdminActivity
        activity.navigateToFragment(servicesFragment)
    }
}