package com.example.project.doctor

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.doctor.ScheduleAdapter
import com.example.project.databinding.ActivityDoctorHomeBinding
import com.example.project.doctor.model.Doctor
import com.example.project.Admin.Patient
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Main screen for the doctor's application, displaying schedule, search, and next appointment details.
 */
class HomeActivity : AppCompatActivity() {

    /**
     * View binding for `activity_doctor_home.xml` layout.
     */
    private lateinit var binding: ActivityDoctorHomeBinding
    /**
     * Adapter for displaying appointments in `RecyclerView`.
     */
    private lateinit var scheduleAdapter: ScheduleAdapter
    /**
     * List of all loaded appointments.
     */
    private var allAppointments: List<Appointment> = listOf()
    /**
     * Formatter for time strings ("HH:mm").
     */
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    /**
     * Formatter for date strings ("yyyy-MM-dd").
     */
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Initializes activity: sets up UI, loads data, configures search and widget.
     * @param savedInstanceState Contains data if activity is re-initialized.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGreeting()
        setupUI()
        loadScheduleData()
        setupSearch()
        setupWidget()
    }

    /**
     * Sets the personalized greeting message.
     */
    private fun setupGreeting() {
        val userName = getUserFirstName()
        if (userName != null) {
            binding.textViewGreeting.text = "Cześć, $userName!"
        } else {
            binding.textViewGreeting.text = "Witaj!"
        }
    }

    /**
     * Retrieves the first name of the logged-in doctor. Placeholder for actual logic.
     * @return Doctor's first name, or `null`.
     */
    private fun getUserFirstName(): String? {
        return "Jan"
    }

    /**
     * Configures UI components, including `ScheduleAdapter` and schedule title.
     */
    private fun setupUI() {
        scheduleAdapter = ScheduleAdapter { appointment ->
            Toast.makeText(this, "Kliknięto wizytę: ${appointment.patient.firstName} ${appointment.patient.lastName}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewSchedule.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = scheduleAdapter
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


    /**
     * Loads schedule data (doctor, patient, appointments). Currently uses static example data.
     */
    private fun loadScheduleData() {
        val thisDoctor = Doctor(
            uid = "1",
            firstName = "Jan",
            lastName = "Kowalski",
            email = "jan.kowalski@example.com",
            specialization = "Kardiolog",
            pwzNumber = "1234567",
            phoneNumber = "123456789"
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

        allAppointments = listOf(
            Appointment(thisDoctor, patient1, todayDateStr, LocalTime.of(9, 0).format(timeFormatter)),
            Appointment(thisDoctor, patient2, todayDateStr, LocalTime.of(10, 30).format(timeFormatter)),
            Appointment(thisDoctor, patient1, todayDateStr, LocalTime.of(9, 45).format(timeFormatter)),
            Appointment(thisDoctor, patient2, todayDateStr, LocalTime.of(11, 15).format(timeFormatter)),
            Appointment(thisDoctor, patient3, todayDateStr, LocalTime.of(13, 0).format(timeFormatter))
        )
        scheduleAdapter.submitList(allAppointments)
    }

    /**
     * Configures the search input field to perform search on IME action.
     */
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

    /**
     * Executes patient search logic.
     * @param query The search query string.
     */
    private fun performSearch(query: String) {
        if (query.isNotEmpty()) {
            Log.d("HomeActivity", "Wyszukiwanie pacjenta: $query")
            Toast.makeText(this, "Wyszukiwanie: $query", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("HomeActivity", "Wyszukiwanie anulowane lub puste")
        }
    }

    /**
     * Sets up the widget displaying the next upcoming appointment.
     */
    private fun setupWidget() {
        val now = LocalTime.now()
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
            binding.textViewWidgetTitle.text = "Następna wizyta (${nextAppointment.time})"
            binding.textViewWidgetContent.text = "${nextAppointment.patient.firstName} ${nextAppointment.patient.lastName}"
        } else {
            binding.textViewWidgetTitle.text = "Następna wizyta"
            binding.textViewWidgetContent.text = "Brak kolejnych wizyt na dziś."
        }
    }
}