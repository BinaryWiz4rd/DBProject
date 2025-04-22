package com.example.project.Menu

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.project.R
// Zaimportuj Firebase Auth i Firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationAdminActivity : AppCompatActivity() {

    // Deklaracja pól EditText i Button
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button

    // Instancje Firebase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration_admin) // Upewnij się, że ta nazwa layoutu jest poprawna

        // Inicjalizacja Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Znajdź widoki po ID
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)

        // Ustawienie Listenera dla Insets (obsługa paska systemowego)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ustawienie Listenera dla przycisku rejestracji
        btnRegister.setOnClickListener {
            registerAdmin()
        }
    }

    private fun registerAdmin() {
        // Pobierz dane z pól EditText
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // --- Walidacja danych ---
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Wszystkie pola są wymagane", Toast.LENGTH_SHORT).show()
            return // Przerwij funkcję, jeśli któreś pole jest puste
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Hasła nie są zgodne", Toast.LENGTH_SHORT).show()
            // Opcjonalnie: wyczyść pola haseł lub ustaw na nich błąd
            etPassword.error = "Hasła niezgodne"
            etConfirmPassword.error = "Hasła niezgodne"
            // Wyczyść błędy z innych pól, jeśli były ustawione
            etEmail.error = null
            return // Przerwij funkcję
        }

        // Opcjonalnie: Dodatkowa walidacja (np. format email, długość hasła)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Niepoprawny format adresu email", Toast.LENGTH_SHORT).show()
            etEmail.error = "Niepoprawny format"
            // Wyczyść błędy z innych pól
            etPassword.error = null
            etConfirmPassword.error = null
            return
        }
        if (password.length < 6) { // Przykładowa minimalna długość hasła
            Toast.makeText(this, "Hasło musi mieć co najmniej 6 znaków", Toast.LENGTH_SHORT).show()
            etPassword.error = "Hasło za krótkie"
            // Wyczyść błędy z innych pól
            etEmail.error = null
            etConfirmPassword.error = null // Wyczyść też błąd potwierdzenia hasła
            return
        }

        // Wyczyść ewentualne błędy, jeśli walidacja przeszła
        etFirstName.error = null
        etLastName.error = null
        etEmail.error = null
        etPassword.error = null
        etConfirmPassword.error = null


        // --- Proces rejestracji (z użyciem Firebase) ---
        Toast.makeText(this, "Próba rejestracji...", Toast.LENGTH_SHORT).show()
        // Opcjonalnie: Zablokuj przycisk podczas rejestracji
        btnRegister.isEnabled = false


        // Użycie Firebase Authentication i Firestore
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Rejestracja w Auth powiodła się, pobierz ID użytkownika
                    val userId = firebaseAuth.currentUser?.uid
                    if (userId != null) {
                        // Stwórz mapę z danymi admina
                        val admin = hashMapOf(
                            "email" to email,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "role" to "admin" // Ustawienie roli
                        )

                        // Zapisz dane w Firestore w kolekcji 'admins' (lub np. 'users' z polem 'role')
                        db.collection("admins").document(userId).set(admin)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Rejestracja admina zakończona sukcesem!", Toast.LENGTH_LONG).show()
                                // Opcjonalnie: Przekieruj do innej aktywności lub zamknij tę
                                finish() // Zamknij aktywność po sukcesie
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Błąd zapisu danych admina: ${e.message}", Toast.LENGTH_LONG).show()
                                // Odblokuj przycisk w razie błędu zapisu do Firestore
                                btnRegister.isEnabled = true
                                // Opcjonalnie: Można rozważyć usunięcie użytkownika z Auth, jeśli zapis do Firestore się nie udał
                                // firebaseAuth.currentUser?.delete()
                            }
                    } else {
                        Toast.makeText(this, "Nie udało się uzyskać ID użytkownika po rejestracji.", Toast.LENGTH_LONG).show()
                        // Odblokuj przycisk
                        btnRegister.isEnabled = true
                    }
                } else {
                    // Rejestracja w Auth nie powiodła się
                    Toast.makeText(this, "Błąd rejestracji: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    // Odblokuj przycisk w razie błędu rejestracji
                    btnRegister.isEnabled = true
                }
            }


        /* // Usunięto logowanie jako że Firebase obsługuje rejestrację
         println("Imię: $firstName")
         println("Nazwisko: $lastName")
         println("Email: $email")
        */

    }
}