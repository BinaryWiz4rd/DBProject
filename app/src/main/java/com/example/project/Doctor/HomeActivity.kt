/* package com.example.project.Doctor // Używam Twojej nazwy pakietu

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
// Importy dla Appointment i ScheduleAdapter muszą pasować do Twojej struktury projektu
// Upewnij się, że te pliki istnieją i są poprawne w tym pakiecie
import com.example.project.Doctor.Appointment
import com.example.project.Doctor.ScheduleAdapter
// Upewnij się, że plik layoutu nazywa się activity_doctor_home.xml
import com.example.project.databinding.ActivityDoctorHomeBinding
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    // Upewnij się, że nazwa bindingu odpowiada nazwie pliku layoutu (activity_doctor_home.xml -> ActivityDoctorHomeBinding)
    private lateinit var binding: ActivityDoctorHomeBinding
    private lateinit var scheduleAdapter: ScheduleAdapter
    private var allAppointments: List<Appointment> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGreeting()
        setupUI()
        loadScheduleData() // Ładuje dane testowe
        setupSearch()
        setupWidget()
    }

    private fun setupGreeting() {
        val userName = getUserFirstName() // Pobiera imię (obecnie placeholder)
        // Upewnij się, że ID textViewGreeting istnieje w pliku activity_doctor_home.xml
        if (userName != null) {
            binding.textViewGreeting.text = "Cześć, $userName!"
        } else {
            binding.textViewGreeting.text = "Witaj!"
        }
    }

    private fun getUserFirstName(): String? {
        // -------> WAŻNE: ZASTĄP TO PRAWDZIWĄ LOGIKĄ POBIERANIA IMIENIA <-------
        // Ta funkcja musi zostać zaimplementowana, aby pobierać rzeczywiste imię zalogowanego lekarza
        // Przykład: return getSharedPreferences("UserData", Context.MODE_PRIVATE).getString("USER_FIRST_NAME", null)
        return "Doktorze" // Tylko przykład!
        // --------------------------------------------------------------------
    }

    private fun setupUI() {
        // Upewnij się, że klasa ScheduleAdapter istnieje i działa poprawnie
        scheduleAdapter = ScheduleAdapter(emptyList()) { appointment ->
            Toast.makeText(this, "Kliknięto: ${appointment.patientName}", Toast.LENGTH_SHORT).show()
            // TODO: Dodać logikę nawigacji do szczegółów wizyty po kliknięciu
            // np. Intent do innej aktywności
        }
        // Upewnij się, że ID recyclerViewSchedule istnieje w pliku activity_doctor_home.xml
        binding.recyclerViewSchedule.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = scheduleAdapter
        }

        val today = LocalDate.now()
        val dateFormatter = try {
            // Użyj polskiego locale dla poprawnego formatowania miesiąca
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("pl", "PL"))
        } catch (e: IllegalArgumentException) {
            // Fallback na domyślny format, jeśli locale "pl" nie jest wspierane w pełni
            Log.w("HomeActivity", "Locale 'pl_PL' not fully supported for date format, using default.")
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
        }

        // Upewnij się, że ID textViewScheduleTitle istnieje w pliku activity_doctor_home.xml
        // Znak '?' zapewnia, że kod się nie wywali, jeśli widok nie zostanie znaleziony (choć nie powinien przy ViewBinding)
        binding.textViewScheduleTitle?.text = "Plan na dziś (${today.format(dateFormatter)})"
    }


    private fun loadScheduleData() {
        // TODO: Załaduj dane z prawdziwego źródła (np. Firebase, API, lokalna baza danych)
        // Obecnie używane są dane testowe. Upewnij się, że konstruktor Appointment
        // zgadza się z przekazywanymi typami (ID, LocalTime, Nazwa, Szczegóły).
        // To tutaj występował błąd, jeśli Appointment oczekiwał String zamiast LocalTime.
        allAppointments = listOf(
            Appointment("1", LocalTime.of(9, 0), "Anna Nowak", "Kontrola"),
            Appointment("2", LocalTime.of(10, 30), "Jan Kowalski", "Ból gardła"),
            Appointment("3", LocalTime.of(9, 45), "Katarzyna Zielińska", "Szczepienie"),
            Appointment("4", LocalTime.of(11, 15), "Piotr Wiśniewski", "Konsultacja wyników"),
            Appointment("5", LocalTime.of(13, 0), "Zofia Michalska", "Badanie okresowe")
        )
        scheduleAdapter.updateData(allAppointments) // Przekaż dane do adaptera
    }

    private fun setupSearch() {
        // Upewnij się, że ID editTextSearch istnieje w pliku activity_doctor_home.xml
        binding.editTextSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                performSearch(query) // Wywołaj funkcję wyszukiwania
                // Ukrycie klawiatury po wyszukiwaniu
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                true // Zasygnalizuj, że zdarzenie zostało obsłużone
            } else {
                false // Zasygnalizuj, że zdarzenie nie zostało obsłużone
            }
        }
    }

    private fun performSearch(query: String) {
        // TODO: Zaimplementuj faktyczną logikę wyszukiwania pacjentów
        // Obecnie tylko wyświetla log i Toast.
        // Wyszukiwanie powinno np. filtrować listę pacjentów lub odpytywać bazę danych/API.
        if (query.isNotEmpty()) {
            Log.d("HomeActivity", "Wyszukiwanie pacjenta: $query")
            Toast.makeText(this, "Wyszukiwanie: $query", Toast.LENGTH_SHORT).show()
            // Tutaj logika filtrowania lub przejścia do ekranu wyników
        } else {
            Log.d("HomeActivity", "Wyszukiwanie anulowane lub puste")
        }
    }

    private fun setupWidget() {
        // TODO: Zaimplementuj lub dostosuj logikę dla widgetu (np. następna wizyta)
        val now = LocalTime.now()
        // Ten kod zakłada, że Appointment ma pole 'time' typu LocalTime
        val nextAppointment = allAppointments
            .filter { it.time.isAfter(now) } // Filtruj przyszłe wizyty
            .minByOrNull { it.time } // Znajdź najwcześniejszą

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        // Upewnij się, że ID textViewWidgetTitle i textViewWidgetContent istnieją w pliku activity_doctor_home.xml
        if (nextAppointment != null) {
            binding.textViewWidgetTitle.text = "Następna wizyta (${nextAppointment.time.format(timeFormatter)})"
            binding.textViewWidgetContent.text = "${nextAppointment.patientName} - ${nextAppointment.details}"
        } else {
            binding.textViewWidgetTitle.text = "Następna wizyta"
            binding.textViewWidgetContent.text = "Brak kolejnych wizyt na dziś."
        }
    }
} */