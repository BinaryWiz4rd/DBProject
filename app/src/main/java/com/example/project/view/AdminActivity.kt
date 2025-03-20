package com.example.project.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.google.firebase.firestore.FirebaseFirestore

// AdminActivity.kt
class AdminActivity : AppCompatActivity() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var btnAddDoctor: Button
    private lateinit var btnAddPatient: Button
    private val usersList = mutableListOf<User>()
    private lateinit var adapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        rvUsers = findViewById(R.id.rvUsers)
        btnAddDoctor = findViewById(R.id.btnAddDoctor)
        btnAddPatient = findViewById(R.id.btnAddPatient)

        adapter = UsersAdapter(usersList)
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = adapter

        // Otwieranie formularza dodawania użytkownika
        btnAddDoctor.setOnClickListener {
            val intent = Intent(this, AddUserActivity::class.java)
            intent.putExtra("role", "doctor")
            startActivity(intent)
        }
        btnAddPatient.setOnClickListener {
            val intent = Intent(this, AddUserActivity::class.java)
            intent.putExtra("role", "patient")
            startActivity(intent)
        }

        // Pobieranie listy użytkowników z Firestore
        fetchUsers()
    }

    private fun fetchUsers() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminActivity", "Błąd podczas pobierania użytkowników", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    usersList.clear()
                    for (doc in snapshot.documents) {
                        val user = doc.toObject(User::class.java)
                        if (user != null) {
                            usersList.add(user)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}
