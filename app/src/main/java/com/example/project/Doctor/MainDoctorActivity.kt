package com.example.project.doctor.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.project.doctor.DoctorCalendarFragment
import com.example.project.doctor.DoctorChatFragment
import com.example.project.doctor.DoctorHomeFragment
import com.example.project.doctor.DoctorScheduleFragment
import com.example.project.Menu.LogIn
import com.example.project.R
import com.example.project.doctor.ui.DoctorServicesFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainDoctorActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var profileButton: ImageButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_doctor)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        //this is to retrieve FCM token and save it in Firestore
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val currentUserId = auth.currentUser?.uid
                if (currentUserId != null && token != null) {
                    db.collection("doctors")
                        .document(currentUserId)
                        .update("fcmToken", token)
                        .addOnSuccessListener {
                            Log.d("FCM", "token updated successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FCM", "failed to update token", e)

                        }
                }
            } else {
                Log.w("FCM", "fetching FCM token failed", task.exception)
            }
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        floatingActionButton = findViewById(R.id.floatingActionButton)
        profileButton = findViewById(R.id.profileButton)

        FirebaseMessaging.getInstance().subscribeToTopic("doctor_${auth.currentUser?.uid}")
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) "subscribed to notifications" else "subscription failed"
                Log.d("FCM", msg)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    loadFragment(DoctorHomeFragment())
                    true
                }
                R.id.schedule -> {
                    loadFragment(DoctorScheduleFragment())
                    true
                }
                R.id.calendar -> {
                    loadFragment(DoctorCalendarFragment())
                    true
                }
                R.id.services -> {
                    loadFragment(DoctorServicesFragment())
                    true
                }
                else -> false
            }
        }

        floatingActionButton.setOnClickListener {
            loadFragment(DoctorChatFragment())
        }

        profileButton.setOnClickListener {
            showProfileDialog()
        }

        if (savedInstanceState == null) {
            loadFragment(DoctorHomeFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment)
        transaction.commit()
    }

    private fun showProfileDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_doctor, null)

        val editTextFirstName = dialogView.findViewById<EditText>(R.id.editTextFirstName)
        val editTextLastName = dialogView.findViewById<EditText>(R.id.editTextLastName)
        val editTextEmail = dialogView.findViewById<EditText>(R.id.editTextEmail)
        val editTextPWZ = dialogView.findViewById<EditText>(R.id.editTextPWZ)
        val editTextSpecialization = dialogView.findViewById<EditText>(R.id.editTextSpecialization)
        val buttonEdit = dialogView.findViewById<Button>(R.id.buttonEdit)
        val buttonLogout = dialogView.findViewById<Button>(R.id.buttonLogout)
        val buttonDeleteAccount = dialogView.findViewById<Button>(R.id.buttonDeleteAccount)

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("doctors").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val email = document.getString("email") ?: ""
                        val pwz = document.getString("pwz") ?: ""
                        val specialization = document.getString("specialization") ?: ""
                        editTextFirstName.setText(firstName)
                        editTextLastName.setText(lastName)
                        editTextEmail.setText(email)
                        editTextPWZ.setText(pwz)
                        editTextSpecialization.setText(specialization)
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
            val newPWZ = editTextPWZ.text.toString().trim()

            if (newFirstName.isEmpty() || newLastName.isEmpty() || newEmail.isEmpty() || newPWZ.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!newPWZ.matches(Regex("\\d{7,}"))) {
                Toast.makeText(this, "Wrong PWZ number (minimum of 7 digits)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentUser?.let { user ->
                //fetching data to compare changes
                db.collection("doctors").document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val oldFirstName = document.getString("firstName") ?: ""
                            val oldLastName = document.getString("lastName") ?: ""
                            val oldEmail = document.getString("email") ?: ""
                            val oldPWZ = document.getString("pwz") ?: ""
                            val oldSpecialization = document.getString("specialization") ?: ""

                            val updates = hashMapOf<String, Any>()
                            val changes = mutableMapOf<String, Any>()

                            if (newFirstName != oldFirstName) {
                                updates["firstName"] = newFirstName
                                changes["firstName"] = newFirstName
                            }
                            if (newLastName != oldLastName) {
                                updates["lastName"] = newLastName
                                changes["lastName"] = newLastName
                            }
                            if (newEmail != oldEmail) {
                                updates["email"] = newEmail
                                changes["email"] = newEmail
                            }
                            if (newPWZ != oldPWZ) {
                                updates["pwz"] = newPWZ
                                changes["pwz"] = newPWZ
                            }
                            //nie chce mi sie pozwalac na zmienianie specki, na razie sprawdzam
                            // jak to dziala sobie

                            if (changes.isNotEmpty()) {
                                //store the edit request for admin review
                                val editRequest = hashMapOf(
                                    "userId" to user.uid,
                                    "timestamp" to com.google.firebase.Timestamp.now(),
                                    "changes" to changes
                                )
                                db.collection("edit_requests").add(editRequest)
                                    .addOnSuccessListener {
                                        // Update the user's 'edit' status
                                        db.collection("doctors").document(user.uid)
                                            .update("edit", true)
                                            .addOnSuccessListener {
                                                Toast.makeText(this@MainDoctorActivity, "Profile update requested!", Toast.LENGTH_SHORT).show()
                                                alertDialog.dismiss()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this@MainDoctorActivity, "Error updating edit status: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this@MainDoctorActivity, "Error submitting edit request: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this@MainDoctorActivity, "No changes to update.", Toast.LENGTH_SHORT).show()
                                alertDialog.dismiss()
                            }
                        } else {
                            Toast.makeText(this@MainDoctorActivity, "Could not retrieve profile data.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@MainDoctorActivity, "Error retrieving profile data: ${e.message}", Toast.LENGTH_SHORT).show()}
            } ?: run {
                Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLogout.setOnClickListener {
            auth.signOut()
            alertDialog.dismiss() // Dismiss dialog before finishing activity
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finish()
            startActivity(Intent(this, LogIn::class.java))
        }

        buttonDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    currentUser?.delete()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                db.collection("doctors").document(currentUser.uid)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "account deleted", Toast.LENGTH_SHORT).show()
                                        alertDialog.dismiss()
                                        finish()
                                        startActivity(Intent(this, LogIn::class.java))
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "error deleting user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "error deleting account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                .setNegativeButton("cancel", null)
                .show()
        }
    }
}