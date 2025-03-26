/**package com.example.project.Doctor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

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
                R.id.schedule -> {
                    loadFragment(DoctorScheduleFragment())
                    true
                }
                R.id.calendar -> {
                    loadFragment(DoctorCalendarFragment())
                    true
                }
                //R.id.chat -> { potem sie stworzy
                //loadFragment(ChatFragment())
                // true
                // }
                else -> false
            }
        }

        floatingActionButton.setOnClickListener {
            openChatActivity()
        }

        if (savedInstanceState == null) {
            loadFragment(DoctorScheduleFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun openChatActivity() {
        //val intent = Intent(this, ChatActivity::class.java)
        //startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        if (shouldShowChatItem()) {
            bottomNavigationView.menu.findItem(R.id.chat).isVisible = true
        } else {
            bottomNavigationView.menu.findItem(R.id.chat).isVisible = false
        }
    }

    private fun shouldShowChatItem(): Boolean {
        return true //potem sie tu pobawi
    }
}
*/