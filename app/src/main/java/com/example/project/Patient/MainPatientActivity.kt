package com.example.project.Patient

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.project.R
import com.example.project.databinding.ActivityMainPatientNewBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainPatientActivity : BaseActivity() {
    private lateinit var binding: ActivityMainPatientNewBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPatientNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupBottomNavigation()
        
        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(PatientHomeFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(PatientHomeFragment())
                    true
                }
                R.id.nav_doctors -> {
                    loadFragment(PatientDoctorsFragment())
                    true
                }
                R.id.nav_services -> {
                    loadFragment(PatientServicesFragment())
                    true
                }
                R.id.nav_appointments -> {
                    loadFragment(PatientAppointmentsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(PatientProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Navigation helpers for fragments
    fun navigateToBooking() {
        loadFragment(PatientServicesFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_services
    }

    fun navigateToAppointments() {
        loadFragment(PatientAppointmentsFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_appointments
    }

    fun navigateToDoctors() {
        loadFragment(PatientDoctorsFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_doctors
    }
    
    fun navigateToBookingWithDoctor(doctorId: String) {
        // Navigate to booking flow with pre-selected doctor
        val fragment = PatientServicesFragment()
        val bundle = Bundle()
        bundle.putString("selected_doctor_id", doctorId)
        fragment.arguments = bundle
        
        loadFragment(fragment)
        binding.bottomNavigation.selectedItemId = R.id.nav_services
    }
    
    fun navigateToHome() {
        loadFragment(PatientHomeFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }
    
    fun getCurrentUser() = auth.currentUser
    
    override fun onBackPressed() {
        // If not on home fragment, navigate to home
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is PatientHomeFragment) {
            navigateToHome()
        } else {
            super.onBackPressed()
        }
    }

}