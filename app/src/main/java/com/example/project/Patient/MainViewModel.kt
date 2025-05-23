package com.example.project.Patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var categoryListener: ListenerRegistration? = null
    private var doctorsListener: ListenerRegistration? = null

    private val _category = MutableLiveData<MutableList<CategoryModel>>()
    private val _doctors = MutableLiveData<MutableList<DoctorsModel>>()

    val category: LiveData<MutableList<CategoryModel>> = _category
    val doctors: LiveData<MutableList<DoctorsModel>> = _doctors

    fun loadCategory() {
        // Remove any existing listener
        categoryListener?.remove()
        
        // Create a new listener for the categories collection
        categoryListener = db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val lists = mutableListOf<CategoryModel>()
                    for (document in snapshot.documents) {
                        val category = document.toObject(CategoryModel::class.java)
                        if (category != null) {
                            lists.add(category)
                        }
                    }
                    _category.value = lists
                }
            }
    }

    fun loadDoctors() {
        // Remove any existing listener
        doctorsListener?.remove()
        
        // Create a new listener for the doctors collection
        doctorsListener = db.collection("doctors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val lists = mutableListOf<DoctorsModel>()
                    for (document in snapshot.documents) {
                        val doctor = document.toObject(DoctorsModel::class.java)
                        if (doctor != null) {
                            lists.add(doctor)
                        }
                    }
                    _doctors.value = lists
                }
            }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Remove listeners when ViewModel is cleared
        categoryListener?.remove()
        doctorsListener?.remove()
    }
}