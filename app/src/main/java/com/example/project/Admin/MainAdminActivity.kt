package com.example.project.Admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.project.R
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main activity for the admin panel. This activity hosts various admin-specific fragments
 * and provides navigation between them using a [BottomNavigationView].
 */
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

        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_admin_appointments
            loadFragment(AdminAppointmentsFragment())
        }
    }

    /**
     * Replaces the current fragment in the [R.id.admin_fragment_container] with the specified [fragment].
     *
     * @param fragment The [Fragment] to load into the container.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .commit()
    }

    /**
     * Navigates to the specified [fragment] by replacing the current fragment in the container
     * and adding the transaction to the back stack. This allows the user to navigate back to
     * the previous fragment using the device's back button.
     *
     * @param fragment The [Fragment] to navigate to.
     */
    fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}