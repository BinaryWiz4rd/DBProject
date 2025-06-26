package com.example.project.Patient

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.project.Menu.LogIn
import com.example.project.R
import com.example.project.databinding.FragmentPatientProfileBinding
import com.example.project.util.FirestoreHelper
import com.example.project.Admin.Patient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A [Fragment] subclass that displays the patient's profile information.
 * It allows patients to view their details, edit their profile, log out,
 * and delete their account. It also shows basic appointment statistics.
 */
class PatientProfileFragment : Fragment() {

    private var _binding: FragmentPatientProfileBinding? = null

    /**
     * This property is only valid between [onCreateView] and [onDestroyView].
     */
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var firestoreHelper: FirestoreHelper
    private var currentPatient: Patient? = null

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been restored in to the view.
     * This method initializes Firebase instances, sets up UI listeners, and loads the patient's profile data.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        firestoreHelper = FirestoreHelper()

        setupUI()
        loadPatientProfile()
    }

    /**
     * Sets up click listeners for various UI elements in the profile screen.
     */
    private fun setupUI() {
        binding.editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        binding.settingsButton.setOnClickListener {
            Toast.makeText(requireContext(), "Settings coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.helpButton.setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.loginButton.setOnClickListener {
            showLoginScreen()
        }
    }

    /**
     * Loads the current patient's profile information from Firestore and displays it.
     * Shows a loading spinner and handles cases where the user is not authenticated or data loading fails.
     */
    private fun loadPatientProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showNotAuthenticatedState()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.profileSection.visibility = View.GONE

        firestoreHelper.getPatientById(currentUser.uid) { patient ->
            activity?.runOnUiThread {
                binding.progressBar.visibility = View.GONE

                if (patient != null) {
                    currentPatient = patient
                    binding.profileSection.visibility = View.VISIBLE

                    binding.patientName.text = "${patient.firstName} ${patient.lastName}"
                    binding.patientEmail.text = patient.email
                    binding.patientPhone.text = patient.phoneNumber.ifEmpty { "Not provided" }
                    binding.patientDateOfBirth.text = patient.dateOfBirth.ifEmpty { "Not provided" }

                    val patientIdentifier = currentUser.email ?: currentUser.uid
                    loadAppointmentStats(patientIdentifier)
                } else {
                    showErrorState("Unable to load profile information")
                }
            }
        }
    }

    /**
     * Loads and displays the patient's appointment statistics (upcoming, completed, and total).
     *
     * @param patientId The identifier (email or UID) of the patient.
     */
    private fun loadAppointmentStats(patientId: String) {
        firestoreHelper.getUpcomingBookingsForPatient(patientId) { upcomingBookings ->
            activity?.runOnUiThread {
                binding.upcomingAppointmentsCount.text = upcomingBookings.size.toString()
            }
        }

        firestoreHelper.getCompletedBookingsForPatient(patientId) { completedBookings ->
            activity?.runOnUiThread {
                binding.favoriteDoctorsCount.text = completedBookings.size.toString()
            }
        }

        firestoreHelper.getAllBookingsForPatient(patientId) { allBookings ->
            activity?.runOnUiThread {
                binding.totalAppointmentsCount.text = allBookings.size.toString()
            }
        }
    }

    /**
     * Displays an [AlertDialog] for editing the patient's profile information.
     * Pre-fills the fields with current data and allows the user to save changes.
     */
    private fun showEditProfileDialog() {
        val currentUser = auth.currentUser ?: return
        val patient = currentPatient ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)

        val editFirstName = dialogView.findViewById<EditText>(R.id.editFirstName)
        val editLastName = dialogView.findViewById<EditText>(R.id.editLastName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val editDateOfBirth = dialogView.findViewById<EditText>(R.id.editDateOfBirth)

        editFirstName.setText(patient.firstName)
        editLastName.setText(patient.lastName)
        editPhone.setText(patient.phoneNumber)
        editDateOfBirth.setText(patient.dateOfBirth)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Edit Profile")
            .setPositiveButton("Save") { _, _ ->
                updateProfile(
                    editFirstName.text.toString().trim(),
                    editLastName.text.toString().trim(),
                    editPhone.text.toString().trim(),
                    editDateOfBirth.text.toString().trim()
                )
            }
            .setNegativeButton("Cancel", null)

        builder.create().show()
    }

    /**
     * Updates the patient's profile information in Firestore.
     * Shows a toast message indicating success or failure.
     *
     * @param firstName The new first name for the patient.
     * @param lastName The new last name for the patient.
     * @param phone The new phone number for the patient.
     * @param dateOfBirth The new date of birth for the patient.
     */
    private fun updateProfile(firstName: String, lastName: String, phone: String, dateOfBirth: String) {
        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(requireContext(), "First name and last name are required", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser ?: return
        val patient = currentPatient ?: return

        val updates = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phone,
            "dateOfBirth" to dateOfBirth
        )

        firestoreHelper.updatePatient(currentUser.uid, updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                loadPatientProfile()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Displays a confirmation dialog before logging out the user.
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Displays a confirmation dialog before deleting the user's account.
     */
    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Logs out the current user from Firebase Authentication and navigates to the login screen.
     */
    private fun logout() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        showLoginScreen()
    }

    /**
     * Deletes the current user's account from Firebase Authentication and their data from Firestore.
     * Navigates to the login screen upon successful deletion.
     */
    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: return

        db.collection("patients").document(currentUser.uid)
            .delete()
            .addOnSuccessListener {
                currentUser.delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        showLoginScreen()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to delete account: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete account data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Updates the UI to reflect a not-authenticated state, hiding profile details and showing a login prompt.
     */
    private fun showNotAuthenticatedState() {
        binding.profileSection.visibility = View.GONE
        binding.notAuthenticatedLayout.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    /**
     * Displays an error message to the user and transitions the UI to a not-authenticated state.
     *
     * @param message The error message to be displayed.
     */
    private fun showErrorState(message: String) {
        binding.profileSection.visibility = View.GONE
        binding.notAuthenticatedLayout.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Navigates the user to the [LogIn] screen and finishes the current activity.
     */
    private fun showLoginScreen() {
        val intent = Intent(requireContext(), LogIn::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    /**
     * Checks if a given [Date] of birth indicates that the person is 18 years or older.
     *
     * @param dateOfBirth The [Date] object representing the birth date.
     * @return `true` if the person is 18 or older, `false` otherwise.
     */
    private fun isAdult(dateOfBirth: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -18)
        return dateOfBirth.before(calendar.time)
    }

    /**
     * Called when the view previously created by [onCreateView] has been detached from the fragment.
     * This method cleans up the binding object to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}