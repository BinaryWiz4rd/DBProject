package com.example.project.Doctor

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.Admin.Doctor
import com.example.project.Admin.Patient
import com.example.project.databinding.FragmentDoctorHomeBinding
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class DoctorHomeFragment : Fragment() {

    private var _binding: FragmentDoctorHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var scheduleAdapter: ScheduleAdapter
    private var allAppointments: List<Appointment> = listOf()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter: DateTimeFormatter by lazy {
        try {
            DateTimeFormatter.ofPattern("d MMMM", Locale("pl", "PL"))
        } catch (e: IllegalArgumentException) {
            Log.w("DoctorHomeFragment", "Locale 'pl_PL' not fully supported, using default.")
            DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreeting()
        setupUI()
        loadScheduleData()
        setupSearch()
        setupWidget()
    }

    private fun setupGreeting() {
        val userName = getUserFirstName()
        binding.textViewGreeting.text = if (userName != null) {
            "Cześć, $userName!"
        } else {
            "Witaj!"
        }
    }

    private fun getUserFirstName(): String? {
        // -------> WAŻNE: ZASTĄP TO PRAWDZIWĄ LOGIKĄ POBIERANIA IMIENIA LEKARZA <-------
        return "Jan" // Example
        // --------------------------------------------------------------------
    }

    private fun setupUI() {
        scheduleAdapter = ScheduleAdapter { appointment ->
            Toast.makeText(
                requireContext(),
                "Kliknięto wizytę: ${appointment.patient.firstName} ${appointment.patient.lastName}",
                Toast.LENGTH_SHORT
            ).show()
            // TODO: Add navigation logic here (e.g., using Navigation Component)
        }
        binding.recyclerViewSchedule.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }

        val today = LocalDate.now()
        binding.textViewScheduleTitle.text = "Plan na dziś (${today.format(displayDateFormatter)})"
    }

    private fun loadScheduleData() {
        // TODO: Load real Doctor, Patient, and Appointment data from your data source (e.g., Room database, Firebase)

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

        allAppointments = listOf(
            Appointment(thisDoctor, patient1, todayDateStr, LocalTime.of(9, 0).format(timeFormatter)),
            Appointment(thisDoctor, patient2, todayDateStr, LocalTime.of(10, 30).format(timeFormatter)),
            Appointment(thisDoctor, patient1, todayDateStr, LocalTime.of(9, 45).format(timeFormatter)),
            Appointment(thisDoctor, patient2, todayDateStr, LocalTime.of(11, 15).format(timeFormatter)),
            Appointment(thisDoctor, patient3, todayDateStr, LocalTime.of(13, 0).format(timeFormatter))
        ).sortedBy {
            try {
                LocalTime.parse(it.time, timeFormatter)
            } catch (e: DateTimeParseException) {
                Log.e("DoctorHomeFragment", "Error parsing time: ${it.time}", e)
                LocalTime.MIN
            }
        }
        scheduleAdapter.submitList(allAppointments)
    }

    private fun setupSearch() {
        binding.editTextSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                performSearch(query)
                val imm =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(query: String) {
        // TODO: Implement actual patient search logic (e.g., using Room database query with LIKE)
        if (query.isNotEmpty()) {
            Log.d("DoctorHomeFragment", "Wyszukiwanie pacjenta: $query")
            Toast.makeText(requireContext(), "Wyszukiwanie: $query", Toast.LENGTH_SHORT).show()
            val searchResults = allAppointments.filter { appointment ->
                appointment.patient.firstName.contains(query, ignoreCase = true) ||
                        appointment.patient.lastName.contains(query, ignoreCase = true)
            }
            scheduleAdapter.submitList(searchResults)

        } else {
            Log.d("DoctorHomeFragment", "Wyszukiwanie anulowane lub puste")
            loadScheduleData()
        }
    }

    private fun setupWidget() {
        val now = LocalTime.now()
        val nextAppointment = allAppointments
            .mapNotNull { appointment ->
                try {
                    Pair(appointment, LocalTime.parse(appointment.time, timeFormatter))
                } catch (e: DateTimeParseException) {
                    Log.e(
                        "DoctorHomeFragment",
                        "Could not parse time string in setupWidget: ${appointment.time}",
                        e
                    )
                    null
                }
            }
            .filter { (_, parsedTime) -> parsedTime.isAfter(now) }
            .minByOrNull { (_, parsedTime) -> parsedTime }
            ?.first

        if (nextAppointment != null) {
            binding.textViewWidgetTitle.text = "Następna wizyta (${nextAppointment.time})"
            binding.textViewWidgetContent.text =
                "${nextAppointment.patient.firstName} ${nextAppointment.patient.lastName}"
        } else {
            binding.textViewWidgetTitle.text = "Następna wizyta"
            binding.textViewWidgetContent.text = "Brak kolejnych wizyt na dziś."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

