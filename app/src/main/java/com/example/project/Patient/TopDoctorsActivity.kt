package com.example.project.Patient

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.databinding.ActivityTopDoctorsBinding

/**
 * Activity displaying a list of top doctors for patients.
 * Uses a ViewModel to observe and display doctor data in a RecyclerView.
 */
class TopDoctorsActivity : BaseActivity() {
    private lateinit var binding: ActivityTopDoctorsBinding
    private val viewModel= MainViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityTopDoctorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTopDoctors()

    }

    /**
     * Initializes the top doctors list and sets up the RecyclerView.
     */
    private fun initTopDoctors() {
        binding.apply {
            progressBarTopDoctor.visibility= View.VISIBLE
            viewModel.doctors.observe(this@TopDoctorsActivity, Observer {
                viewTopDoctorList.layoutManager=
                    LinearLayoutManager(this@TopDoctorsActivity, LinearLayoutManager.VERTICAL,false)
                viewTopDoctorList.adapter= TopDoctorAdapter2(it)
                progressBarTopDoctor.visibility= View.GONE
            })
            viewModel.loadDoctors()

            backBtn.setOnClickListener { finish() }
        }
    }
}
