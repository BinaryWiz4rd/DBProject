package com.example.project.Admin

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * A [Fragment] for administrators to create new doctor accounts.
 * This fragment provides a form for entering doctor details and handles
 * the registration process with Firebase Authentication and Firestore.
 */
class AdminCreateDoctorFragment : Fragment() {

    private val TAG = "AdminCreateDoctorFrag"

    // UI Components
    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var specializationEditText: TextInputEditText
    private lateinit var pwzNumberEditText: TextInputEditText
    private lateinit var phoneNumberEditText: TextInputEditText
    private lateinit var createDoctorButton: Button
    private lateinit var progressIndicator: CircularProgressIndicator

    // Input Layouts for validation
    private lateinit var firstNameLayout: TextInputLayout
    private lateinit var lastNameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var specializationLayout: TextInputLayout
    private lateinit var pwzNumberLayout: TextInputLayout
    private lateinit var phoneNumberLayout: TextInputLayout

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    /**
     * Inflates the layout for this fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_create_doctor, container, false)
    }

    /**
     * Initializes the views and sets up the click listener for the create button.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirestoreHelper().getDbInstance()

        // Initialize UI components
        initializeViews(view)
        
        // Set up click listener for register button
        createDoctorButton.setOnClickListener {
            if (validateInputs()) {
                registerDoctor()
            }
        }
    }

    /**
     * Initializes all the UI components from the view.
     * @param view The fragment's root view.
     */
    private fun initializeViews(view: View) {
        // EditTexts
        firstNameEditText = view.findViewById(R.id.firstNameEditText)
        lastNameEditText = view.findViewById(R.id.lastNameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
        specializationEditText = view.findViewById(R.id.specializationEditText)
        pwzNumberEditText = view.findViewById(R.id.pwzNumberEditText)
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText)
        
        // Layouts
        firstNameLayout = view.findViewById(R.id.firstNameLayout)
        lastNameLayout = view.findViewById(R.id.lastNameLayout)
        emailLayout = view.findViewById(R.id.emailLayout)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout)
        specializationLayout = view.findViewById(R.id.specializationLayout)
        pwzNumberLayout = view.findViewById(R.id.pwzNumberLayout)
        phoneNumberLayout = view.findViewById(R.id.phoneNumberLayout)
        
        // Button and Progress Indicator
        createDoctorButton = view.findViewById(R.id.createDoctorButton)
        progressIndicator = view.findViewById(R.id.progressIndicator)
    }

    /**
     * Validates all the input fields in the form.
     * @return `true` if all inputs are valid, `false` otherwise.
     */
    private fun validateInputs(): Boolean {
        // Reset errors
        firstNameLayout.error = null
        lastNameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        confirmPasswordLayout.error = null
        specializationLayout.error = null
        pwzNumberLayout.error = null
        phoneNumberLayout.error = null

        // Get values
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        val specialization = specializationEditText.text.toString().trim()
        val pwzNumber = pwzNumberEditText.text.toString().trim()
        val phoneNumber = phoneNumberEditText.text.toString().trim()

        // Validate fields
        if (firstName.isEmpty()) {
            firstNameLayout.error = "First name is required"
            return false
        }

        if (lastName.isEmpty()) {
            lastNameLayout.error = "Last name is required"
            return false
        }

        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email address"
            return false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            return false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = "Please confirm your password"
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordLayout.error = "Passwords do not match"
            return false
        }

        if (specialization.isEmpty()) {
            specializationLayout.error = "Specialization is required"
            return false
        }

        if (pwzNumber.isEmpty()) {
            pwzNumberLayout.error = "PWZ number is required"
            return false
        }

        if (phoneNumber.isEmpty()) {
            phoneNumberLayout.error = "Phone number is required"
            return false
        }

        return true
    }

    /**
     * Handles the doctor registration process. It creates a new user in
     * Firebase Authentication and then saves the doctor's details to Firestore.
     */
    private fun registerDoctor() {
        // Show progress and disable button
        progressIndicator.visibility = View.VISIBLE
        createDoctorButton.isEnabled = false

        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        // Create Firebase Auth user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User created successfully, now save additional info to Firestore
                    val userId = task.result?.user?.uid
                    if (userId != null) {
                        saveUserToFirestore(userId)
                    } else {
                        hideProgress()
                        showToast("Failed to get user ID")
                    }
                } else {
                    hideProgress()
                    showToast("Registration failed: ${task.exception?.message ?: "Unknown error"}")
                    Log.e(TAG, "Error creating user", task.exception)
                }
            }
    }

    /**
     * Saves the new doctor's data to the "users" collection in Firestore.
     * @param userId The unique ID of the user from Firebase Authentication.
     */
    private fun saveUserToFirestore(userId: String) {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val specialization = specializationEditText.text.toString().trim()
        val pwzNumber = pwzNumberEditText.text.toString().trim()
        val phoneNumber = phoneNumberEditText.text.toString().trim()

        val userData = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "role" to "doctor",
            "specialization" to specialization,
            "pwzNumber" to pwzNumber,
            "phoneNumber" to phoneNumber,
            "createdAt" to Date()
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                hideProgress()
                showToast("Doctor account created successfully")
                clearFields()
            }
            .addOnFailureListener { e ->
                hideProgress()
                showToast("Error saving doctor data: ${e.message}")
                Log.e(TAG, "Error saving doctor data", e)
            }
    }

    /**
     * Hides the progress indicator and re-enables the create button.
     */
    private fun hideProgress() {
        progressIndicator.visibility = View.GONE
        createDoctorButton.isEnabled = true
    }

    /**
     * Displays a short toast message.
     * @param message The message to be displayed.
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Clears all the input fields in the form.
     */
    private fun clearFields() {
        firstNameEditText.setText("")
        lastNameEditText.setText("")
        emailEditText.setText("")
        passwordEditText.setText("")
        confirmPasswordEditText.setText("")
        specializationEditText.setText("")
        pwzNumberEditText.setText("")
        phoneNumberEditText.setText("")
    }
}