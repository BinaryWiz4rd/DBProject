package com.example.project.Menu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R

class SplashScreen : AppCompatActivity() {

    /**
     * Shows the splash screen and navigates to the LogIn activity after a delay.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Po 3 sekundach przechodzi do RoleSelectionActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
            finish()
        }, 3000) // 3000 ms = 3 sekundy
    }
}
