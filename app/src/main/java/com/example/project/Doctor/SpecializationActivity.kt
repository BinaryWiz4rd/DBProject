/**package com.example.project.Doctor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

class SpecializationActivity : AppCompatActivity(), SpecializationView {
    private lateinit var presenter: SpecializationPresenter
    private lateinit var adapter: SpecializationAdapter
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_specialization)

        isAdmin = intent.getBooleanExtra("IS_ADMIN", false)
        findViewById<AdminSidePanel>(R.id.adminSidePanel).showIfAdmin(isAdmin)
        presenter = SpecializationPresenter(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SpecializationAdapter(mutableListOf(), isAdmin, presenter::deleteSpecialization)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.addButton)?.setOnClickListener {
            val input = findViewById<EditText>(R.id.specializationInput).text.toString()
            if (input.isNotBlank()) presenter.addSpecialization(input)
        }

        presenter.loadSpecializations()
    }

    override fun showSpecializations(specializations: List<Specialization>) {
        adapter.updateData(specializations)
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }
}
*/