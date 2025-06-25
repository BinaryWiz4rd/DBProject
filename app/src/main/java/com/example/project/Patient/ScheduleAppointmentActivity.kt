package com.example.project.Patient
/*import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.project.R

/**
 * Activity for scheduling an appointment with a doctor.
 * Allows patients to select a doctor, date, and time for booking an appointment.
 */
class ScheduleAppointmentActivity : AppCompatActivity() {
// to jest dla pacjenta

    private lateinit var spinnerDoctors: Spinner
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var btnSchedule: Button

    // Przykładowa lista lekarzy (w prawdziwej aplikacji może być pobierana z API/bazy)
    private val doctorsList = listOf(
        Doctor(1, "Jan Kowalski", "Kardiolog"),
        Doctor(2, "Anna Nowak", "Dermatolog"),
        Doctor(3, "Piotr Wiśniewski", "Ortopeda")
    )

    // Przykładowy pacjent zalogowany (w prawdziwej aplikacji pobrane z sesji lub bazy)
    private val currentPatient = Patient("P001", "Adam Pacjent")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_appointment)

        // Inicjalizacja widoków
        spinnerDoctors = findViewById(R.id.spinnerDoctors)
        datePicker = findViewById(R.id.datePicker)
        timePicker = findViewById(R.id.timePicker)
        btnSchedule = findViewById(R.id.btnSchedule)

        // Konfiguracja Spinnera - wyświetlamy nazwy lekarzy
        val doctorNames = doctorsList.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            doctorNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDoctors.adapter = adapter

        // Obsługa kliknięcia przycisku "Umów wizytę"
        btnSchedule.setOnClickListener {
            // Sprawdzamy, który lekarz został wybrany
            val doctorIndex = spinnerDoctors.selectedItemPosition
            if (doctorIndex == AdapterView.INVALID_POSITION) {
                Toast.makeText(this, "Wybierz lekarza!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedDoctor = doctorsList[doctorIndex]

            // Pobieramy datę z DatePicker
            val day = datePicker.dayOfMonth
            val month = datePicker.month + 1 // w DatePicker miesiące są od 0
            val year = datePicker.year
            val dateStr = String.format("%04d-%02d-%02d", year, month, day)

            // Pobieramy godzinę z TimePicker
            val hour: Int
            val minute: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = timePicker.hour
                minute = timePicker.minute
            } else {
                hour = timePicker.currentHour
                minute = timePicker.currentMinute
            }
            val timeStr = String.format("%02d:%02d", hour, minute)

            // Tworzymy obiekt Appointment
            val appointment = Appointment(
                doctor = selectedDoctor,
                patient = currentPatient,
                date = dateStr,
                time = timeStr
            )

            // Możemy np. wyświetlić Toast z informacją
            Toast.makeText(this, appointment.getDetails(), Toast.LENGTH_LONG).show()

            // Ewentualnie tutaj możesz zapisać appointment w bazie / wysłać do API itp.
        }
    }
}*/