package com.example.project.Doctor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.project.R

class MainDoctorActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var floatingActionButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_doctor)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        floatingActionButton = findViewById(R.id.floatingActionButton)

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
                else -> false
            }
        }

        floatingActionButton.setOnClickListener {
            loadFragment(DoctorChatFragment())

        }

        if (savedInstanceState == null) {
            loadFragment(DoctorHomeFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment)
        //transaction.addToBackStack(null) poczytac o backstack
        transaction.commit()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun shouldShowChatItem(): Boolean {
        return true //chyba juz niepotrzebne
    }
}