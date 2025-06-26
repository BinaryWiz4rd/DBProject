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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.Menu.LogIn
import com.example.project.R
import com.example.project.databinding.FragmentPatientHomeBinding
import com.example.project.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A [Fragment] subclass that displays the patient's home dashboard.
 * This includes a welcome message, quick action buttons, upcoming appointments,
 * top doctors, and medical categories. It also provides functionality to
 * view and edit the patient's profile, log out, and delete the account.
 */
class PatientHomeFragment : Fragment() {

    private var _binding: FragmentPatientHomeBinding? = null

    /**
     * This property is only valid between [onCreateView] and [onDestroyView].
     */
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var firestoreHelper: FirestoreHelper

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
        _binding = FragmentPatientHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been restored in to the view.
     * This method initializes Firebase instances, sets up UI listeners, and loads dashboard data.
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
        loadPatientDashboard()
        initCategory()
        initTopDoctors()
    }

    /**
     * Sets up click listeners for various UI elements like the profile button and quick action buttons.
     */
    private fun setupUI() {
        binding.profileButton.setOnClickListener {
            showProfileDialog()
        }

        binding.bookAppointmentButton.setOnClickListener {
            (activity as? MainPatientActivity)?.navigateToBooking()
        }
        binding.viewAllDoctorsButton.setOnClickListener {
            (activity as? MainPatientActivity)?.navigateToDoctors()
        }

        binding.myAppointmentsButton.setOnClickListener {
            (activity as? MainPatientActivity)?.navigateToAppointments()
        }
    }

    /**
     * Loads patient-specific data for the dashboard, including their name and upcoming appointments.
     * Displays a welcome message and the next appointment details, or hides the appointment card if none.
     */
    private fun loadPatientDashboard() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.welcomeText.text = "Hi, Welcome!"
            return
        }

        binding.progressBarCategory?.visibility = View.VISIBLE

        firestoreHelper.getPatientById(currentUser.uid) { patient ->
            activity?.runOnUiThread {
                if (patient != null) {
                    binding.welcomeText.text = "Hi, ${patient.firstName} ${patient.lastName}!"
                } else {
                    binding.welcomeText.text = "Hi, Welcome!"
                }
            }
        }

        val patientIdentifier = currentUser.email ?: currentUser.uid
        firestoreHelper.getUpcomingBookingsForPatient(patientIdentifier) { appointmentDetails ->
            activity?.runOnUiThread {
                binding.progressBarCategory?.visibility = View.GONE

                if (appointmentDetails.isNotEmpty()) {
                    binding.upcomingAppointmentCard.visibility = View.VISIBLE
                    val nextAppointment = appointmentDetails.first()

                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val date = inputFormat.parse(nextAppointment.date)
                        val formattedDate = if (date != null) outputFormat.format(date) else nextAppointment.date
                        binding.nextAppointmentText.text = "Next appointment: $formattedDate at ${nextAppointment.startTime}"
                        binding.nextAppointmentDoctor.text = "Dr. ${nextAppointment.doctorName}"
                    } catch (e: ParseException) {
                        binding.nextAppointmentText.text = "Next appointment: ${nextAppointment.date} at ${nextAppointment.startTime}"
                        binding.nextAppointmentDoctor.text = "Dr. ${nextAppointment.doctorName}"
                    }

                    binding.upcomingAppointmentCard.setOnClickListener {
                        (activity as? MainPatientActivity)?.navigateToAppointments()
                    }
                } else {
                    binding.upcomingAppointmentCard.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Initializes and displays a list of top doctors in a horizontal RecyclerView.
     * Also sets a click listener to navigate to the all doctors tab.
     */
    private fun initTopDoctors() {
        binding.apply {
            progressBarTopDoctor.visibility = View.VISIBLE

            firestoreHelper.getAllDoctors { doctors ->
                activity?.runOnUiThread {
                    progressBarTopDoctor.visibility = View.GONE

                    if (doctors.isNotEmpty()) {
                        val topDoctors = doctors.take(5)
                        recyclerViewTopDoctor.layoutManager =
                            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        recyclerViewTopDoctor.adapter = DoctorListAdapter(topDoctors) { doctor ->
                            (activity as? MainPatientActivity)?.navigateToBookingWithDoctor(doctor.uid)
                        }
                    }
                }
            }

            doctorListTxt.setOnClickListener {
                (activity as? MainPatientActivity)?.navigateToDoctors()
            }
        }
    }

    /**
     * Initializes and displays medical categories in a horizontal RecyclerView.
     * Fetches categories from Firestore and handles success and failure scenarios.
     */
    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE

        db.collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBarCategory.visibility = View.GONE
                val categories = mutableListOf<CategoryModel>()

                for (document in documents) {
                    val category = document.toObject(CategoryModel::class.java)
                    categories.add(category)
                }

                if (categories.isNotEmpty()) {
                    binding.viewCategory.layoutManager =
                        LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    binding.viewCategory.adapter = CategoryAdapter(categories)
                }
            }
            .addOnFailureListener {
                binding.progressBarCategory.visibility = View.GONE
                binding.viewCategory.visibility = View.GONE
            }
    }

    /**
     * Displays an [AlertDialog] for the patient's profile, allowing them to view, edit,
     * log out, or delete their account.
     */
    private fun showProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile_patient, null)

        val editTextFirstName = dialogView.findViewById<EditText>(R.id.editTextFirstName)
        val editTextLastName = dialogView.findViewById<EditText>(R.id.editTextLastName)
        val editTextEmail = dialogView.findViewById<EditText>(R.id.editTextEmail)
        val editTextDateOfBirth = dialogView.findViewById<EditText>(R.id.editTextDateOfBirth)

        val buttonEdit = dialogView.findViewById<Button>(R.id.buttonEdit)
        val buttonLogout = dialogView.findViewById<Button>(R.id.buttonLogout)
        val buttonDeleteAccount = dialogView.findViewById<Button>(R.id.buttonDeleteAccount)

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("patients").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val email = document.getString("email") ?: ""
                        val dateOfBirth = document.getString("dateOfBirth") ?: ""

                        editTextFirstName.setText(firstName)
                        editTextLastName.setText(lastName)
                        editTextEmail.setText(email)
                        editTextDateOfBirth.setText(dateOfBirth)
                    } else {
                        Toast.makeText(requireContext(), "Could not retrieve profile data.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error retrieving profile data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Profile")

        val alertDialog = builder.create()
        alertDialog.show()

        buttonEdit.setOnClickListener {
            val newFirstName = editTextFirstName.text.toString().trim()
            val newLastName = editTextLastName.text.toString().trim()
            val newEmail = editTextEmail.text.toString().trim()
            val newDateOfBirthStr = editTextDateOfBirth.text.toString().trim()

            if (newFirstName.isEmpty() || newLastName.isEmpty() || newEmail.isEmpty() || newDateOfBirthStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.isLenient = false
            val newDateOfBirth: Date
            try {
                newDateOfBirth = sdf.parse(newDateOfBirthStr) ?: throw ParseException("Parsed date is null", 0)
            } catch (e: ParseException) {
                Toast.makeText(requireContext(), "Invalid date format (use yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isAdult(newDateOfBirth)) {
                Toast.makeText(requireContext(), "You must be 18 or older.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentUser?.let { user ->
                db.collection("patients").document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val oldFirstName = document.getString("firstName") ?: ""
                            val oldLastName = document.getString("lastName") ?: ""
                            val oldEmail = document.getString("email") ?: ""
                            val oldDateOfBirth = document.getString("dateOfBirth") ?: ""

                            val changes = mutableMapOf<String, Any>()

                            if (newFirstName != oldFirstName) {
                                changes["firstName"] = newFirstName
                            }
                            if (newLastName != oldLastName) {
                                changes["lastName"] = newLastName
                            }
                            if (newEmail != oldEmail) {
                                changes["email"] = newEmail
                            }
                            if (newDateOfBirthStr != oldDateOfBirth) {
                                changes["dateOfBirth"] = newDateOfBirthStr
                            }

                            if (changes.isNotEmpty()) {
                                val editRequest = hashMapOf(
                                    "userId" to user.uid,
                                    "userType" to "patient",
                                    "timestamp" to com.google.firebase.Timestamp.now(),
                                    "changes" to changes
                                )
                                db.collection("edit_requests").add(editRequest)
                                    .addOnSuccessListener {
                                        db.collection("patients").document(user.uid)
                                            .update("edit_pending", true)
                                            .addOnSuccessListener {
                                                Toast.makeText(requireContext(), "Profile update requested!", Toast.LENGTH_SHORT).show()
                                                alertDialog.dismiss()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(requireContext(), "Error updating edit status: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), "Error submitting edit request: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(requireContext(), "No changes to update.", Toast.LENGTH_SHORT).show()
                                alertDialog.dismiss()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Could not retrieve current profile data for comparison.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error retrieving profile data for comparison: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LogIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        buttonDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    currentUser?.let { userToDelete ->
                        val userId = userToDelete.uid
                        db.collection("patients").document(userId)
                            .delete()
                            .addOnSuccessListener {
                                userToDelete.delete()
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(requireContext(), "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                                            alertDialog.dismiss()
                                            val intent = Intent(requireContext(), LogIn::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            requireActivity().finish()
                                        } else {
                                            Toast.makeText(requireContext(), "Error deleting account authentication: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error deleting user data from Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } ?: run {
                        Toast.makeText(requireContext(), "User not authenticated. Cannot delete account.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    /**
     * Checks if a given [Date] of birth corresponds to an individual who is 18 years or older.
     *
     * @param dateOfBirth The [Date] object representing the birth date.
     * @return `true` if the person is 18 or older, `false` otherwise.
     */
    private fun isAdult(dateOfBirth: Date): Boolean {
        val dob = Calendar.getInstance().apply { time = dateOfBirth }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age >= 18
    }

    /**
     * Called when the view previously created by [onCreateView] has been detached from the fragment.
     * This is an important lifecycle callback to clean up the binding object.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}