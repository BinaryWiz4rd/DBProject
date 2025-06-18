package com.example.project.doctor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.Admin.Patient
import com.example.project.Booking
import com.example.project.R
import com.example.project.databinding.FragmentDoctorHomeBinding
import com.example.project.doctor.model.Doctor
import com.example.project.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.DayOfWeek
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
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("pl", "PL"))
        } catch (e: IllegalArgumentException) {
            Log.w("DoctorHomeFragment", "Locale 'pl_PL' not fully supported, using default.")
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
        }
    }
    private val weekDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth
    private var currentDoctor: Doctor? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestoreHelper = FirestoreHelper()
        auth = FirebaseAuth.getInstance()

        setupUI()
        loadDoctorProfileAndInitialData()
    }

    private fun loadDoctorProfileAndInitialData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            setDefaultDashboardValues()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val document = firestoreHelper.getDoctorById(userId).await()
                if (document.exists()) {
                    currentDoctor = document.toObject(Doctor::class.java)?.apply {
                        if (this.uid.isBlank()) this.uid = document.id
                    }
                    withContext(Dispatchers.Main) {
                        setupGreeting()
                        loadDashboardAndScheduleData()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error loading doctor profile", Toast.LENGTH_SHORT).show()
                        setDefaultDashboardValues()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    setDefaultDashboardValues()
                }
            }
        }
    }

    private fun setDefaultDashboardValues() {
        binding.textViewTodayDate.text = LocalDate.now().format(displayDateFormatter)
        binding.textViewTodayAppointmentsCount.text = "0"
        binding.textViewWeekAppointmentsCount.text = "0"
        binding.textViewNewPatientsCount.text = "0"
        updateScheduleUI(emptyList())
    }

    private fun setupGreeting() {
        val doctorFirstName = currentDoctor?.firstName ?: "Doctor"
        binding.textViewGreeting.text = "Hello, $doctorFirstName!"
        binding.textViewTodayDate.text = LocalDate.now().format(displayDateFormatter)
    }

    private fun setupUI() {
        scheduleAdapter = ScheduleAdapter { appointment ->
            showAppointmentDetails(appointment)
        }
        binding.recyclerViewSchedule.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }

        binding.textViewScheduleTitle.text = "Today's Schedule"

        binding.editTextSearch.setOnEditorActionListener { v, actionId, _ ->
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
        binding.textViewWidgetSubtitle.visibility = View.GONE
        binding.divider.visibility = View.GONE
        binding.textViewPatientNameLabel.visibility = View.GONE
        binding.textViewPatientName.visibility = View.GONE
        binding.textViewServiceLabel.visibility = View.GONE
        binding.textViewService.visibility = View.GONE
        binding.buttonViewDetails.visibility = View.GONE
    }

    private fun loadDashboardAndScheduleData() {
        val doctorId = currentDoctor?.uid?.takeIf { it.isNotBlank() } ?: auth.currentUser?.uid
        if (doctorId == null) {
            setDefaultDashboardValues()
            return
        }
        loadTodaysSchedule(doctorId)
        loadWeeklyStats(doctorId)
    }

    private fun loadTodaysSchedule(doctorId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val todayDateStr = LocalDate.now().format(dateFormatter)
                val bookingsWithServices = firestoreHelper.getBookingsWithServiceDetails(doctorId, todayDateStr).await()

                val appointmentsList = mutableListOf<Appointment>()
                val processedBookings = mutableSetOf<String>()

                for ((booking, service) in bookingsWithServices) {
                    if (processedBookings.contains(booking.id)) continue
                    processedBookings.add(booking.id)
                    try {
                        val patientDoc = firestoreHelper.getPatientById(booking.patient_name as String).await()
                        val patient = if (patientDoc.exists()) {
                            patientDoc.toObject(Patient::class.java) ?: createPlaceholderPatient(booking.patient_name as String)
                        } else {
                            createPlaceholderPatient(booking.patient_name as String)
                        }
                        val doctorForAppointment = currentDoctor
                        if (doctorForAppointment != null && service != null) {
                            appointmentsList.add(
                                Appointment(
                                    doctor = doctorForAppointment,
                                    patient = patient,
                                    date = booking.date,
                                    time = booking.start_time,
                                    serviceName = service.name,
                                    servicePrice = service.price,
                                    serviceDuration = service.duration_minutes
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("DoctorHomeFragment", "Error fetching patient ${booking.patient_name}", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    updateScheduleUI(appointmentsList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load today's schedule: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateScheduleUI(emptyList())
                }
            }
        }
    }

    private fun updateScheduleUI(appointments: List<Appointment>) {
        allAppointments = appointments.sortedBy {
            try { LocalTime.parse(it.time, timeFormatter) }
            catch (e: DateTimeParseException) { LocalTime.MIDNIGHT }
        }
        if (isAdded) {
            scheduleAdapter.submitList(allAppointments)
            binding.textViewTodayAppointmentsCount.text = allAppointments.size.toString()
            val today = LocalDate.now()
            binding.textViewScheduleTitle.text = "Today's Schedule (${today.format(displayDateFormatter)})"

            setupWidget()
        }
    }

    private fun loadWeeklyStats(doctorId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val today = LocalDate.now()
                val firstDayOfWeek = today.with(DayOfWeek.MONDAY)
                val lastDayOfWeek = today.with(DayOfWeek.SUNDAY)

                val startDateStr = firstDayOfWeek.format(weekDateFormatter)
                val endDateStr = lastDayOfWeek.format(weekDateFormatter)

                val querySnapshot = firestoreHelper.getBookingsForDoctorDateRange(doctorId, startDateStr, endDateStr).await()
                if (isAdded) {
                    val weeklyBookingsCount = querySnapshot.size()
                    val uniquePatientIds = querySnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Booking::class.java)?.patient_name
                    }.distinct()
                    withContext(Dispatchers.Main) {
                        binding.textViewWeekAppointmentsCount.text = weeklyBookingsCount.toString()
                        binding.textViewNewPatientsCount.text = uniquePatientIds.size.toString()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.textViewWeekAppointmentsCount.text = "0"
                    binding.textViewNewPatientsCount.text = "0"
                    Toast.makeText(requireContext(), "Failed to load weekly stats: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isNotEmpty()) {
            val filteredAppointments = allAppointments.filter { appointment ->
                val patientName = "${appointment.patient.firstName} ${appointment.patient.lastName}".lowercase(Locale.getDefault())
                patientName.contains(query.lowercase(Locale.getDefault()))
            }
            scheduleAdapter.submitList(filteredAppointments)
        } else {
            scheduleAdapter.submitList(allAppointments)
        }
    }

    private fun setupWidget() {
        val now = LocalTime.now()
        val nextAppointment = allAppointments
            .mapNotNull { appointment ->
                try {
                    Pair(appointment, LocalTime.parse(appointment.time, timeFormatter))
                } catch (e: DateTimeParseException) {
                    Log.e("DoctorHomeFragment", "Could not parse time for widget: ${appointment.time}", e)
                    null
                }
            }
            .filter { (_, parsedTime) -> parsedTime.isAfter(now) }
            .minByOrNull { (_, parsedTime) -> parsedTime }
            ?.first

        if (isAdded) {
            if (nextAppointment != null) {
                binding.textViewWidgetTitle.text = "Next Appointment"
                binding.textViewWidgetSubtitle.text = "Today at ${nextAppointment.time}"
                binding.textViewWidgetContent.visibility = View.GONE

                binding.textViewPatientName.text = "${nextAppointment.patient.firstName} ${nextAppointment.patient.lastName}"
                binding.textViewService.text = nextAppointment.serviceName

                binding.textViewWidgetSubtitle.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE
                binding.textViewPatientNameLabel.visibility = View.VISIBLE
                binding.textViewPatientName.visibility = View.VISIBLE
                binding.textViewServiceLabel.visibility = View.VISIBLE
                binding.textViewService.visibility = View.VISIBLE
                binding.buttonViewDetails.visibility = View.VISIBLE

                binding.buttonViewDetails.setOnClickListener {
                    showAppointmentDetails(nextAppointment)
                }
            } else {
                binding.textViewWidgetTitle.text = "No Upcoming Appointments"
                binding.textViewWidgetContent.text = "Your schedule is clear for today"
                binding.textViewWidgetContent.visibility = View.VISIBLE

                binding.textViewWidgetSubtitle.visibility = View.GONE
                binding.divider.visibility = View.GONE
                binding.textViewPatientNameLabel.visibility = View.GONE
                binding.textViewPatientName.visibility = View.GONE
                binding.textViewServiceLabel.visibility = View.GONE
                binding.textViewService.visibility = View.GONE
                binding.buttonViewDetails.visibility = View.GONE
            }
        }
    }

    private fun showAppointmentDetails(appointment: Appointment) {
        if (!isAdded) return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_appointment_details, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogAnimation)
            .setView(dialogView)
            .create()

        val titleTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentTitle)
        val dateTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentDate)
        val timeTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentTime)
        val patientTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentPatient)
        val serviceTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentService)
        val durationTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentDuration)
        val priceTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentPrice)

        val patientEmailTextView = dialogView.findViewById<TextView>(R.id.textViewPatientEmail)
        val patientPhoneTextView = dialogView.findViewById<TextView>(R.id.textViewPatientPhone)
        val patientDobTextView = dialogView.findViewById<TextView>(R.id.textViewPatientDob)

        val viewInCalendarButton = dialogView.findViewById<Button>(R.id.buttonViewInCalendar)
        val viewPatientHistoryButton = dialogView.findViewById<Button>(R.id.buttonPatientHistory)
        val callPatientButton = dialogView.findViewById<Button>(R.id.buttonCallPatient)
        val emailPatientButton = dialogView.findViewById<Button>(R.id.buttonEmailPatient)
        val messagePatientButton = dialogView.findViewById<Button>(R.id.buttonMessagePatient)
        val closeButton = dialogView.findViewById<Button>(R.id.buttonClose)

        titleTextView.text = getString(R.string.appointment_details)
        dateTextView.text = appointment.date
        timeTextView.text = appointment.time
        patientTextView.text = "${appointment.patient.firstName} ${appointment.patient.lastName}"
        serviceTextView.text = appointment.serviceName
        durationTextView.text = "Duration: ${appointment.serviceDuration} minutes"

        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale.US)
        priceTextView.text = currencyFormat.format(appointment.servicePrice)

        patientEmailTextView.text = appointment.patient.email
        patientPhoneTextView.text = appointment.patient.phoneNumber.ifEmpty { "Not provided" }
        patientDobTextView.text = appointment.patient.dateOfBirth.ifEmpty { "Not provided" }

        callPatientButton.visibility = View.GONE
        messagePatientButton.visibility = View.GONE
        emailPatientButton.isEnabled = appointment.patient.email.isNotEmpty()

        viewInCalendarButton.setOnClickListener {
            navigateToCalendarWithDate(appointment.date, appointment.time)
            dialog.dismiss()
        }

        viewPatientHistoryButton.setOnClickListener {
            showPatientHistory(appointment.patient)
            dialog.dismiss()
        }

        emailPatientButton.setOnClickListener {
            if (appointment.patient.email.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${appointment.patient.email}")
                        putExtra(Intent.EXTRA_SUBJECT, "Regarding your appointment on ${appointment.date}")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("DoctorHomeFragment", "Error launching email app: ${e.message}")
                    Toast.makeText(context, "Could not open email app", Toast.LENGTH_SHORT).show()
                }
            }
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPatientHistory(patient: Patient) {
        try {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_patient_history, null)

            val dialog = AlertDialog.Builder(requireContext(), R.style.DialogAnimation)
                .setTitle(getString(R.string.patient_history_title, "${patient.firstName} ${patient.lastName}"))
                .setView(dialogView)
                .setPositiveButton(R.string.close, null)
                .create()

            val loadingContainer = dialogView.findViewById<View>(R.id.loadingContainer)
            val historyCardView = dialogView.findViewById<View>(R.id.historyCardView)
            val historyList = dialogView.findViewById<ListView>(R.id.historyListView)
            val noHistoryText = dialogView.findViewById<TextView>(R.id.noHistoryTextView)
            val historyTitleTextView = dialogView.findViewById<TextView>(R.id.historyTitleTextView)

            loadingContainer.visibility = View.VISIBLE
            historyCardView.visibility = View.GONE
            noHistoryText.visibility = View.GONE
            historyTitleTextView.visibility = View.GONE

            dialog.show()

            loadPatientHistory(patient.email) { historyItems ->
                if (isAdded) {
                    requireActivity().runOnUiThread {
                        loadingContainer.visibility = View.GONE

                        if (historyItems.isEmpty()) {
                            noHistoryText.visibility = View.VISIBLE
                            historyCardView.visibility = View.GONE
                        } else {
                            val adapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                historyItems
                            )
                            historyList.adapter = adapter
                            historyCardView.visibility = View.VISIBLE
                            noHistoryText.visibility = View.GONE
                            historyTitleTextView.visibility = View.VISIBLE
                            historyTitleTextView.text = getString(R.string.patient_history)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DoctorHomeFragment", "Error showing patient history: ${e.message}", e)
            Toast.makeText(requireContext(), "Could not load patient history", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPatientHistory(patientId: String?, callback: (List<String>) -> Unit) {
        if (patientId.isNullOrEmpty()) {
            callback(emptyList())
            return
        }

        firestoreHelper.getDbInstance()
            .collection("bookings")
            .whereEqualTo("patient_name", patientId)
            .get()
            .addOnSuccessListener { bookingDocuments ->
                if (bookingDocuments.isEmpty) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                val historyItems = mutableListOf<HistoryItem>()
                val totalBookings = bookingDocuments.size()
                var processedBookings = 0

                for (bookingDoc in bookingDocuments) {
                    val booking = bookingDoc.toObject(Booking::class.java).copy(id = bookingDoc.id)

                    firestoreHelper.getDoctorById(booking.doctor_id)
                        .addOnSuccessListener { doctorDoc ->
                            val doctorName = if (doctorDoc.exists()) {
                                val firstName = doctorDoc.getString("firstName") ?: ""
                                val lastName = doctorDoc.getString("lastName") ?: ""
                                "$firstName $lastName"
                            } else {
                                "Unknown"
                            }

                            firestoreHelper.getServiceById(booking.service_id)
                                .addOnSuccessListener { serviceDoc ->
                                    val serviceName = if (serviceDoc.exists()) {
                                        serviceDoc.getString("name") ?: "Unknown Service"
                                    } else {
                                        "Unknown Service"
                                    }

                                    historyItems.add(
                                        HistoryItem(
                                            date = booking.date,
                                            serviceName = serviceName,
                                            doctorName = doctorName
                                        )
                                    )

                                    processedBookings++

                                    if (processedBookings == totalBookings) {
                                        historyItems.sortByDescending { it.date }
                                        val displayItems = historyItems.map { item ->
                                            getString(
                                                R.string.history_date_format,
                                                item.date,
                                                item.serviceName,
                                                item.doctorName
                                            )
                                        }
                                        callback(displayItems)
                                    }
                                }
                                .addOnFailureListener {
                                    processedBookings++
                                    if (processedBookings == totalBookings) {
                                        historyItems.sortByDescending { it.date }
                                        val displayItems = historyItems.map { item ->
                                            getString(
                                                R.string.history_date_format,
                                                item.date,
                                                item.serviceName,
                                                item.doctorName
                                            )
                                        }
                                        callback(displayItems)
                                    }
                                }
                        }
                        .addOnFailureListener {
                            processedBookings++
                            if (processedBookings == totalBookings) {
                                historyItems.sortByDescending { it.date }
                                val displayItems = historyItems.map { item ->
                                    getString(
                                        R.string.history_date_format,
                                        item.date,
                                        item.serviceName,
                                        item.doctorName
                                    )
                                }
                                callback(displayItems)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DoctorHomeFragment", "Error loading patient history: ${e.message}", e)
                callback(emptyList())
            }
    }

    private data class HistoryItem(
        val date: String,
        val serviceName: String,
        val doctorName: String
    )

    private fun createPlaceholderPatient(email: String): Patient {
        val displayName = email.substringBefore("@")
        return Patient(
            firstName = displayName,
            lastName = "(Patient)",
            email = email,
            phoneNumber = "",
            dateOfBirth = ""
        )
    }

    private fun navigateToCalendarWithDate(dateString: String, timeString: String = "") {
        try {
            val bundle = Bundle().apply {
                putString("selected_date", dateString)
                if (timeString.isNotEmpty()) {
                    putString("selected_time", timeString)
                }
            }

            val calendarFragment = DoctorCalendarFragment().apply {
                arguments = bundle
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, calendarFragment)
                .addToBackStack("doctor_home")
                .commit()

            Toast.makeText(
                requireContext(),
                "Opening calendar for date: $dateString",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e("DoctorHomeFragment", "Error navigating to calendar: ${e.message}", e)
            Toast.makeText(requireContext(), "Could not open calendar view", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}