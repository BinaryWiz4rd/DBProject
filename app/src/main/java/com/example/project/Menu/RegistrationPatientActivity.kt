package com.example.project.Menu

import com.example.project.R
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegistrationPatientActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etDateOfBirth: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogIn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_patient)

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etDateOfBirth = findViewById(R.id.etDateOfBirth)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogIn = findViewById(R.id.btnLogIn)

        btnRegister.setOnClickListener {
            registerPatient()
        }

        btnLogIn.setOnClickListener {
            Toast.makeText(
                this,
                "Przejdź do logowania",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun registerPatient() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val dateOfBirthStr = etDateOfBirth.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Walidacja pól
        if (firstName.isEmpty() || lastName.isEmpty() || dateOfBirthStr.isEmpty() ||
            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
        ) {
            Toast.makeText(this, "Proszę wypełnić wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }

        // Sprawdzenie zgodności haseł
        if (password != confirmPassword) {
            Toast.makeText(this, "Hasła nie są identyczne", Toast.LENGTH_SHORT).show()
            return
        }

        // Parsowanie daty urodzenia
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateOfBirth: Date
        try {
            dateOfBirth = sdf.parse(dateOfBirthStr)
        } catch (e: ParseException) {
            Toast.makeText(this, "Nieprawidłowy format daty (użyj yyyy-MM-dd)", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Sprawdzenie, czy pacjent jest pełnoletni (18 lat lub więcej)
        if (!isAdult(dateOfBirth)) {
            Toast.makeText(
                this,
                "Musisz być pełnoletni, aby korzystać z aplikacji",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Logika rejestracji – np. zapis danych lub przejście do kolejnego ekranu
        Toast.makeText(this, "Rejestracja zakończona sukcesem", Toast.LENGTH_SHORT).show()
    }

    // Metoda sprawdzająca, czy użytkownik jest pełnoletni
    private fun isAdult(dateOfBirth: Date): Boolean {
        val dob = Calendar.getInstance().apply { time = dateOfBirth }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age >= 18
    }
}
