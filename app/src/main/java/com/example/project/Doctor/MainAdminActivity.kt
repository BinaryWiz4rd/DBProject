package com.example.project.Doctor

import android.adservices.ondevicepersonalization.UserData
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

class MainAdminActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth


    private var currentFilter = "doctors"
    private val usersList = mutableListOf<UserData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_admin)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()


    }
}