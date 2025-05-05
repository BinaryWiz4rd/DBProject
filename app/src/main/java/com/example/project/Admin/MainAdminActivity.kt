package com.example.project.Admin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.appcompat.widget.SearchView

class MainAdminActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var chipDoctors: Chip
    private lateinit var chipPatients: Chip
    private lateinit var chipAdmins: Chip
    private lateinit var filterAdd: Chip
    private lateinit var filterEdit: Chip
    private lateinit var filterDelete: Chip
    private lateinit var searchView: SearchView

    private var currentDoctorAdapter: DoctorAdapter? = null
    private var currentPatientAdapter: PatientAdapter? = null
    private var currentAdminAdapter: AdminAdapter? = null
    private val doctorsList = mutableListOf<Doctor>()
    private val patientsList = mutableListOf<Patient>()
    private val adminsList = mutableListOf<Admin>()
    private var allDoctorsList = listOf<Doctor>()
    private var allPatientsList = listOf<Patient>()
    private var allAdminsList = listOf<Admin>()

    private var currentUserType: String = "doctors"
    private var currentFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_admin)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        chipDoctors = findViewById(R.id.chip_doctors)
        chipPatients = findViewById(R.id.chip_patients)
        chipAdmins = findViewById(R.id.chip_admins)
        filterAdd = findViewById(R.id.filter_add)
        filterEdit = findViewById(R.id.filter_edit)
        filterDelete = findViewById(R.id.filter_delete)

        val searchViewNullable = findViewById<androidx.appcompat.widget.SearchView>(R.id.main_search_view)
        if (searchViewNullable is SearchView) {
            searchView = searchViewNullable
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterList(newText)
                    return true
                }
            })
        } else {
            Log.e("MainAdmin", "Error: Could not find or cast SearchView with ID main_search_view")
            Toast.makeText(this, "Error initializing search view.", Toast.LENGTH_LONG).show()
        }


        chipDoctors.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentUserType = "doctors"
                applyFilters()
            }
        }

        chipPatients.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentUserType = "patients"
                applyFilters()
            }
        }

        chipAdmins.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentUserType = "admins"
                applyFilters()
            }
        }

        filterAdd.setOnCheckedChangeListener { _, isChecked ->
            currentFilter = if (isChecked) "add" else null
            applyFilters()
        }

        filterEdit.setOnCheckedChangeListener { _, isChecked ->
            currentFilter = if (isChecked) "edit" else null
            applyFilters()
        }

        filterDelete.setOnCheckedChangeListener { _, isChecked ->
            currentFilter = if (isChecked) "delete" else null
            applyFilters()
        }

        applyFilters()
    }

    private fun applyFilters() {
        when (currentUserType) {
            "doctors" -> fetchFilteredDoctors()
            "patients" -> fetchFilteredPatients()
            "admins" -> fetchFilteredAdmins()
        }
    }

    private fun fetchFilteredDoctors() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var query = db.collection("doctors") as Query
                if (currentFilter == "add") {
                    query = query.whereEqualTo("add", true)
                } else if (currentFilter == "delete") {
                    query = query.whereEqualTo("delete", true)
                } else if (currentFilter == "edit") {
                    query = query.whereEqualTo("edit", true)
                }

                val querySnapshot = query.get().await()
                val fetchedDoctors = querySnapshot.toObjects(Doctor::class.java).map { it.apply { uid = querySnapshot.documents[querySnapshot.documents.indexOfFirst { doc -> doc.toObject(Doctor::class.java) == it }].id } }

                withContext(Dispatchers.Main) {
                    allDoctorsList = fetchedDoctors
                    doctorsList.clear()
                    doctorsList.addAll(fetchedDoctors)
                    currentDoctorAdapter = DoctorAdapter(doctorsList) { userId, action, approved ->
                        handleUserAction(userId, action, approved, "doctors")
                    }
                    recyclerView.adapter = currentDoctorAdapter
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.w("MainAdmin", "Error getting filtered doctors.", e)
                    Toast.makeText(this@MainAdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchFilteredPatients() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var query = db.collection("patients") as Query
                if (currentFilter == "add") {
                    query = query.whereEqualTo("add", true)
                } else if (currentFilter == "delete") {
                    query = query.whereEqualTo("delete", true)
                } else if (currentFilter == "edit") {
                    query = query.whereEqualTo("edit", true)
                }

                val querySnapshot = query.get().await()
                val fetchedPatients = querySnapshot.toObjects(Patient::class.java).map { it.apply { uid = querySnapshot.documents[querySnapshot.documents.indexOfFirst { doc -> doc.toObject(Patient::class.java) == it }].id } }

                withContext(Dispatchers.Main) {
                    allPatientsList = fetchedPatients
                    patientsList.clear()
                    patientsList.addAll(fetchedPatients)
                    currentPatientAdapter = PatientAdapter(patientsList) { userId, action, approved ->
                        handleUserAction(userId, action, approved, "patients")
                    }
                    recyclerView.adapter = currentPatientAdapter
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.w("MainAdmin", "Error getting filtered patients.", e)
                    Toast.makeText(this@MainAdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchFilteredAdmins() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var query = db.collection("admins") as Query
                if (currentFilter == "add") {
                    query = query.whereEqualTo("add", true)
                } else if (currentFilter == "delete") {
                    query = query.whereEqualTo("delete", true)
                } else if (currentFilter == "edit") {
                    query = query.whereEqualTo("edit", true)
                }

                val querySnapshot = query.get().await()
                val fetchedAdmins = querySnapshot.toObjects(Admin::class.java).map { it.apply { uid = querySnapshot.documents[querySnapshot.documents.indexOfFirst { doc -> doc.toObject(Admin::class.java) == it }].id } }

                withContext(Dispatchers.Main) {
                    allAdminsList = fetchedAdmins
                    adminsList.clear()
                    adminsList.addAll(fetchedAdmins)
                    currentAdminAdapter = AdminAdapter(adminsList) { userId, action, approved ->
                        handleUserAction(userId, action, approved, "admins")
                    }
                    recyclerView.adapter = currentAdminAdapter
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.w("MainAdmin", "Error getting filtered admins.", e)
                    Toast.makeText(this@MainAdminActivity, "Error getting filtered admins.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterList(query: String?) {
        if (!query.isNullOrEmpty()) {
            when (currentUserType) {
                "doctors" -> {
                    val filteredList = allDoctorsList.filter {
                        it.firstName.contains(query, ignoreCase = true) ||
                                it.lastName.contains(query, ignoreCase = true) ||
                                it.email.contains(query, ignoreCase = true)
                    }
                    currentDoctorAdapter?.updateList(filteredList)
                }
                "patients" -> {
                    val filteredList = allPatientsList.filter {
                        it.firstName.contains(query, ignoreCase = true) ||
                                it.lastName.contains(query, ignoreCase = true) ||
                                it.email.contains(query, ignoreCase = true)
                    }
                    currentPatientAdapter?.updateList(filteredList)
                }
                "admins" -> {
                    val filteredList = allAdminsList.filter {
                        it.firstName.contains(query, ignoreCase = true) ||
                                it.lastName.contains(query, ignoreCase = true) ||
                                it.email.contains(query, ignoreCase = true)
                    }
                    currentAdminAdapter?.updateList(filteredList)
                }
            }
        } else {
            when (currentUserType) {
                "doctors" -> currentDoctorAdapter?.updateList(allDoctorsList)
                "patients" -> currentPatientAdapter?.updateList(allPatientsList)
                "admins" -> currentAdminAdapter?.updateList(allAdminsList)
            }
        }
    }

    private fun handleUserAction(userId: String, action: String, approved: Boolean, collection: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection(collection).document(userId)
                    .update(action, !approved)
                    .await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainAdminActivity, "${collection.dropLast(1)} ${if (approved) "approved" else "rejected"}", Toast.LENGTH_SHORT).show()
                    applyFilters()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainAdminActivity, "Error updating ${collection.dropLast(1)}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}