package com.example.project.doctor

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.Admin.Patient
import com.example.project.R
import com.example.project.Service
import com.example.project.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.example.project.doctor.AppointmentAdapter
import com.example.project.doctor.AppointmentCalendar
import com.example.project.Booking
import com.example.project.doctor.WorkingHours
import java.text.SimpleDateFormat
import java.util.*

class DoctorCalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var addAppointmentButton: Button
    private lateinit var appointmentsRecyclerView: RecyclerView 
    private lateinit var workingHoursTextView: TextView
    private lateinit var editWorkingHoursButton: Button
    private lateinit var emptyAppointmentsView: View

    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var appointmentsListener: ListenerRegistration
    private lateinit var workingHoursListener: ListenerRegistration
    private val auth = FirebaseAuth.getInstance()
    private var doctorName: String = "Doctor"

    private var startHour = 8
    private var endHour = 14

    private var selectedDate = ""
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val appointmentsList = mutableListOf<AppointmentCalendar>()
    private lateinit var appointmentAdapter: AppointmentAdapter

    private var doctorServices = mutableListOf<Service>()
    private var filteredPatients = mutableListOf<Patient>()

    private val TAG = "DoctorCalendarFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_calendar, container, false)

        firestoreHelper = FirestoreHelper()
        
        calendarView = view.findViewById(R.id.calendarView)
        addAppointmentButton = view.findViewById(R.id.addAppointmentButton)
        appointmentsRecyclerView = view.findViewById(R.id.appointmentsRecyclerView) 
        workingHoursTextView = view.findViewById(R.id.workingHoursTextView)
        editWorkingHoursButton = view.findViewById(R.id.editWorkingHoursButton)
        emptyAppointmentsView = view.findViewById(R.id.emptyAppointmentsView)

        arguments?.getString("selected_date")?.let { selectedDateArg ->
            selectedDate = selectedDateArg
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(selectedDateArg)
                if (date != null) {
                    calendarView.date = date.time
                    Log.d(TAG, "Setting calendar to date: $selectedDateArg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting selected date: ${e.message}", e)
                selectedDate = dateFormatter.format(Date(calendarView.date))
            }
        } ?: run {
            selectedDate = dateFormatter.format(Date(calendarView.date))
        }

        setupListeners()

        if (activity != null && isAdded) {
            appointmentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            val highlightedTime = arguments?.getString("selected_time")

            appointmentAdapter = AppointmentAdapter(
                requireContext(),
                appointmentsList,
                highlightedTime
            ) { appointment ->
                showAppointmentOptions(appointment)
            }
            appointmentsRecyclerView.adapter = appointmentAdapter
        }

        if (isAdded) {
            loadDoctorDetails()
            loadServices()
            loadWorkingHours()
            loadAppointments()
        }

        return view
    }

    private fun setupListeners() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            loadAppointments()
        }

        addAppointmentButton.setOnClickListener {
            if (isAdded) {
                showAddAppointmentDialog()
            }
        }

        editWorkingHoursButton.setOnClickListener {
            if (isAdded) {
                showEditWorkingHoursDialog()
            }
        }
    }

    private fun loadDoctorDetails() {
        val doctorId = auth.currentUser?.uid ?: return
        
        firestoreHelper.getDoctorById(doctorId)
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                
                if (document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    doctorName = "Dr. $firstName $lastName"

                    if (isAdded) {
                        loadWorkingHours()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading doctor details", e)
            }
    }

    private fun loadServices() {
        val doctorId = auth.currentUser?.uid ?: return

        firestoreHelper.createSampleServiceIfNeeded(doctorId)
            .addOnCompleteListener {
                firestoreHelper.getServicesForDoctor(doctorId)
                    .addOnSuccessListener { result ->
                        doctorServices.clear()
                        for (document in result) {
                            val service = document.toObject(Service::class.java).copy(id = document.id)
                            doctorServices.add(service)
                        }

                        doctorServices.sortBy { it.name }
                        
                        Log.d(TAG, "Loaded ${doctorServices.size} services")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error loading services", e)
                    }
            }
    }

    private fun loadWorkingHours() {
        val doctorId = auth.currentUser?.uid ?: return

        if (::workingHoursListener.isInitialized) {
            workingHoursListener.remove()
        }

        val workingHoursRef = firestoreHelper.getDbInstance()
            .collection("doctorSettings")
            .document(doctorId)

        workingHoursListener = workingHoursRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen for working hours failed", e)
                if (context != null && isAdded) {
                    Toast.makeText(context, "Failed to load working hours: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val workingHours = snapshot.toObject(WorkingHours::class.java)
                if (workingHours != null) {
                    startHour = workingHours.startHour
                    endHour = workingHours.endHour
                    if (isAdded) {
                        updateWorkingHoursDisplay()
                        loadAppointments()
                    }
                } else {
                    saveWorkingHours(startHour, endHour)
                    if (isAdded) {
                        updateWorkingHoursDisplay()
                    }
                }
            } else {
                saveWorkingHours(startHour, endHour)
                if (isAdded) {
                    updateWorkingHoursDisplay()
                }
            }
        }
    }

    private fun updateWorkingHoursDisplay() {
        if (!isAdded) return
        
        try {
            workingHoursTextView.text = "$doctorName: ${getString(R.string.working_hours_format, startHour, endHour)}"
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Fragment not attached, cannot update working hours display", e)
        }
    }

    private fun loadAppointments() {
        val doctorId = auth.currentUser?.uid ?: return

        firestoreHelper.getBookingsForDoctor(doctorId, selectedDate)
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                appointmentsList.clear()
                if (snapshot != null && !snapshot.isEmpty) {
                    for (document in snapshot.documents) {
                        val booking = document.toObject(Booking::class.java) ?: continue
                        val appointment = AppointmentCalendar(
                            id = document.id,
                            patientName = booking.patient_name,
                            patientId = booking.patient_id,
                            date = booking.date,
                            timeSlot = booking.start_time,
                            endTime = booking.end_time,
                            serviceId = booking.service_id,
                            // You might need to fetch service details separately if they're not in the booking
                            serviceName = "Loading...", 
                            notes = booking.notes
                        )
                        appointmentsList.add(appointment)
                    }
                    appointmentsList.sortBy { it.timeSlot }
                    fetchServiceDetailsForAppointments()
                }
                updateAppointmentsUI()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Log.w(TAG, "Listen for appointments failed", e)
                Toast.makeText(context, "Failed to load appointments: ${e.message}", Toast.LENGTH_SHORT).show()
                updateAppointmentsUI()
            }
    }

    private fun fetchServiceDetailsForAppointments() {
        if (!isAdded) return
        val serviceIds = appointmentsList.map { it.serviceId }.distinct()
        for (serviceId in serviceIds) {
            if (serviceId.isEmpty()) continue
            firestoreHelper.getServiceById(serviceId).addOnSuccessListener { serviceDoc ->
                if (!isAdded) return@addOnSuccessListener
                if (serviceDoc.exists()) {
                    val serviceName = serviceDoc.getString("name") ?: "Unknown"
                    val servicePrice = serviceDoc.getLong("price")?.toInt() ?: 0
                    val serviceDuration = serviceDoc.getLong("duration_minutes")?.toInt() ?: 0
                    appointmentsList.filter { it.serviceId == serviceId }.forEach {
                        it.serviceName = serviceName
                        it.servicePrice = servicePrice
                        it.serviceDuration = serviceDuration
                    }
                    appointmentAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun updateAppointmentsUI() {
        if (!isAdded) return
        appointmentAdapter.notifyDataSetChanged()
        if (appointmentsList.isEmpty()) {
            emptyAppointmentsView.visibility = View.VISIBLE
            appointmentsRecyclerView.visibility = View.GONE
        } else {
            emptyAppointmentsView.visibility = View.GONE
            appointmentsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showAddAppointmentDialog() {
        if (!isAdded || context == null) {
            Log.e(TAG, "Fragment not attached, cannot show dialog")
            return
        }

        val availableTimeSlots = getAvailableTimeSlots()

        if (availableTimeSlots.isEmpty()) {
            Toast.makeText(requireContext(), "No available time slots for this day.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_appointment_with_search, null)

        val patientSearchEditText = dialogView.findViewById<EditText>(R.id.patientSearchEditText)
        val patientsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.patientsRecyclerView)
        val selectedPatientNameTextView = dialogView.findViewById<TextView>(R.id.selectedPatientNameTextView)
        val serviceSpinner = dialogView.findViewById<Spinner>(R.id.serviceSpinner)
        val timeSlotSpinner = dialogView.findViewById<Spinner>(R.id.timeSlotSpinner)
        val notesEditText = dialogView.findViewById<EditText>(R.id.notesEditText)
        val patientsAdapter = PatientSearchAdapter { patient ->
            selectedPatientNameTextView.text = "${patient.firstName} ${patient.lastName}"
            selectedPatientNameTextView.tag = patient // Store the patient object
            patientSearchEditText.setText("") // Clear search
            patientsRecyclerView.visibility = View.GONE
        }
        
        patientsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = patientsAdapter
        }

        patientSearchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 2) {
                    searchPatients(query) { patients ->
                        filteredPatients = patients
                        patientsAdapter.submitList(filteredPatients)
                        patientsRecyclerView.visibility = if (patients.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                } else {
                    patientsRecyclerView.visibility = View.GONE
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        val serviceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            doctorServices.map { it.name }
        )
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        serviceSpinner.adapter = serviceAdapter

        val timeSlotAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableTimeSlots
        )
        timeSlotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSlotSpinner.adapter = timeSlotAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Appointment")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val selectedPatient = selectedPatientNameTextView.tag as? Patient
                
                if (selectedPatient == null) {
                    Toast.makeText(requireContext(), "Please select a patient", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val servicePosition = serviceSpinner.selectedItemPosition
                if (servicePosition < 0 || servicePosition >= doctorServices.size) {
                    Toast.makeText(requireContext(), "Please select a service", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val selectedService = doctorServices[servicePosition]
                val timeSlot = timeSlotSpinner.selectedItem.toString()
                val notes = notesEditText.text.toString().trim()

                addAppointment(selectedPatient, selectedService, timeSlot, notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun searchPatients(query: String, callback: (MutableList<Patient>) -> Unit) {
        firestoreHelper.getDbInstance()
            .collection("patients")
            .orderBy("firstName")
            .get()
            .addOnSuccessListener { snapshot ->
                val patients = mutableListOf<Patient>()
                val lowerCaseQuery = query.lowercase(Locale.getDefault())
                for (document in snapshot.documents) {
                    val patient = document.toObject(Patient::class.java)
                    if (patient != null) {
                        val fullName = "${patient.firstName} ${patient.lastName}".lowercase(Locale.getDefault())
                        val email = patient.email?.lowercase(Locale.getDefault()) ?: ""
                        
                        if (fullName.contains(lowerCaseQuery) || 
                            patient.firstName.lowercase(Locale.getDefault()).startsWith(lowerCaseQuery) ||
                            patient.lastName.lowercase(Locale.getDefault()).startsWith(lowerCaseQuery) ||
                            email.contains(lowerCaseQuery)) {
                            patients.add(patient)
                        }
                    }
                }
                callback(patients)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error searching patients", e)
                callback(mutableListOf())
            }
    }

    private fun getAvailableTimeSlots(): List<String> {
        val allTimeSlots = mutableListOf<String>()
        val bookedTimeSlots = appointmentsList.map { it.timeSlot }

        for (hour in startHour until endHour) {
            val slot1 = String.format(Locale.US, "%02d:00", hour)
            val slot2 = String.format(Locale.US, "%02d:30", hour)
            if (slot1 !in bookedTimeSlots) {
                allTimeSlots.add(slot1)
            }
            if (slot2 !in bookedTimeSlots) {
                allTimeSlots.add(slot2)
            }
        }

        return allTimeSlots
    }

    private fun addAppointment(patient: Patient, service: Service, timeSlot: String, notes: String) {
        val doctorId = auth.currentUser?.uid ?: return
        val endTimeSlot = calculateEndTime(timeSlot, service.duration_minutes)

        val booking = Booking(
            doctor_id = doctorId,
            patient_id = patient.uid,
            patient_name = "${patient.firstName} ${patient.lastName}",
            service_id = service.id,
            date = selectedDate,
            start_time = timeSlot,
            end_time = endTimeSlot,
            status = "confirmed",
            notes = notes
        )

        firestoreHelper.addBooking(booking)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Appointment added successfully", Toast.LENGTH_SHORT).show()
                    loadAppointments() // Refresh the list
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Error adding appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun createBookingFromAppointment(patient: Patient, service: Service, appointment: AppointmentCalendar) {
        // This function is now redundant and can be removed.
        // The logic is handled directly in addAppointment.
    }
    
    private fun calculateEndTime(startTime: String, durationMinutes: Int): String {
        val parts = startTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        val totalMinutes = hour * 60 + minute + durationMinutes
        val newHour = totalMinutes / 60
        val newMinute = totalMinutes % 60
        
        return String.format(Locale.US, "%02d:%02d", newHour, newMinute)
    }

    private fun showAppointmentOptionsDialog(appointment: AppointmentCalendar) {
        if (!isAdded || context == null) {
            Log.e(TAG, "Fragment not attached, cannot show dialog")
            return
        }
        
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(requireContext())
            .setTitle("Appointment Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditAppointmentDialog(appointment)
                    1 -> showDeleteConfirmationDialog(appointment)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditAppointmentDialog(appointment: AppointmentCalendar) {
        if (!isAdded || context == null) {
            Log.e(TAG, "Fragment not attached, cannot show dialog")
            return
        }
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_appointment_with_service, null)
        val patientNameTextView = dialogView.findViewById<TextView>(R.id.patientNameTextView)
        val serviceSpinner = dialogView.findViewById<Spinner>(R.id.serviceSpinner)
        var timeSlotSpinner = dialogView.findViewById<Spinner>(R.id.timeSlotSpinner)
        val notesEditText = dialogView.findViewById<EditText>(R.id.notesEditText)

        patientNameTextView.text = appointment.patientName
        notesEditText.setText(appointment.notes)

        val serviceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            doctorServices.map { it.name }
        )
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        serviceSpinner.adapter = serviceAdapter

        val servicePosition = doctorServices.indexOfFirst { it.id == appointment.serviceId }
        if (servicePosition >= 0) {
            serviceSpinner.setSelection(servicePosition)
        }

        val availableTimeSlots = getAvailableTimeSlots().toMutableList()
        if (appointment.timeSlot !in availableTimeSlots) {
            availableTimeSlots.add(appointment.timeSlot)
            availableTimeSlots.sort()
        }

        val timeSlotAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            availableTimeSlots
        )
        timeSlotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSlotSpinner.adapter = timeSlotAdapter

        val currentPosition = availableTimeSlots.indexOf(appointment.timeSlot)
        if (currentPosition >= 0) {
            timeSlotSpinner.setSelection(currentPosition)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Appointment")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val servicePosition = serviceSpinner.selectedItemPosition
                if (servicePosition < 0 || servicePosition >= doctorServices.size) {
                    Toast.makeText(requireContext(), "Please select a service", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val selectedService = doctorServices[servicePosition]
                val timeSlot = timeSlotSpinner.selectedItem.toString()
                val notes = notesEditText.text.toString().trim()

                updateAppointment(appointment.id, appointment.patientName, appointment.patientId, 
                    selectedService, timeSlot, notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAppointment(id: String, patientName: String, patientId: String, 
                                service: Service, timeSlot: String, notes: String) {
        val doctorId = auth.currentUser?.uid ?: return

        val endTimeSlot = calculateEndTime(timeSlot, service.duration_minutes)
        
        val updatedAppointment = AppointmentCalendar(
            id = id,
            patientName = patientName,
            patientId = patientId,
            date = selectedDate,
            timeSlot = timeSlot,
            notes = notes,
            time = timeSlot,
            endTime = endTimeSlot,
            serviceId = service.id,
            serviceName = service.name,
            servicePrice = service.price,
            serviceDuration = service.duration_minutes
        )

        val appointmentRef = firestoreHelper.getDbInstance()
            .collection("doctorCalendars")
            .document(doctorId)
            .collection("dates")
            .document(selectedDate)
            .collection("appointments")
            .document(id)

        appointmentRef.set(updatedAppointment)
            .addOnSuccessListener {
                if (isAdded && context != null) {
                    Toast.makeText(context, "Appointment updated successfully", Toast.LENGTH_SHORT).show()
                }

                updateAppointmentInFirestore(updatedAppointment)
            }
            .addOnFailureListener { e ->
                if (isAdded && context != null) {
                    Toast.makeText(context, "Error updating appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun updateAppointmentInFirestore(updatedAppointment: AppointmentCalendar) {
        val doctorId = auth.currentUser?.uid ?: return

        val bookingUpdate = mapOf(
            "service_id" to updatedAppointment.serviceId,
            "start_time" to updatedAppointment.timeSlot,
            "end_time" to updatedAppointment.endTime,
            "notes" to updatedAppointment.notes
            // Note: patient and doctor details are generally not editable from this dialog
        )

        firestoreHelper.getDbInstance().collection("bookings").document(updatedAppointment.id)
            .update(bookingUpdate)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Appointment updated successfully.", Toast.LENGTH_SHORT).show()
                    loadAppointments()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Error updating appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun updateBookingFromAppointment(updatedAppointment: AppointmentCalendar) {
        // This function is redundant now.
    }

    private fun showDeleteConfirmationDialog(appointment: AppointmentCalendar) {
        if (!isAdded || context == null) {
            Log.e(TAG, "Fragment not attached, cannot show dialog")
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Appointment")
            .setMessage("Are you sure you want to delete the appointment for ${appointment.patientName} at ${appointment.timeSlot}?")
            .setPositiveButton("Yes, Delete") { _, _ ->
                deleteAppointment(appointment)
            }
            .setNegativeButton("No, Cancel", null)
            .show()
    }

    private fun deleteAppointment(appointment: AppointmentCalendar) {
        firestoreHelper.getDbInstance().collection("bookings").document(appointment.id).delete()
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Appointment deleted successfully", Toast.LENGTH_SHORT).show()
                    loadAppointments()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Error deleting appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun deleteBookingFromAppointment(appointment: AppointmentCalendar) {
        // This is now redundant.
    }

    private fun showEditWorkingHoursDialog() {
        if (!isAdded || context == null) {
            Log.e(TAG, "Fragment not attached, cannot show dialog")
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_edit_working_hours, null)
        val startTimeButton = dialogView.findViewById<Button>(R.id.startTimeButton)
        val endTimeButton = dialogView.findViewById<Button>(R.id.endTimeButton)

        startTimeButton.text = String.format(Locale.US, "%d:00", startHour)
        endTimeButton.text = String.format(Locale.US, "%d:00", endHour)

        var tempStartHour = startHour
        var tempEndHour = endHour

        startTimeButton.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, _ ->
                    tempStartHour = hourOfDay
                    startTimeButton.text = String.format(Locale.US, "%d:00", tempStartHour)
                },
                tempStartHour,
                0,
                true
            ).show()
        }

        endTimeButton.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, _ ->
                    tempEndHour = hourOfDay
                    endTimeButton.text = String.format(Locale.US, "%d:00", tempEndHour)
                },
                tempEndHour,
                0,
                true
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Working Hours")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                if (tempStartHour >= tempEndHour) {
                    if (isAdded) {
                        Toast.makeText(context, "End time must be after start time", Toast.LENGTH_SHORT).show()
                    }
                    return@setPositiveButton
                }
                startHour = tempStartHour
                endHour = tempEndHour
                saveWorkingHours(startHour, endHour)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveWorkingHours(start: Int, end: Int) {
        val doctorId = auth.currentUser?.uid ?: return
        
        val workingHours = WorkingHours(startHour = start, endHour = end)

        val workingHoursRef = firestoreHelper.getDbInstance()
            .collection("doctorSettings")
            .document(doctorId)

        workingHoursRef.set(workingHours)
            .addOnSuccessListener {
                if (isAdded && context != null) {
                    safelyShowToast("Working hours updated successfully")
                }
            }
            .addOnFailureListener { e ->
                if (isAdded && context != null) {
                    safelyShowToast("Error updating working hours: ${e.message}")
                }
            }
    }

    override fun onDestroyView() {
        if (::appointmentsListener.isInitialized) {
            appointmentsListener.remove()
        }
        if (::workingHoursListener.isInitialized) {
            workingHoursListener.remove()
        }
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        if (::appointmentsListener.isInitialized) {
            appointmentsListener.remove()
        }
        if (::workingHoursListener.isInitialized) {
            workingHoursListener.remove()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (isAdded && activity != null) {
            loadWorkingHours()
            loadAppointments()
        }
    }

    private fun safelyRunOnUiThread(action: () -> Unit) {
        if (isAdded && activity != null) {
            try {
                activity?.runOnUiThread {
                    if (isAdded && !isDetached) {
                        action()
                    }
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Fragment not attached to activity", e)
            }
        }
    }

    private fun safelyShowToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppointmentOptions(appointment: AppointmentCalendar) {
        if (!isAdded || context == null) {
            Log.e(TAG, "Fragment not attached, cannot show dialog")
            return
        }
        
        val options = arrayOf("View Details", "Edit Appointment", "Delete Appointment")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Appointment Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAppointmentDetails(appointment)
                    1 -> showEditAppointmentDialog(appointment)
                    2 -> showDeleteConfirmationDialog(appointment)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAppointmentDetails(appointment: AppointmentCalendar) {
        if (!isAdded || context == null) {
            Log.e(TAG, "Fragment not attached, cannot show dialog")
            return
        }

        val timeRange = "${appointment.timeSlot} - ${appointment.endTime}"
        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale.US)
        val formattedPrice = currencyFormat.format(appointment.servicePrice)
        
        val message = """
            Patient: ${appointment.patientName}
            Time: $timeRange
            Service: ${appointment.serviceName}
            Duration: ${appointment.serviceDuration} minutes
            Price: $formattedPrice
            
            ${if (appointment.notes.isNotEmpty()) "Notes: ${appointment.notes}" else "No notes provided"}
        """.trimIndent()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Appointment Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }
}

class PatientSearchAdapter(private val onPatientSelected: (Patient) -> Unit) : 
    RecyclerView.Adapter<PatientSearchAdapter.PatientViewHolder>() {
    
    private var patients = listOf<Patient>()
    
    fun submitList(list: List<Patient>) {
        patients = list
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_search, parent, false)
        return PatientViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patients[position])
    }
    
    override fun getItemCount() = patients.size
    
    inner class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val patientNameTextView: TextView = itemView.findViewById(R.id.patientNameTextView)
        private val patientEmailTextView: TextView = itemView.findViewById(R.id.patientEmailTextView)
        
        fun bind(patient: Patient) {
            patientNameTextView.text = "${patient.firstName} ${patient.lastName}"
            patientEmailTextView.text = patient.email
            
            itemView.setOnClickListener {
                onPatientSelected(patient)
            }
        }
    }
}