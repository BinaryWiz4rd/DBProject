package com.example.project.Patient

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.project.R
import com.example.project.databinding.ActivityMainPatientNewBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

/**
 * Main activity for the patient application, hosting various fragments
 * managed by a [BottomNavigationView].
 *
 * This activity handles navigation between different sections of the patient app
 * like Home, Doctors, Services, Appointments, and Profile, using fragments.
 * It also provides helper functions for fragment navigation and access to the
 * current Firebase user.
 */
class MainPatientActivity : BaseActivity() {
    private lateinit var binding: ActivityMainPatientNewBinding
    private lateinit var auth: FirebaseAuth

    /**
     * Called when the activity is first created.
     *
     * Initializes view binding, Firebase authentication, sets up the bottom navigation,
     * and loads the [PatientHomeFragment] as the default fragment.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in [onSaveInstanceState]. Note: Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPatientNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(PatientHomeFragment())
        }
    }

    /**
     * Sets up the [BottomNavigationView] to handle navigation item selections.
     *
     * When a navigation item is selected, the corresponding fragment is loaded
     * into the `fragment_container`.
     */
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

    /**
     * Replaces the current fragment in `fragment_container` with the specified [fragment].
     *
     * @param fragment The [Fragment] to be loaded.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Navigates to the "Services" fragment and selects the corresponding item
     * in the bottom navigation.
     */
    fun navigateToBooking() {
        loadFragment(PatientServicesFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_services
    }

    /**
     * Navigates to the "Appointments" fragment and selects the corresponding item
     * in the bottom navigation.
     */
    fun navigateToAppointments() {
        loadFragment(PatientAppointmentsFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_appointments
    }

    /**
     * Navigates to the "Doctors" fragment and selects the corresponding item
     * in the bottom navigation.
     */
    fun navigateToDoctors() {
        loadFragment(PatientDoctorsFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_doctors
    }

    /**
     * Navigates to the "Services" fragment, passing a pre-selected doctor ID
     * to potentially filter or highlight services for that doctor.
     *
     * @param doctorId The ID of the doctor to pre-select or filter by.
     */
    fun navigateToBookingWithDoctor(doctorId: String) {
        val fragment = PatientServicesFragment()
        val bundle = Bundle()
        bundle.putString("selected_doctor_id", doctorId)
        fragment.arguments = bundle

        loadFragment(fragment)
        binding.bottomNavigation.selectedItemId = R.id.nav_services
    }

    /**
     * Navigates to the "Home" fragment and selects the corresponding item
     * in the bottom navigation.
     */
    fun navigateToHome() {
        loadFragment(PatientHomeFragment())
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    /**
     * Returns the current authenticated Firebase user.
     *
     * @return The current [com.google.firebase.auth.FirebaseUser] or null if no user is signed in.
     */
    fun getCurrentUser() = auth.currentUser

    /**
     * Overrides the default back button behavior.
     *
     * If the current fragment is not [PatientHomeFragment], it navigates to the home fragment.
     * Otherwise, it performs the default back action.
     */
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is PatientHomeFragment) {
            navigateToHome()
        } else {
            super.onBackPressed()
        }
    }
}