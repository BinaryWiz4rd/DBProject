package com.example.project.Patient

import android.content.Intent
import android.os.Bundle
import com.example.project.databinding.ActivityIntroBinding

/**
 * The initial activity displayed to the user upon launching the patient application.
 *
 * This activity serves as an introduction and provides a button to navigate to the
 * [MainPatientActivity].
 */
class IntroActivity : BaseActivity() {
    private lateinit var binding: ActivityIntroBinding

    /**
     * Called when the activity is first created.
     *
     * Initializes the view binding and sets up the click listener for the start button,
     * which navigates the user to the [MainPatientActivity].
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in [onSaveInstanceState]. Note: Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            startBtn.setOnClickListener {
                startActivity(Intent(this@IntroActivity, MainPatientActivity::class.java))
            }
        }
    }
}