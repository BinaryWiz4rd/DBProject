package com.example.project.Admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.project.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainAdminActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_admin)

        bottomNavigationView = findViewById(R.id.admin_bottom_navigation)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            var selectedFragment: Fragment? = null
            when (menuItem.itemId) {
                R.id.nav_admin_appointments -> selectedFragment = AdminAppointmentsFragment()
                R.id.nav_admin_patients -> selectedFragment = AdminPatientsFragment()
                R.id.nav_admin_doctors -> selectedFragment = AdminDoctorsFragment()
                R.id.nav_admin_create_doctor -> selectedFragment = AdminCreateDoctorFragment()
                R.id.nav_admin_edit_requests -> selectedFragment = AdminEditRequestsFragment()
            }
            if (selectedFragment != null) {
                loadFragment(selectedFragment)
            }
            true
        }

        // Load the default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_admin_appointments // Set default selected item
            loadFragment(AdminAppointmentsFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .commit()
    }
    
    // Add this method to navigate to any fragment
    fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .addToBackStack(null)  // Add to back stack so user can navigate back
            .commit()
    }
}