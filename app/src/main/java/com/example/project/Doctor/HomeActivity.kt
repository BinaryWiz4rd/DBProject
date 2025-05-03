package com.example.project.Doctor // Używam Twojej nazwy pakietu
// home
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
// Importy dla Appointment i ScheduleAdapter muszą pasować do Twojej struktury projektu
import com.example.project.Doctor.ScheduleAdapter // Corrected: Using ScheduleAdapter
// Upewnij się, że plik layoutu nazywa się activity_doctor_home.xml
import com.example.project.databinding.ActivityDoctorHomeBinding
// Importy dla Doctor i Patient z ich pakietu
import com.example.project.Admin.Doctor
import com.example.project.Admin.Patient
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorHomeBinding
    private lateinit var scheduleAdapter: ScheduleAdapter // Using ScheduleAdapter
    private var allAppointments: List<Appointment> = listOf() // Corrected: Using singular Appointment
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGreeting()
        setupUI()
        loadScheduleData() // Loads test data (corrected object creation)
        setupSearch()
        setupWidget()
    }

    private fun setupGreeting() {
        val userName = getUserFirstName()
        if (userName != null) {
            binding.textViewGreeting.text = "Cześć, $userName!"
        } else {
            binding.textViewGreeting.text = "Witaj!"
        }
    }

    private fun getUserFirstName(): String? {
        // -------> WAŻNE: ZASTĄP TO PRAWDZIWĄ LOGIKĄ POBIERANIA IMIENIA LEKARZA <-------
        return "Jan" // Example
        // --------------------------------------------------------------------
    }

    private fun setupUI() {
        // Initialize the corrected ScheduleAdapter
        scheduleAdapter = ScheduleAdapter { appointment ->
            // Click listener lambda implementation
            Toast.makeText(this, "Kliknięto wizytę: ${appointment.patient.firstName} ${appointment.patient.lastName}", Toast.LENGTH_SHORT).show()
            // TODO: Add navigation logic here
        }
        binding.recyclerViewSchedule.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = scheduleAdapter // Set the ScheduleAdapter
        }

        val today = LocalDate.now()
        val displayDateFormatter = try {
            DateTimeFormatter.ofPattern("d MMMM isobar", Locale("pl", "PL"))
        } catch (e: IllegalArgumentException) {
            Log.w("HomeActivity", "Locale 'pl_PL' not fully supported for date format, using default.")
            DateTimeFormatter.ofPattern("d MMMM isobar", Locale.getDefault())
        }
        binding.textViewScheduleTitle?.text = "Plan na dziś (${today.format(displayDateFormatter)})"
    }


    private fun loadScheduleData() {
        // TODO: Load real Doctor, Patient, and Appointment data from your data source

        // --- Example data matching the data class definitions ---
        val thisDoctor = Doctor(
            email = "jan.kowalski@example.com",
            firstName = "Jan",
            lastName = "Kowalski",
            pwz = "1234567",
            specialization = "Kardiolog"
        )
        val patient1 = Patient(
            email = "anna.nowak@example.com",
            firstName = "Anna",
            lastName = "Nowak",
            dateOfBirth = "1990-05-15"
        )
        val patient2 = Patient(
            email = "piotr.wisniewski@example.com",
            firstName = "Piotr",
            lastName = "Wiśniewski",
            dateOfBirth = "1985-11-22"
        )
        val patient3 = Patient(
            email = "zofia.michalska@example.com",
            firstName = "Zofia",
            lastName = "Michalska",
            dateOfBirth = "1978-02-10"
        )

        val todayDateStr = LocalDate.now().format(dateFormatter)
        // -------------------------------------------------------------------

        // Create list using the correct Appointment (singular) constructor
        allAppointments = listOf(
            Appointment(thisDoctor, patient1, todayDateStr, LocalTime.of(9, 0).format(timeFormatter)),
            Appointment(thisDoctor, patient2, todayDateStr, LocalTime.of(10, 30).format(timeFormatter)),
            Appointment(thisDoctor, patient1, todayDateStr, LocalTime.of(9, 45).format(timeFormatter)),
            Appointment(thisDoctor, patient2, todayDateStr, LocalTime.of(11, 15).format(timeFormatter)),
            Appointment(thisDoctor, patient3, todayDateStr, LocalTime.of(13, 0).format(timeFormatter))
        )
        // Use submitList for ListAdapter
        scheduleAdapter.submitList(allAppointments)
    }


    private fun setupSearch() {
        binding.editTextSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                performSearch(query)
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(query: String) {
        // TODO: Implement actual patient search logic
        if (query.isNotEmpty()) {
            Log.d("HomeActivity", "Wyszukiwanie pacjenta: $query")
            Toast.makeText(this, "Wyszukiwanie: $query", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("HomeActivity", "Wyszukiwanie anulowane lub puste")
        }
    }

    private fun setupWidget() {
        // TODO: Implement or customize widget logic
        val now = LocalTime.now()
        // This logic correctly handles time as String by parsing it back
        val nextAppointment = allAppointments
            .mapNotNull { appointment ->
                try {
                    Pair(appointment, LocalTime.parse(appointment.time, timeFormatter))
                } catch (e: DateTimeParseException) {
                    Log.e("HomeActivity", "Could not parse time string in setupWidget: ${appointment.time}", e)
                    null
                }
            }
            .filter { (_, parsedTime) -> parsedTime.isAfter(now) }
            .minByOrNull { (_, parsedTime) -> parsedTime }
            ?.first

        if (nextAppointment != null) {
            binding.textViewWidgetTitle.text = "Następna wizyta (${nextAppointment.time})" // Time as String
            binding.textViewWidgetContent.text = "${nextAppointment.patient.firstName} ${nextAppointment.patient.lastName}"
            // Or use: binding.textViewWidgetContent.text = nextAppointment.getDetails()
        } else {
            binding.textViewWidgetTitle.text = "Następna wizyta"
            binding.textViewWidgetContent.text = "Brak kolejnych wizyt na dziś."
        }
    }
}
