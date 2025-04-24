/* package com.example.project.Doctor

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.google.firebase.firestore.FirebaseFirestore

class MedicalCardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var patientNameTextView: TextView
    private lateinit var diagnosisTextView: TextView
    private lateinit var medicationsTextView: TextView
    private lateinit var treatmentTextView: TextView
    private lateinit var notesTextView: TextView
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medical_card_doctor)

        db = FirebaseFirestore.getInstance()

        patientNameTextView = findViewById(R.id.medicalCardPatientName)
        diagnosisTextView = findViewById(R.id.medicalCardDiagnosis)
        medicationsTextView = findViewById(R.id.medicalCardMedications)
        treatmentTextView = findViewById(R.id.medicalCardTreatment)
        notesTextView = findViewById(R.id.medicalCardNotes)
        saveButton = findViewById(R.id.saveMedicalCardButton)

        val patientName = intent.getStringExtra("NAME") ?: "Unknown"
        val patientSurname = intent.getStringExtra("SURNAME") ?: "Unknown"
        val patientPesel = intent.getStringExtra("PESEL") ?: "Unknown"

        patientNameTextView.text = "Patient: $patientName $patientSurname"

        // Pobieranie danych pacjenta z Firestore
        loadMedicalCard(patientPesel)

        saveButton.setOnClickListener {
            saveMedicalCard(patientPesel)
        }
    }

    private fun loadMedicalCard(patientPesel: String) {
        db.collection("medical_cards").document(patientPesel)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    diagnosisTextView.text = "Diagnosis: ${document.getString("diagnosis") ?: "Not Available"}"
                    medicationsTextView.text = "Medications: ${document.getString("medications") ?: "Not Available"}"
                    treatmentTextView.text = "Treatment Plan: ${document.getString("treatment") ?: "Not Available"}"
                    notesTextView.text = "Doctor Notes: ${document.getString("notes") ?: "Not Available"}"
                } else {
                    Toast.makeText(this, "No medical record found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveMedicalCard(patientPesel: String) {
        val medicalCard = hashMapOf(
            "diagnosis" to diagnosisTextView.text.toString().removePrefix("Diagnosis: "),
            "medications" to medicationsTextView.text.toString().removePrefix("Medications: "),
            "treatment" to treatmentTextView.text.toString().removePrefix("Treatment Plan: "),
            "notes" to notesTextView.text.toString().removePrefix("Doctor Notes: ")
        )

        db.collection("medical_cards").document(patientPesel)
            .set(medicalCard)
            .addOnSuccessListener {
                Toast.makeText(this, "Medical card saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
} */
