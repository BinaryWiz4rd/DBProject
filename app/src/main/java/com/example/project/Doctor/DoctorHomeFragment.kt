package com.example.project.doctor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.Admin.Patient
import com.example.project.databinding.FragmentDoctorHomeBinding
import com.example.project.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import com.example.project.doctor.Appointment
import com.example.project.doctor.ScheduleAdapter
import com.example.project.doctor.model.Doctor
import com.google.android.gms.tasks.Tasks // Added for Tasks.whenAllComplete
import java.time.DayOfWeek // Added for weekly calculations
import com.example.project.Booking // Added import
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.example.project.R

class DoctorHomeFragment : Fragment() {

    private var _binding: FragmentDoctorHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var scheduleAdapter: ScheduleAdapter
    private var allAppointments: List<Appointment> = listOf()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter: DateTimeFormatter by lazy {
        try {
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("pl", "PL")) // Added yyyy for clarity
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
        if (userId != null) {
            firestoreHelper.getDoctorById(userId)
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentDoctor = document.toObject(Doctor::class.java)?.apply {
                            // Assuming Doctor model has a 'uid' field for the document ID
                            if (this.uid.isBlank()) { // Changed from id to uid
                                this.uid = document.id // Changed from id to uid
                            }
                        }
                        setupGreeting()
                        loadDashboardAndScheduleData()
                    } else {
                        Log.w("DoctorHomeFragment", "No doctor document found for ID: $userId")
                        Toast.makeText(requireContext(), "Error loading doctor profile", Toast.LENGTH_SHORT).show()
                        setDefaultDashboardValues()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DoctorHomeFragment", "Error loading doctor profile", e)
                    Toast.makeText(requireContext(), "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    setDefaultDashboardValues()
                }
        } else {
            Log.w("DoctorHomeFragment", "User not logged in")
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            setDefaultDashboardValues()
        }
    }

    private fun setDefaultDashboardValues() {
        binding.textViewTodayDate.text = LocalDate.now().format(displayDateFormatter)
        binding.textViewTodayAppointmentsCount.text = "0"
        binding.textViewWeekAppointmentsCount.text = "0"
        binding.textViewNewPatientsCount.text = "0"
        updateScheduleUI(emptyList()) // Clears schedule and updates widget
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

        binding.textViewScheduleTitle.text = "Today's Schedule" // Date will be dynamic if needed or removed if redundant with top date

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
        // Initial state for next appointment details (already set to gone in XML, this is for programmatic safety)
        binding.textViewWidgetSubtitle.visibility = View.GONE
        binding.divider.visibility = View.GONE
        binding.textViewPatientNameLabel.visibility = View.GONE
        binding.textViewPatientName.visibility = View.GONE
        binding.textViewServiceLabel.visibility = View.GONE
        binding.textViewService.visibility = View.GONE
        binding.buttonViewDetails.visibility = View.GONE
    }

    private fun loadDashboardAndScheduleData() {
        val doctorId = currentDoctor?.uid?.takeIf { it.isNotBlank() } ?: auth.currentUser?.uid // Changed from id to uid
        if (doctorId == null) {
            Log.w("DoctorHomeFragment", "Doctor ID not available for loading dashboard data.")
            setDefaultDashboardValues()
            return
        }

        loadTodaysSchedule(doctorId)
        loadWeeklyStats(doctorId)
    }

    private fun loadTodaysSchedule(doctorId: String) {
        // Get current date in the format "yyyy-MM-dd"
        // Get the current date from system to match the format in the database (May 11, 2025)
        val todayDateStr = LocalDate.now().format(dateFormatter)
        
        // Log the date being queried for debugging
        Log.d("DoctorHomeFragment", "Loading schedule for date: $todayDateStr")
        
        firestoreHelper.getBookingsWithServiceDetails(doctorId, todayDateStr)
            .addOnSuccessListener { bookingsWithServices ->
                val appointmentsList = mutableListOf<Appointment>()
                val patientFetchTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

                if (bookingsWithServices.isEmpty()) {
                    updateScheduleUI(emptyList())
                    return@addOnSuccessListener
                }

                val processedBookings = mutableSetOf<String>() // To handle multiple calls if any

                for ((booking, service) in bookingsWithServices) {
                    // Assuming com.example.project.Booking has 'id: String'
                    if (processedBookings.contains(booking.id)) continue 
                    processedBookings.add(booking.id)

                    // Assuming booking.patient_name is String, but error indicates it's seen as Any
                    val patientTask = firestoreHelper.getPatientById(booking.patient_name as String) 
                        .addOnSuccessListener { patientDoc ->
                            val doctorForAppointment = currentDoctor // Capture currentDoctor state
                            if (doctorForAppointment != null && service != null) {
                                // Create patient object - either from document or create a placeholder
                                val patient = if (patientDoc.exists()) {
                                    patientDoc.toObject(Patient::class.java) ?: createPlaceholderPatient(booking.patient_name as String)
                                } else {
                                    Log.w("DoctorHomeFragment", "Patient document not found for ID: ${booking.patient_name.toString()}")
                                    // Create placeholder patient with the email
                                    createPlaceholderPatient(booking.patient_name as String)
                                }
                                
                                // Add appointment with patient (real or placeholder)
                                appointmentsList.add(Appointment(
                                    doctor = doctorForAppointment,
                                    patient = patient,
                                    date = booking.date,
                                    time = booking.start_time,
                                    serviceName = service.name,
                                    servicePrice = service.price,
                                    serviceDuration = service.duration_minutes
                                ))
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("DoctorHomeFragment", "Error fetching patient ${booking.patient_name.toString()}", e)
                        }
                    patientFetchTasks.add(patientTask)
                }

                Tasks.whenAllComplete(patientFetchTasks)
                    .addOnCompleteListener {
                        // This ensures updateScheduleUI is called after all attempts to fetch patient data
                        updateScheduleUI(appointmentsList)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DoctorHomeFragment", "Error loading today's schedule data", e)
                Toast.makeText(requireContext(), "Failed to load today's schedule: ${e.message}", Toast.LENGTH_SHORT).show()
                updateScheduleUI(emptyList())
            }
    }

    private fun updateScheduleUI(appointments: List<Appointment>) {
        // Log all appointments received for debugging
        Log.d("DoctorHomeFragment", "Received ${appointments.size} appointments")
        for (appointment in appointments) {
            Log.d("DoctorHomeFragment", "Appointment: date=${appointment.date}, time=${appointment.time}, patient=${appointment.patient.firstName}")
        }
        
        allAppointments = appointments.sortedBy {
            try { LocalTime.parse(it.time, timeFormatter) }
            catch (e: DateTimeParseException) { LocalTime.MIDNIGHT }
        }
        if (isAdded) { // Ensure fragment is still added to an activity
            // Log appointments after sorting
            Log.d("DoctorHomeFragment", "Submitting ${allAppointments.size} appointments to adapter")
            
            scheduleAdapter.submitList(allAppointments)
            binding.textViewTodayAppointmentsCount.text = allAppointments.size.toString()
            val today = LocalDate.now()
            binding.textViewScheduleTitle.text = "Today's Schedule (${today.format(displayDateFormatter)})"

            setupWidget()
        }
    }

    private fun loadWeeklyStats(doctorId: String) {
        val today = LocalDate.now()
        val firstDayOfWeek = today.with(DayOfWeek.MONDAY)
        val lastDayOfWeek = today.with(DayOfWeek.SUNDAY)

        val startDateStr = firstDayOfWeek.format(weekDateFormatter)
        val endDateStr = lastDayOfWeek.format(weekDateFormatter)

        firestoreHelper.getBookingsForDoctorDateRange(doctorId, startDateStr, endDateStr)
            .addOnSuccessListener { querySnapshot ->
                if (isAdded) {
                    val weeklyBookingsCount = querySnapshot.size()
                    binding.textViewWeekAppointmentsCount.text = weeklyBookingsCount.toString()

                    val uniquePatientIds = querySnapshot.documents.mapNotNull { doc ->
                        // Assuming Booking class is com.example.project.Booking
                        doc.toObject(com.example.project.Booking::class.java)?.patient_name
                    }.distinct()
                    binding.textViewNewPatientsCount.text = uniquePatientIds.size.toString()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DoctorHomeFragment", "Error loading weekly stats", e)
                if (isAdded) {
                    binding.textViewWeekAppointmentsCount.text = "0"
                    binding.textViewNewPatientsCount.text = "0"
                    Toast.makeText(requireContext(), "Failed to load weekly stats: ${e.message}", Toast.LENGTH_SHORT).show()
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
                try { Pair(appointment, LocalTime.parse(appointment.time, timeFormatter)) }
                catch (e: DateTimeParseException) {
                    Log.e("DoctorHomeFragment", "Could not parse time for widget: ${appointment.time}", e)
                    null
                }
            }
            .filter { (_, parsedTime) -> parsedTime.isAfter(now) }
            .minByOrNull { (_, parsedTime) -> parsedTime }
            ?.first

        if (isAdded) { // Ensure fragment is still added
            if (nextAppointment != null) {
                binding.textViewWidgetTitle.text = "Next Appointment"
                binding.textViewWidgetSubtitle.text = "Today at ${nextAppointment.time}"
                binding.textViewWidgetContent.visibility = View.GONE // Hide generic content

                binding.textViewPatientName.text = "${nextAppointment.patient.firstName} ${nextAppointment.patient.lastName}"
                binding.textViewService.text = nextAppointment.serviceName

                binding.textViewWidgetSubtitle.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE
                binding.textViewPatientNameLabel.visibility = View.VISIBLE
                binding.textViewPatientName.visibility = View.VISIBLE
                binding.textViewServiceLabel.visibility = View.VISIBLE
                binding.textViewService.visibility = View.VISIBLE
                binding.buttonViewDetails.visibility = View.VISIBLE
                
                // Set up the "View Details" button
                binding.buttonViewDetails.setOnClickListener {
                    showAppointmentDetails(nextAppointment)
                }
            } else {
                binding.textViewWidgetTitle.text = "No Upcoming Appointments"
                binding.textViewWidgetContent.text = "Your schedule is clear for today"
                binding.textViewWidgetContent.visibility = View.VISIBLE // Show generic content

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
        if (!isAdded) return // Make sure fragment is attached

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_appointment_details, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogAnimation)
            .setView(dialogView)
            .create()

        // Set up dialog views
        val titleTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentTitle)
        val dateTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentDate)
        val timeTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentTime)
        val patientTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentPatient)
        val serviceTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentService)
        val durationTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentDuration)
        val priceTextView = dialogView.findViewById<TextView>(R.id.textViewAppointmentPrice)
        
        // Patient contact information views
        val patientEmailTextView = dialogView.findViewById<TextView>(R.id.textViewPatientEmail)
        val patientPhoneTextView = dialogView.findViewById<TextView>(R.id.textViewPatientPhone)
        val patientDobTextView = dialogView.findViewById<TextView>(R.id.textViewPatientDob)
        
        // Buttons
        val viewInCalendarButton = dialogView.findViewById<Button>(R.id.buttonViewInCalendar)
        val viewPatientHistoryButton = dialogView.findViewById<Button>(R.id.buttonPatientHistory)
        val callPatientButton = dialogView.findViewById<Button>(R.id.buttonCallPatient)
        val emailPatientButton = dialogView.findViewById<Button>(R.id.buttonEmailPatient)
        val messagePatientButton = dialogView.findViewById<Button>(R.id.buttonMessagePatient)
        val closeButton = dialogView.findViewById<Button>(R.id.buttonClose)

        // Populate dialog with appointment details
        titleTextView.text = getString(R.string.appointment_details)
        dateTextView.text = appointment.date
        timeTextView.text = appointment.time
        patientTextView.text = "${appointment.patient.firstName} ${appointment.patient.lastName}"
        serviceTextView.text = appointment.serviceName
        durationTextView.text = "Duration: ${appointment.serviceDuration} minutes"
        
        // Format price using NumberFormat for proper currency formatting
        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale.US)
        priceTextView.text = currencyFormat.format(appointment.servicePrice)
        
        // Set patient contact information
        patientEmailTextView.text = appointment.patient.email
        patientPhoneTextView.text = appointment.patient.phoneNumber.ifEmpty { "Not provided" }
        patientDobTextView.text = appointment.patient.dateOfBirth.ifEmpty { "Not provided" }
        
        // Hide call and message options
        callPatientButton.visibility = View.GONE
        messagePatientButton.visibility = View.GONE
        
        // Keep email option
        emailPatientButton.isEnabled = appointment.patient.email.isNotEmpty()
        
        // Set up button actions
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
            // Create a dialog to show patient history
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_patient_history, null)
            
            val dialog = AlertDialog.Builder(requireContext(), R.style.DialogAnimation)
                .setTitle(getString(R.string.patient_history_title, "${patient.firstName} ${patient.lastName}"))
                .setView(dialogView)
                .setPositiveButton(R.string.close, null)
                .create()
            
            // Find views in dialog
            val loadingContainer = dialogView.findViewById<View>(R.id.loadingContainer)
            val historyCardView = dialogView.findViewById<View>(R.id.historyCardView)
            val historyList = dialogView.findViewById<ListView>(R.id.historyListView)
            val noHistoryText = dialogView.findViewById<TextView>(R.id.noHistoryTextView)
            val historyTitleTextView = dialogView.findViewById<TextView>(R.id.historyTitleTextView)
            
            // Show loading initially
            loadingContainer.visibility = View.VISIBLE
            historyCardView.visibility = View.GONE
            noHistoryText.visibility = View.GONE
            historyTitleTextView.visibility = View.GONE
            
            // Show the dialog first, then load data
            dialog.show()
            
            // This has been updated to fetch real data from Firestore
            loadPatientHistory(patient.email) { historyItems ->
                if (isAdded) { // Check if fragment is still attached
                    // Update UI on main thread
                    requireActivity().runOnUiThread {
                        loadingContainer.visibility = View.GONE
                        
                        if (historyItems.isEmpty()) {
                            noHistoryText.visibility = View.VISIBLE
                            historyCardView.visibility = View.GONE
                        } else {
                            // Create and set adapter with a custom row layout for better appearance
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
        
        // This will get all bookings associated with this patient
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
                    
                    // Get doctor information for this booking
                    firestoreHelper.getDoctorById(booking.doctor_id)
                        .addOnSuccessListener { doctorDoc ->
                            val doctorName = if (doctorDoc.exists()) {
                                val firstName = doctorDoc.getString("firstName") ?: ""
                                val lastName = doctorDoc.getString("lastName") ?: ""
                                "$firstName $lastName"
                            } else {
                                "Unknown"
                            }
                            
                            // Get service information
                            firestoreHelper.getServiceById(booking.service_id)
                                .addOnSuccessListener { serviceDoc ->
                                    val serviceName = if (serviceDoc.exists()) {
                                        serviceDoc.getString("name") ?: "Unknown Service"
                                    } else {
                                        "Unknown Service"
                                    }
                                    
                                    historyItems.add(HistoryItem(
                                        date = booking.date,
                                        serviceName = serviceName,
                                        doctorName = doctorName
                                    ))
                                    
                                    processedBookings++
                                    
                                    // Check if all bookings are processed
                                    if (processedBookings == totalBookings) {
                                        // Sort by date in descending order (newest first)
                                        historyItems.sortByDescending { it.date }
                                        
                                        // Convert to display format
                                        val displayItems = historyItems.map { item ->
                                            getString(R.string.history_date_format, 
                                                item.date, 
                                                item.serviceName, 
                                                item.doctorName)
                                        }
                                        
                                        callback(displayItems)
                                    }
                                }
                                .addOnFailureListener {
                                    processedBookings++
                                    if (processedBookings == totalBookings) {
                                        // Still need to complete even if some bookings fail
                                        historyItems.sortByDescending { it.date }
                                        val displayItems = historyItems.map { item ->
                                            getString(R.string.history_date_format, 
                                                item.date, 
                                                item.serviceName, 
                                                item.doctorName)
                                        }
                                        callback(displayItems)
                                    }
                                }
                        }
                        .addOnFailureListener {
                            processedBookings++
                            if (processedBookings == totalBookings) {
                                // Still need to complete even if some bookings fail
                                historyItems.sortByDescending { it.date }
                                val displayItems = historyItems.map { item ->
                                    getString(R.string.history_date_format, 
                                        item.date, 
                                        item.serviceName, 
                                        item.doctorName)
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

    // Helper class for patient history items
    private data class HistoryItem(
        val date: String,
        val serviceName: String,
        val doctorName: String
    )
    
    // Helper method to create placeholder patient when patient document is not found
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
            // Create a bundle to pass data to the calendar fragment
            val bundle = Bundle().apply {
                putString("selected_date", dateString)
                if (timeString.isNotEmpty()) {
                    putString("selected_time", timeString)
                }
            }

            // Get or create the DoctorCalendarFragment
            val calendarFragment = DoctorCalendarFragment().apply {
                arguments = bundle
            }

            // Navigate to the calendar fragment and add transaction to back stack
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, calendarFragment)
                .addToBackStack("doctor_home")
                .commit()
            
            // Show toast confirming navigation
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

