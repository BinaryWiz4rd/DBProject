package com.example.project.view

import com.example.project.model.Specialization

interface SpecializationView {
    fun showSpecializations(specializations: List<Specialization>)
    fun showMessage(message: String)
    fun showError(error: String)
}
