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

class PatientHomeFragment : Fragment() {
    
    private var _binding: FragmentPatientHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var firestoreHelper: FirestoreHelper
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
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
    
    private fun setupUI() {
        binding.profileButton.setOnClickListener {
            showProfileDialog()
        }
        
        // Set up quick action buttons
        binding.bookAppointmentButton.setOnClickListener {
            // Navigate to booking flow
            (activity as? MainPatientActivity)?.navigateToBooking()
        }
          binding.viewAllDoctorsButton.setOnClickListener {
            // Navigate to doctors tab
            (activity as? MainPatientActivity)?.navigateToDoctors()
        }
        
        binding.myAppointmentsButton.setOnClickListener {
            // Navigate to appointments
            (activity as? MainPatientActivity)?.navigateToAppointments()
        }
    }
      private fun loadPatientDashboard() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.welcomeText.text = "Hi, Welcome!"
            return        }
        
        // Show loading state
        binding.progressBarCategory?.visibility = View.VISIBLE
        
        // Load patient profile
        firestoreHelper.getPatientById(currentUser.uid) { patient ->
            activity?.runOnUiThread {
                if (patient != null) {
                    binding.welcomeText.text = "Hi, ${patient.firstName} ${patient.lastName}!"                } else {
                    binding.welcomeText.text = "Hi, Welcome!"
                }
            }
        }        
        // Load upcoming appointments - use email instead of UID
        val patientIdentifier = currentUser.email ?: currentUser.uid
        firestoreHelper.getUpcomingBookingsForPatient(patientIdentifier) { appointmentDetails ->
            activity?.runOnUiThread {
                binding.progressBarCategory?.visibility = View.GONE
                
                if (appointmentDetails.isNotEmpty()) {
                    binding.upcomingAppointmentCard.visibility = View.VISIBLE
                    val nextAppointment = appointmentDetails.first()
                    
                    // Format date and time
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val date = inputFormat.parse(nextAppointment.date)
                        val formattedDate = if (date != null) outputFormat.format(date) else nextAppointment.date
                          binding.nextAppointmentText.text = "Next appointment: $formattedDate at ${nextAppointment.startTime}"
                        binding.nextAppointmentDoctor.text = "Dr. ${nextAppointment.doctorName}"                    } catch (e: ParseException) {
                        binding.nextAppointmentText.text = "Next appointment: ${nextAppointment.date} at ${nextAppointment.startTime}"
                        binding.nextAppointmentDoctor.text = "Dr. ${nextAppointment.doctorName}"
                    }
                    
                    // Set click listener for appointment card
                    binding.upcomingAppointmentCard.setOnClickListener {
                        (activity as? MainPatientActivity)?.navigateToAppointments()
                    }
                } else {
                    binding.upcomingAppointmentCard.visibility = View.GONE
                }
            }
        }
    }
      private fun initTopDoctors() {
        binding.apply {
            progressBarTopDoctor.visibility = View.VISIBLE
            
            // Load top doctors using FirestoreHelper
            firestoreHelper.getAllDoctors { doctors ->
                activity?.runOnUiThread {
                    progressBarTopDoctor.visibility = View.GONE
                    
                    if (doctors.isNotEmpty()) {
                        // Take only top 5 doctors for the home screen
                        val topDoctors = doctors.take(5)
                        recyclerViewTopDoctor.layoutManager =
                            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        recyclerViewTopDoctor.adapter = DoctorListAdapter(topDoctors) { doctor ->
                            // Navigate to doctor details
                            (activity as? MainPatientActivity)?.navigateToBookingWithDoctor(doctor.uid)
                        }
                    }
                }
            }
            
            doctorListTxt.setOnClickListener {
                // Navigate to doctors tab
                (activity as? MainPatientActivity)?.navigateToDoctors()
            }
        }
    }
      private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE
        
        // Load categories from Firebase (if categories collection exists)
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
                // Hide category section if no categories found
                binding.viewCategory.visibility = View.GONE
            }
    }
    
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
    
    private fun isAdult(dateOfBirth: Date): Boolean {
        val dob = Calendar.getInstance().apply { time = dateOfBirth }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age >= 18
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
