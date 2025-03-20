package com.example.project.view.doctor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.project.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainDoctorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_doctor)

        val doctorHomeFragment = DoctorHomeFragment()
        val doctorMedicalServicesFragment = DoctorMedicalServicesFragment()
        val doctorAccountFragment = DoctorAccountFragment()

        makeCurrentFragment(doctorHomeFragment)

        findViewById<BottomNavigationView>(R.id.bottom_navigation_doctor).setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.baseline_home -> makeCurrentFragment(doctorHomeFragment)
                R.id.baseline_account -> makeCurrentFragment(doctorAccountFragment)
                R.id.baseline_medical_services -> makeCurrentFragment(doctorMedicalServicesFragment)
            }
            true
        }
    }

    private fun makeCurrentFragment(fragment: Fragment) = supportFragmentManager.beginTransaction().apply{
        replace(R.id.fl_wrapper,fragment)
        commit()
    }
}