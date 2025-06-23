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

class PatientProfileFragment : Fragment() {

    private var _binding: FragmentPatientProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var firestoreHelper: FirestoreHelper
    private var currentPatient: Patient? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        firestoreHelper = FirestoreHelper()

        setupUI()
        loadPatientProfile()
    }

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

    private fun loadPatientProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showNotAuthenticatedState()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.profileSection.visibility = View.GONE

        // Load patient profile data
        firestoreHelper.getPatientById(currentUser.uid) { patient ->
            activity?.runOnUiThread {
                binding.progressBar.visibility = View.GONE

                if (patient != null) {
                    currentPatient = patient
                    binding.profileSection.visibility = View.VISIBLE

                    // Set profile information
                    binding.patientName.text = "${patient.firstName} ${patient.lastName}"
                    binding.patientEmail.text = patient.email
                    binding.patientPhone.text = patient.phoneNumber.ifEmpty { "Not provided" }
                    binding.patientDateOfBirth.text = patient.dateOfBirth.ifEmpty { "Not provided" }

                    // Load appointment statistics - use email instead of UID
                    val patientIdentifier = currentUser.email ?: currentUser.uid
                    loadAppointmentStats(patientIdentifier)
                } else {
                    showErrorState("Unable to load profile information")
                }
            }
        }
    }

    private fun loadAppointmentStats(patientId: String) {
        // Load upcoming appointments  
        firestoreHelper.getUpcomingBookingsForPatient(patientId) { upcomingBookings ->
            activity?.runOnUiThread {
                binding.upcomingAppointmentsCount.text = upcomingBookings.size.toString()
            }
        }

        // Load completed appointments (using favorite doctors count for now)
        firestoreHelper.getCompletedBookingsForPatient(patientId) { completedBookings ->
            activity?.runOnUiThread {
                binding.favoriteDoctorsCount.text = completedBookings.size.toString()
            }
        }

        // Load total appointments
        firestoreHelper.getAllBookingsForPatient(patientId) { allBookings ->
            activity?.runOnUiThread {
                binding.totalAppointmentsCount.text = allBookings.size.toString()
            }
        }
    }

    private fun showEditProfileDialog() {
        val currentUser = auth.currentUser ?: return
        val patient = currentPatient ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)

        val editFirstName = dialogView.findViewById<EditText>(R.id.editFirstName)
        val editLastName = dialogView.findViewById<EditText>(R.id.editLastName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val editDateOfBirth = dialogView.findViewById<EditText>(R.id.editDateOfBirth)

        // Load current data
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

    private fun logout() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        showLoginScreen()
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: return

        // Delete user document from Firestore
        db.collection("patients").document(currentUser.uid)
            .delete()
            .addOnSuccessListener {
                // Delete Firebase Auth account
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

    private fun showNotAuthenticatedState() {
        binding.profileSection.visibility = View.GONE
        binding.notAuthenticatedLayout.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        binding.profileSection.visibility = View.GONE
        binding.notAuthenticatedLayout.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoginScreen() {
        val intent = Intent(requireContext(), LogIn::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun isAdult(dateOfBirth: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -18)
        return dateOfBirth.before(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
