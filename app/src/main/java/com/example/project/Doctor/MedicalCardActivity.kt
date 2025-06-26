package com.example.project.doctor

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity for displaying and managing a patient's medical card.
 * Allows doctors to view, and potentially save/update, diagnosis, medications, treatment, and notes.
 */
class MedicalCardActivity : AppCompatActivity() {

    /**
     * Firebase Firestore database instance.
     */
    private lateinit var db: FirebaseFirestore
    /**
     * TextView to display the patient's name.
     */
    private lateinit var patientNameTextView: TextView
    /**
     * TextView to display the diagnosis.
     */
    private lateinit var diagnosisTextView: TextView
    /**
     * TextView to display medications.
     */
    private lateinit var medicationsTextView: TextView
    /**
     * TextView to display the treatment plan.
     */
    private lateinit var treatmentTextView: TextView
    /**
     * TextView to display doctor's notes.
     */
    private lateinit var notesTextView: TextView
    /**
     * Button to save changes to the medical card.
     */
    private lateinit var saveButton: Button

    /**
     * Initializes the activity, sets up Firebase, binds views, and loads medical card data.
     * @param savedInstanceState If the activity is re-initialized, this Bundle contains previous state.
     */
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

        loadMedicalCard(patientPesel)

        saveButton.setOnClickListener {
            saveMedicalCard(patientPesel)
        }
    }

    /**
     * Loads the medical card data for a given patient PESEL from Firestore.
     * Displays the data in the respective TextViews or a toast if no record is found/error occurs.
     * @param patientPesel The PESEL number of the patient.
     */
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

    /**
     * Saves the current medical card data to Firestore for a given patient PESEL.
     * Extracts text from TextViews, removes prefixes, and stores as a HashMap.
     * Displays a toast upon success or failure.
     * @param patientPesel The PESEL number of the patient.
     */
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
}