package com.example.project.Doctor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText // Zmieniono z TextView na EditText dla edycji
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R
import com.google.firebase.firestore.FirebaseFirestore

// Połączona aktywność, która wyświetla szczegóły pacjenta i jego kartę medyczną
class PatientDetailAndMedicalCardActivity : AppCompatActivity() {

    // Deklaracje dla widoków szczegółów pacjenta
    private lateinit var nameTextView: TextView
    private lateinit var surnameTextView: TextView
    private lateinit var peselTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var phoneTextView: TextView

    // Deklaracje dla widoków karty medycznej (używamy EditText do edycji)
    private lateinit var diagnosisEditText: EditText
    private lateinit var medicationsEditText: EditText
    private lateinit var treatmentEditText: EditText
    private lateinit var notesEditText: EditText
    private lateinit var saveMedicalCardButton: Button

    private lateinit var db: FirebaseFirestore

    private lateinit var patientPesel: String

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

        // Pobranie danych pacjenta przekazanych przez Intent
        val name = intent.getStringExtra("NAME") ?: "Unknown"
        val surname = intent.getStringExtra("SURNAME") ?: "Unknown"
        patientPesel = intent.getStringExtra("PESEL") ?: "Unknown" // Zapisz PESEL do zmiennej klasy
        val address = intent.getStringExtra("ADDRESS") ?: "Unknown"
        val phone = intent.getStringExtra("PHONE") ?: "Unknown"

        // Sprawdzenie czy PESEL został poprawnie przekazany
        if (patientPesel == "Unknown") {
            Toast.makeText(this, "Error: Patient PESEL not provided.", Toast.LENGTH_LONG).show()
            // Można rozważyć zamknięcie aktywności lub inne działanie
            finish()
            return // Zakończ onCreate, jeśli PESEL jest nieznany
        }

        // Ustawienie tekstu dla widoków szczegółów pacjenta
        nameTextView.text = "Name: $name"
        surnameTextView.text = "Surname: $surname"
        peselTextView.text = "PESEL: $patientPesel"
        addressTextView.text = "Address: $address"
        phoneTextView.text = "Phone: $phone"

        // Załadowanie danych karty medycznej z Firestore
        loadMedicalCard(patientPesel)

        // Ustawienie listenera dla przycisku zapisu karty medycznej
        saveMedicalCardButton.setOnClickListener {
            saveMedicalCard(patientPesel)
        }
    }

    private fun loadMedicalCard(pesel: String) {
        db.collection("medical_cards").document(pesel)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Ustawienie pobranych danych w polach EditText
                    diagnosisEditText.setText(document.getString("diagnosis") ?: "")
                    medicationsEditText.setText(document.getString("medications") ?: "")
                    treatmentEditText.setText(document.getString("treatment") ?: "")
                    notesEditText.setText(document.getString("notes") ?: "")
                    Toast.makeText(this, "Medical record loaded.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No medical record found for this patient. You can create one now.", Toast.LENGTH_SHORT).show()
                    // Pola EditText pozostaną puste, gotowe do wypełnienia
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading medical data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveMedicalCard(pesel: String) {
        // Pobranie danych z pól EditText
        val diagnosis = diagnosisEditText.text.toString()
        val medications = medicationsEditText.text.toString()
        val treatment = treatmentEditText.text.toString()
        val notes = notesEditText.text.toString()

        // Stworzenie mapy danych do zapisania
        val medicalCard = hashMapOf(
            "diagnosis" to diagnosis,
            "medications" to medications,
            "treatment" to treatment,
            "notes" to notes
            // Możesz dodać inne pola, np. datę modyfikacji
            // "lastUpdated" to com.google.firebase.Timestamp.now()
        )

        // Zapisanie danych do Firestore w kolekcji 'medical_cards' używając PESEL jako ID dokumentu
        db.collection("medical_cards").document(pesel)
            .set(medicalCard) // Użyj .set() aby nadpisać lub stworzyć dokument
            .addOnSuccessListener {
                Toast.makeText(this, "Medical card saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving medical data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}