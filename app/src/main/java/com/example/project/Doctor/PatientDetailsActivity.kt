package com.example.project.doctor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity for displaying patient details and their editable medical card.
 * Allows doctors to view patient information and manage diagnosis, medications, treatment, and notes.
 */
class PatientDetailAndMedicalCardActivity : AppCompatActivity() {

    /**
     * TextView for displaying the patient's name.
     */
    private lateinit var nameTextView: TextView
    /**
     * TextView for displaying the patient's surname.
     */
    private lateinit var surnameTextView: TextView
    /**
     * TextView for displaying the patient's PESEL.
     */
    private lateinit var peselTextView: TextView
    /**
     * TextView for displaying the patient's address.
     */
    private lateinit var addressTextView: TextView
    /**
     * TextView for displaying the patient's phone number.
     */
    private lateinit var phoneTextView: TextView

    // Declarations for medical card views (using EditText for editing)
    /**
     * EditText for entering/displaying the diagnosis.
     */
    private lateinit var diagnosisEditText: EditText
    /**
     * EditText for entering/displaying medications.
     */
    private lateinit var medicationsEditText: EditText
    /**
     * EditText for entering/displaying the treatment plan.
     */
    private lateinit var treatmentEditText: EditText
    /**
     * EditText for entering/displaying doctor's notes.
     */
    private lateinit var notesEditText: EditText
    /**
     * Button to save changes to the medical card.
     */
    private lateinit var saveMedicalCardButton: Button

    /**
     * Firebase Firestore database instance.
     */
    private lateinit var db: FirebaseFirestore

    /**
     * The PESEL number of the patient currently being viewed.
     */
    private lateinit var patientPesel: String

    /**
     * Initializes the activity: sets up views, retrieves patient data from Intent,
     * loads medical card data from Firestore, and sets up save button listener.
     * @param savedInstanceState If the activity is re-initialized, this Bundle contains previous state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_details)

        db = FirebaseFirestore.getInstance()

        nameTextView = findViewById(R.id.PatientName)
        surnameTextView = findViewById(R.id.patientSurname)
        peselTextView = findViewById(R.id.patientPesel)
        addressTextView = findViewById(R.id.patientAddress)
        phoneTextView = findViewById(R.id.patientPhone)

        diagnosisEditText = findViewById(R.id.medicalCardDiagnosis)
        medicationsEditText = findViewById(R.id.medicalCardMedications)
        treatmentEditText = findViewById(R.id.medicalCardTreatment)
        notesEditText = findViewById(R.id.medicalCardNotes)
        saveMedicalCardButton = findViewById(R.id.btnSettings)

        val name = intent.getStringExtra("NAME") ?: "Unknown"
        val surname = intent.getStringExtra("SURNAME") ?: "Unknown"
        patientPesel = intent.getStringExtra("PESEL") ?: "Unknown"
        val address = intent.getStringExtra("ADDRESS") ?: "Unknown"
        val phone = intent.getStringExtra("PHONE") ?: "Unknown"

        if (patientPesel == "Unknown") {
            Toast.makeText(this, "Error: Patient PESEL not provided.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        nameTextView.text = "Name: $name"
        surnameTextView.text = "Surname: $surname"
        peselTextView.text = "PESEL: $patientPesel"
        addressTextView.text = "Address: $address"
        phoneTextView.text = "Phone: $phone"

        loadMedicalCard(patientPesel)

        saveMedicalCardButton.setOnClickListener {
            saveMedicalCard(patientPesel)
        }
    }

    /**
     * Loads the medical card data for a given PESEL from Firestore.
     * Populates the EditText fields or shows a toast if no record is found/an error occurs.
     * @param pesel The PESEL number of the patient.
     */
    private fun loadMedicalCard(pesel: String) {
        db.collection("medical_cards").document(pesel)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    diagnosisEditText.setText(document.getString("diagnosis") ?: "")
                    medicationsEditText.setText(document.getString("medications") ?: "")
                    treatmentEditText.setText(document.getString("treatment") ?: "")
                    notesEditText.setText(document.getString("notes") ?: "")
                    Toast.makeText(this, "Medical record loaded.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No medical record found for this patient. You can create one now.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading medical data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Saves the current medical card data from EditText fields to Firestore.
     * Stores data in the 'medical_cards' collection using PESEL as the document ID.
     * @param pesel The PESEL number of the patient.
     */
    private fun saveMedicalCard(pesel: String) {
        val diagnosis = diagnosisEditText.text.toString()
        val medications = medicationsEditText.text.toString()
        val treatment = treatmentEditText.text.toString()
        val notes = notesEditText.text.toString()

        val medicalCard = hashMapOf(
            "diagnosis" to diagnosis,
            "medications" to medications,
            "treatment" to treatment,
            "notes" to notes
        )

        db.collection("medical_cards").document(pesel)
            .set(medicalCard)
            .addOnSuccessListener {
                Toast.makeText(this, "Medical card saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving medical data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}