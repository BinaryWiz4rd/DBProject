package com.example.project.Patient

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.databinding.ActivityMainPatientBinding

class MainPatientActivity : BaseActivity() {
    private lateinit var binding: ActivityMainPatientBinding
    private val viewModel = MainViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initCategory()
        initTopDoctors()
    }

    private fun initTopDoctors() {
        binding.apply {
            progressBarTopDoctor.visibility = View.VISIBLE
            viewModel.doctors.observe(this@MainPatientActivity, Observer {
                recyclerViewTopDoctor.layoutManager =
                    LinearLayoutManager(this@MainPatientActivity, LinearLayoutManager.HORIZONTAL, false)
                recyclerViewTopDoctor.adapter = TopDoctorAdapter(it)
                progressBarTopDoctor.visibility = View.GONE
            })
            viewModel.loadDoctors()

            doctorListTxt.setOnClickListener {
                startActivity(Intent(this@MainPatientActivity, TopDoctorsActivity::class.java))
            }
        }
    }

    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE
        viewModel.category.observe(this, Observer {
            binding.viewCategory.layoutManager =
                LinearLayoutManager(this@MainPatientActivity, LinearLayoutManager.HORIZONTAL, false)
            binding.viewCategory.adapter = CategoryAdapter(it)
            binding.progressBarCategory.visibility = View.GONE
        })
        viewModel.loadCategory()
    }
}