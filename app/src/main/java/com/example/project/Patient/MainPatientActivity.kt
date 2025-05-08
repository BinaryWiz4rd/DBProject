package com.example.project.Patient

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.Menu.LogIn
import com.example.project.R
import com.example.project.databinding.ActivityMainPatientBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainPatientActivity : BaseActivity() {
    private lateinit var binding: ActivityMainPatientBinding
    private val viewModel = MainViewModel()
    private lateinit var profileButton: ImageButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        profileButton = findViewById(R.id.profileButton)

        profileButton.setOnClickListener {
            showProfileDialog()
        }

        initCategory()
        initTopDoctors()
    }

    private fun initTopDoctors() {
        binding.apply {
            progressBarTopDoctor.visibility = View.VISIBLE
            viewModel.doctors.observe(this@MainPatientActivity, Observer {
                recyclerViewTopDoctor.layoutManager =
                    LinearLayoutManager(this@MainPatientActivity, LinearLayoutManager.HORIZONTAL, false)
                recyclerViewTopDoctor.adapter = TopDoctorAdapter(it)
                progressBarTopDoctor.visibility = View.GONE
            })
            viewModel.loadDoctors()

            doctorListTxt.setOnClickListener {
                startActivity(Intent(this@MainPatientActivity, TopDoctorsActivity::class.java))
            }
        }
    }

    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE
        viewModel.category.observe(this, Observer {
            binding.viewCategory.layoutManager =
                LinearLayoutManager(this@MainPatientActivity, LinearLayoutManager.HORIZONTAL, false)
            binding.viewCategory.adapter = CategoryAdapter(it)
            binding.progressBarCategory.visibility = View.GONE
        })
        viewModel.loadCategory()
    }
    private fun showProfileDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_patient, null)

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
                        Toast.makeText(this, "Could not retrieve profile data.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error retrieving profile data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }

        val builder = AlertDialog.Builder(this)
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
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.isLenient = false
            val newDateOfBirth: Date
            try {
                newDateOfBirth = sdf.parse(newDateOfBirthStr) ?: throw ParseException("Parsed date is null", 0)
            } catch (e: ParseException) {
                Toast.makeText(this, "Invalid date format (use yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isAdult(newDateOfBirth)) {
                Toast.makeText(this, "You must be 18 or older.", Toast.LENGTH_SHORT).show()
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
                                                Toast.makeText(this, "Profile update requested!", Toast.LENGTH_SHORT).show()
                                                alertDialog.dismiss()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Error updating edit status: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error submitting edit request: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "No changes to update.", Toast.LENGTH_SHORT).show()
                                alertDialog.dismiss()
                            }
                        } else {
                            Toast.makeText(this, "Could not retrieve current profile data for comparison.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error retrieving profile data for comparison: ${e.message}", Toast.LENGTH_SHORT).show()}
            } ?: run {
                Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LogIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        buttonDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
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
                                            Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                                            alertDialog.dismiss()
                                            val intent = Intent(this, LogIn::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Error deleting account authentication: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error deleting user data from Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } ?: run {
                        Toast.makeText(this, "User not authenticated. Cannot delete account.", Toast.LENGTH_SHORT).show()
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
}