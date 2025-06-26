package com.example.project.Patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * [MainViewModel] serves as a ViewModel for fetching and managing data related to
 * categories and doctors from Firestore.
 *
 * It exposes [LiveData] objects for categories and doctors, allowing UI components
 * to observe changes in real-time. It uses Firestore snapshot listeners to keep
 * the data up-to-date.
 */
class MainViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var categoryListener: ListenerRegistration? = null
    private var doctorsListener: ListenerRegistration? = null

    private val _category = MutableLiveData<MutableList<CategoryModel>>()
    private val _doctors = MutableLiveData<MutableList<DoctorsModel>>()

    /**
     * [LiveData] holding a mutable list of [CategoryModel] objects.
     * Observers will be notified when the list of categories changes.
     */
    val category: LiveData<MutableList<CategoryModel>> = _category
    /**
     * [LiveData] holding a mutable list of [DoctorsModel] objects.
     * Observers will be notified when the list of doctors changes.
     */
    val doctors: LiveData<MutableList<DoctorsModel>> = _doctors

    /**
     * Loads categories from the "categories" collection in Firestore.
     *
     * It sets up a real-time listener using `addSnapshotListener` to continuously
     * update the `_category` LiveData with the latest data. Any previously active
     * listener for categories will be removed before a new one is set up.
     */
    fun loadCategory() {
        categoryListener?.remove()

        categoryListener = db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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

    /**
     * Loads doctors from the "doctors" collection in Firestore.
     *
     * It sets up a real-time listener using `addSnapshotListener` to continuously
     * update the `_doctors` LiveData with the latest data. Any previously active
     * listener for doctors will be removed before a new one is set up.
     */
    fun loadDoctors() {
        doctorsListener?.remove()

        doctorsListener = db.collection("doctors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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

    /**
     * Called when the ViewModel is no longer used and will be destroyed.
     *
     * This method is overridden to ensure that all active Firestore snapshot listeners
     * ([categoryListener], [doctorsListener]) are removed to prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        categoryListener?.remove()
        doctorsListener?.remove()
    }
}