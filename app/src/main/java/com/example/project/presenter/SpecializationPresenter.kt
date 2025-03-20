package com.example.project.presenter

import com.example.project.model.Specialization
import com.example.project.view.SpecializationView
import com.google.firebase.database.FirebaseDatabase

class SpecializationPresenter(private val view: SpecializationView) {
    private val database = FirebaseDatabase.getInstance().reference.child("specializations")

    fun loadSpecializations() {
        database.get().addOnSuccessListener { snapshot ->
            val specializations = snapshot.children.mapNotNull { it.getValue(Specialization::class.java) }
            view.showSpecializations(specializations)
        }.addOnFailureListener {
            view.showError("Failed to load specializations")
        }
    }

    fun addSpecialization(name: String) {
        val id = database.push().key ?: return
        val specialization = Specialization(id, name)
        database.child(id).setValue(specialization)
            .addOnSuccessListener { view.showMessage("Specialization added") }
            .addOnFailureListener { view.showError("Failed to add specialization") }
    }

    fun deleteSpecialization(id: String) {
        database.child(id).removeValue()
            .addOnSuccessListener { view.showMessage("Specialization deleted") }
            .addOnFailureListener { view.showError("Failed to delete specialization") }
    }
}
