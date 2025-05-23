package com.example.project.Admin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.Admin.AdminAppointmentItem
import com.example.project.Admin.AdminAppointmentAdapter
import com.example.project.R
import com.example.project.Service
import com.example.project.util.FirestoreHelper
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class AdminAppointmentsFragment : Fragment() {

    private val TAG = "AdminAppointmentsFragment"
    
    // UI components
    private lateinit var appointmentsRecyclerView: RecyclerView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateTextView: TextView
    private lateinit var statusFilterSpinner: Spinner
    private lateinit var sortBySpinner: Spinner
    private lateinit var dateFilterButton: Button
    private lateinit var clearFiltersButton: Button
    
    // Data
    private lateinit var appointmentAdapter: AdminAppointmentAdapter
    private val appointmentList = mutableListOf<AdminAppointmentItem>()
    private val allAppointments = mutableListOf<AdminAppointmentItem>()
    private val doctorMap = mutableMapOf<String, String>() // doctorId -> doctorName
    private val patientMap = mutableMapOf<String, String>() // patientId -> patientName
    private val serviceMap = mutableMapOf<String, Service>() // serviceId -> Service
    
    // Firebase
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var db: FirebaseFirestore
    
    // Filters
    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null
    private var selectedStatus: String? = null
    private var currentSortOption = SortOption.DATE_ASC

    // Sort options enum
    private enum class SortOption {
        DATE_ASC, DATE_DESC, PATIENT_NAME, DOCTOR_NAME, STATUS
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_appointments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        firestoreHelper = FirestoreHelper()
        db = firestoreHelper.getDbInstance()
        
        // Initialize UI components
        appointmentsRecyclerView = view.findViewById(R.id.appointmentsRecyclerView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        statusFilterSpinner = view.findViewById(R.id.statusFilterSpinner)
        sortBySpinner = view.findViewById(R.id.sortBySpinner)
        dateFilterButton = view.findViewById(R.id.dateFilterButton)
        clearFiltersButton = view.findViewById(R.id.clearFiltersButton)
        
        // Setup RecyclerView
        appointmentAdapter = AdminAppointmentAdapter(
            onEditClick = { appointment -> showEditAppointmentDialog(appointment) },
            onDeleteClick = { appointment -> showDeleteConfirmationDialog(appointment) }
        )
        
        appointmentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = appointmentAdapter
        }
        
        // Setup spinners
        setupStatusSpinner()
        setupSortSpinner()
        
        // Setup filters
        setupDateFilter()
        setupClearFilters()
        
        // Load data - load in sequence to ensure all reference data is available
        loadDoctors { 
            loadPatients { 
                loadServices { 
                    loadAppointments() 
                }
            }
        }
    }
    
    private fun setupStatusSpinner() {
        val statusOptions = arrayOf("All Statuses", "Confirmed", "Pending", "Cancelled")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        statusFilterSpinner.adapter = adapter
        statusFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStatus = when (position) {
                    0 -> null // All
                    else -> statusOptions[position].lowercase(Locale.ROOT)
                }
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedStatus = null
            }
        }
    }
    
    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Date (Ascending)", "Date (Descending)", "Patient Name", "Doctor Name", "Status")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        sortBySpinner.adapter = adapter
        sortBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortOption = when (position) {
                    0 -> SortOption.DATE_ASC
                    1 -> SortOption.DATE_DESC
                    2 -> SortOption.PATIENT_NAME
                    3 -> SortOption.DOCTOR_NAME
                    4 -> SortOption.STATUS
                    else -> SortOption.DATE_ASC
                }
                applyFilters() // This will also apply the sort
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentSortOption = SortOption.DATE_ASC
            }
        }
    }
    
    private fun setupDateFilter() {
        dateFilterButton.setOnClickListener {
            showDateRangePickerDialog()
        }
    }
    
    private fun setupClearFilters() {
        clearFiltersButton.setOnClickListener {
            // Reset filters
            selectedStartDate = null
            selectedEndDate = null
            selectedStatus = null
            
            // Reset UI
            statusFilterSpinner.setSelection(0)
            sortBySpinner.setSelection(0)
            dateFilterButton.text = "Select dates"
            
            // Apply (clear) filters
            applyFilters()
        }
    }
    
    private fun showDateRangePickerDialog() {
        val calendar = Calendar.getInstance()
        
        // Show start date picker
        val startDateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedStartDate = calendar.clone() as Calendar
            
            // After selecting start date, show end date picker
            val endDateListener = DatePickerDialog.OnDateSetListener { _, endYear, endMonth, endDayOfMonth ->
                val endCalendar = Calendar.getInstance()
                endCalendar.set(Calendar.YEAR, endYear)
                endCalendar.set(Calendar.MONTH, endMonth)
                endCalendar.set(Calendar.DAY_OF_MONTH, endDayOfMonth)
                selectedEndDate = endCalendar.clone() as Calendar
                
                // Update button text
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDateStr = dateFormat.format(selectedStartDate!!.time)
                val endDateStr = dateFormat.format(selectedEndDate!!.time)
                dateFilterButton.text = "$startDateStr to $endDateStr"
                
                // Apply filters
                applyFilters()
            }
            
            // Show end date picker, defaulting to 7 days after start date
            val endCalendar = calendar.clone() as Calendar
            endCalendar.add(Calendar.DAY_OF_YEAR, 7)
            DatePickerDialog(
                requireContext(),
                endDateListener,
                endCalendar.get(Calendar.YEAR),
                endCalendar.get(Calendar.MONTH),
                endCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        DatePickerDialog(
            requireContext(),
            startDateListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun loadDoctors(onComplete: () -> Unit) {
        db.collection("doctors")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val doctorId = document.id
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val doctorName = "Dr. $firstName $lastName"
                    doctorMap[doctorId] = doctorName
                    Log.d(TAG, "Loaded doctor: $doctorId -> $doctorName")
                }
                Log.d(TAG, "Loaded ${doctorMap.size} doctors")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading doctors", e)
                onComplete() // Still continue even if there's an error
            }
    }
    
    private fun loadPatients(onComplete: () -> Unit) {
        db.collection("patients")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val patientId = document.id
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val patientName = "$firstName $lastName"
                    patientMap[patientId] = patientName
                    patientMap[document.getString("email") ?: ""] = patientName // Also map by email
                    Log.d(TAG, "Loaded patient: $patientId -> $patientName")
                }
                Log.d(TAG, "Loaded ${patientMap.size} patients")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading patients", e)
                onComplete() // Still continue even if there's an error
            }
    }
    
    private fun loadServices(onComplete: () -> Unit) {
        db.collection("services")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val serviceId = document.id
                    val name = document.getString("name") ?: "Unknown Service"
                    val price = document.getLong("price")?.toInt() ?: 0
                    val doctorId = document.getString("doctor_id") ?: ""
                    val durationMinutes = document.getLong("duration_minutes")?.toInt() ?: 30
                    
                    val service = Service(
                        id = serviceId,
                        doctor_id = doctorId,
                        name = name,
                        price = price,
                        duration_minutes = durationMinutes
                    )
                    
                    serviceMap[serviceId] = service
                    Log.d(TAG, "Loaded service: $serviceId -> $name (Doctor: $doctorId)")
                }
                Log.d(TAG, "Loaded ${serviceMap.size} services")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading services", e)
                onComplete() // Still continue even if there's an error
            }
    }
    
    private fun loadAppointments() {
        showLoading(true)
        
        // Clear existing data
        allAppointments.clear()
        
        // Get all bookings from Firestore
        db.collection("bookings")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val id = document.id
                    val doctorId = document.getString("doctor_id") ?: ""
                    val patientId = document.getString("patient_name") ?: "" // Using patient_name field which contains email/id
                    val serviceId = document.getString("service_id") ?: ""
                    val date = document.getString("date") ?: ""
                    val startTime = document.getString("start_time") ?: ""
                    val endTime = document.getString("end_time") ?: ""
                    val status = document.getString("status") ?: "confirmed"
                    val notes = document.getString("notes") ?: ""
                    
                    Log.d(TAG, "Processing appointment: ID=$id, Doctor=$doctorId, Patient=$patientId, Service=$serviceId")
                    
                    // Get names from maps
                    val doctorName = doctorMap[doctorId] ?: "Unknown Doctor ($doctorId)"
                    val patientName = patientMap[patientId] ?: patientId // Fall back to ID/email if name not found
                    val service = serviceMap[serviceId]
                    
                    // Log for debugging
                    if (service == null) {
                        Log.w(TAG, "Service not found: $serviceId")
                    }
                    if (!doctorMap.containsKey(doctorId)) {
                        Log.w(TAG, "Doctor not found: $doctorId")
                    }
                    
                    val appointmentItem = AdminAppointmentItem(
                        id = id,
                        doctorId = doctorId,
                        doctorName = doctorName,
                        patientId = patientId,
                        patientName = patientName,
                        serviceId = serviceId,
                        serviceName = service?.name ?: "Unknown Service ($serviceId)",
                        servicePrice = service?.price ?: 0,
                        serviceDuration = service?.duration_minutes ?: 30,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        status = status,
                        notes = notes
                    )
                    
                    allAppointments.add(appointmentItem)
                }
                
                Log.d(TAG, "Loaded ${allAppointments.size} appointments")
                
                // Apply filters to show initial data
                applyFilters()
                
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading appointments", e)
                showLoading(false)
                showEmptyState(true, "Error loading appointments: ${e.message}")
            }
    }
    
    private fun applyFilters() {
        val filteredList = allAppointments.filter { appointment ->
            var matches = true
            
            // Apply status filter if selected
            if (selectedStatus != null) {
                matches = matches && appointment.status.equals(selectedStatus, ignoreCase = true)
            }
            
            // Apply date range filter if selected
            if (selectedStartDate != null && selectedEndDate != null) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val appointmentDate = dateFormat.parse(appointment.date)
                    val startDate = selectedStartDate!!.time
                    val endDate = selectedEndDate!!.time
                    
                    // Add one day to end date to include the end date in the range
                    val endDatePlusOneDay = selectedEndDate!!.clone() as Calendar
                    endDatePlusOneDay.add(Calendar.DAY_OF_YEAR, 1)
                    
                    matches = matches && appointmentDate != null && 
                              !appointmentDate.before(startDate) && 
                              appointmentDate.before(endDatePlusOneDay.time)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date", e)
                }
            }
            
            matches
        }
        
        // Apply sorting
        val sortedList = when (currentSortOption) {
            SortOption.DATE_ASC -> filteredList.sortedWith(compareBy({ it.date }, { it.startTime }))
            SortOption.DATE_DESC -> filteredList.sortedWith(compareByDescending<AdminAppointmentItem> { it.date }
                                      .thenByDescending { it.startTime })
            SortOption.PATIENT_NAME -> filteredList.sortedBy { it.patientName }
            SortOption.DOCTOR_NAME -> filteredList.sortedBy { it.doctorName }
            SortOption.STATUS -> filteredList.sortedBy { it.status }
        }
        
        // Update adapter with filtered & sorted list
        appointmentList.clear()
        appointmentList.addAll(sortedList)
        appointmentAdapter.updateAppointments(appointmentList)
        
        // Show empty state if no appointments found
        showEmptyState(appointmentList.isEmpty())
    }
    
    private fun showEmptyState(show: Boolean, message: String = "No appointments found") {
        emptyStateTextView.text = message
        emptyStateTextView.visibility = if (show) View.VISIBLE else View.GONE
        appointmentsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            appointmentsRecyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.GONE
        }
    }
    
    private fun showEditAppointmentDialog(appointment: AdminAppointmentItem) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_appointment, null)
        
        // Get dialog views
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.editDateInput)
        val startTimeInput = dialogView.findViewById<TextInputEditText>(R.id.editStartTimeInput)
        val endTimeInput = dialogView.findViewById<TextInputEditText>(R.id.editEndTimeInput)
        val doctorSpinner = dialogView.findViewById<Spinner>(R.id.editDoctorSpinner)
        val patientAutoComplete = dialogView.findViewById<AutoCompleteTextView>(R.id.editPatientAutoComplete)
        val serviceSpinner = dialogView.findViewById<Spinner>(R.id.editServiceSpinner)
        val statusSpinner = dialogView.findViewById<Spinner>(R.id.editStatusSpinner)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.editNotesInput)
        
        // Set initial values
        dateInput.setText(appointment.date)
        startTimeInput.setText(appointment.startTime)
        endTimeInput.setText(appointment.endTime)
        notesInput.setText(appointment.notes)
        
        // Setup date picker
        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateInput.setText(dateFormat.format(selectedDate.time))
            }, currentYear, currentMonth, currentDay).show()
        }
        
        // Setup time pickers
        startTimeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                startTimeInput.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute))
            }, currentHour, currentMinute, true).show()
        }
        
        endTimeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                endTimeInput.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute))
            }, currentHour, currentMinute, true).show()
        }
        
        // Setup doctor spinner
        val doctorNames = mutableListOf<String>()
        val doctorIds = mutableListOf<String>()
        doctorMap.forEach { (id, name) ->
            doctorNames.add(name)
            doctorIds.add(id)
        }
        val doctorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, doctorNames)
        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        doctorSpinner.adapter = doctorAdapter
        
        // Set selected doctor
        val doctorIndex = doctorIds.indexOf(appointment.doctorId)
        if (doctorIndex >= 0) {
            doctorSpinner.setSelection(doctorIndex)
        }
        
        // Setup patient autocomplete
        val patientNames = mutableListOf<String>()
        val patientIds = mutableListOf<String>()
        patientMap.forEach { (id, name) ->
            patientNames.add(name)
            patientIds.add(id)
        }
        val patientAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, patientNames)
        patientAutoComplete.setAdapter(patientAdapter)
        patientAutoComplete.setText(appointment.patientName)
        
        // Setup service spinner
        val serviceList = serviceMap.values.filter { it.doctor_id == appointment.doctorId }
        val serviceNames = serviceList.map { it.name }
        val serviceIds = serviceList.map { it.id }
        val serviceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, serviceNames)
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        serviceSpinner.adapter = serviceAdapter
        
        // Set selected service
        val serviceIndex = serviceIds.indexOf(appointment.serviceId)
        if (serviceIndex >= 0) {
            serviceSpinner.setSelection(serviceIndex)
        }
        
        // Update services when doctor changes
        doctorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDoctorId = doctorIds[position]
                val doctorServices = serviceMap.values.filter { it.doctor_id == selectedDoctorId }
                val updatedServiceNames = doctorServices.map { it.name }
                val updatedServiceIds = doctorServices.map { it.id }
                
                val updatedServiceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, updatedServiceNames)
                updatedServiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                serviceSpinner.adapter = updatedServiceAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Setup status spinner
        val statusOptions = arrayOf("Confirmed", "Pending", "Cancelled")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        statusSpinner.adapter = statusAdapter
        
        // Set selected status
        val statusIndex = statusOptions.indexOfFirst { it.equals(appointment.status, ignoreCase = true) }
        if (statusIndex >= 0) {
            statusSpinner.setSelection(statusIndex)
        }
        
        // Create and show the dialog
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Get updated values
                val updatedDate = dateInput.text.toString()
                val updatedStartTime = startTimeInput.text.toString()
                val updatedEndTime = endTimeInput.text.toString()
                val updatedDoctorId = doctorIds[doctorSpinner.selectedItemPosition]
                val updatedDoctorName = doctorNames[doctorSpinner.selectedItemPosition]
                
                // Find patient ID from name
                val patientText = patientAutoComplete.text.toString()
                val patientEntry = patientMap.entries.find { it.value == patientText }
                val updatedPatientId = patientEntry?.key ?: appointment.patientId
                val updatedPatientName = patientEntry?.value ?: patientText
                
                // Get service based on spinner selection
                val doctorServices = serviceMap.values.filter { it.doctor_id == updatedDoctorId }
                val updatedService = if (serviceSpinner.selectedItemPosition >= 0 && 
                                        serviceSpinner.selectedItemPosition < doctorServices.size) {
                    doctorServices[serviceSpinner.selectedItemPosition]
                } else {
                    serviceMap[appointment.serviceId] // Fallback to current service
                }
                
                val updatedStatus = statusOptions[statusSpinner.selectedItemPosition].lowercase(Locale.ROOT)
                val updatedNotes = notesInput.text.toString()
                
                // Update appointment
                val updatedAppointment = AdminAppointmentItem(
                    id = appointment.id,
                    doctorId = updatedDoctorId,
                    doctorName = updatedDoctorName,
                    patientId = updatedPatientId,
                    patientName = updatedPatientName,
                    serviceId = updatedService?.id ?: appointment.serviceId,
                    serviceName = updatedService?.name ?: appointment.serviceName,
                    servicePrice = updatedService?.price ?: appointment.servicePrice,
                    serviceDuration = updatedService?.duration_minutes ?: appointment.serviceDuration,
                    date = updatedDate,
                    startTime = updatedStartTime,
                    endTime = updatedEndTime,
                    notes = updatedNotes,
                    status = updatedStatus
                )
                
                updateAppointment(updatedAppointment)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
    
    private fun updateAppointment(appointment: AdminAppointmentItem) {
        // Update in Firestore
        db.collection("bookings")
            .document(appointment.id)
            .update(
                mapOf(
                    "doctor_id" to appointment.doctorId,
                    "patient_name" to appointment.patientId,
                    "service_id" to appointment.serviceId,
                    "date" to appointment.date,
                    "start_time" to appointment.startTime,
                    "end_time" to appointment.endTime,
                    "status" to appointment.status,
                    "notes" to appointment.notes
                )
            )
            .addOnSuccessListener {
                Toast.makeText(context, "Appointment updated successfully", Toast.LENGTH_SHORT).show()
                
                // Also update in the doctor's calendar if needed
                updateAppointmentInDoctorCalendar(appointment)
                
                // Refresh the appointment list
                loadAppointments()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error updating appointment", e)
            }
    }
    
    private fun updateAppointmentInDoctorCalendar(appointment: AdminAppointmentItem) {
        // First, check if there is an existing appointment in the doctor's calendar
        db.collection("doctorCalendars")
            .document(appointment.doctorId)
            .collection("dates")
            .document(appointment.date)
            .collection("appointments")
            .whereEqualTo("patientId", appointment.patientId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Update existing appointment
                    for (document in querySnapshot.documents) {
                        document.reference.update(
                            mapOf(
                                "patientName" to appointment.patientName,
                                "patientId" to appointment.patientId,
                                "timeSlot" to appointment.startTime,
                                "endTime" to appointment.endTime,
                                "serviceId" to appointment.serviceId,
                                "serviceName" to appointment.serviceName,
                                "servicePrice" to appointment.servicePrice,
                                "serviceDuration" to appointment.serviceDuration,
                                "notes" to appointment.notes
                            )
                        )
                    }
                } else {
                    // If status is "cancelled", we don't need to create a new calendar entry
                    if (appointment.status == "cancelled") {
                        return@addOnSuccessListener
                    }
                    
                    // Create new appointment in doctor's calendar
                    val calendarAppointment = hashMapOf(
                        "patientName" to appointment.patientName,
                        "patientId" to appointment.patientId,
                        "date" to appointment.date,
                        "timeSlot" to appointment.startTime,
                        "time" to appointment.startTime, // For compatibility
                        "endTime" to appointment.endTime,
                        "serviceId" to appointment.serviceId,
                        "serviceName" to appointment.serviceName,
                        "servicePrice" to appointment.servicePrice,
                        "serviceDuration" to appointment.serviceDuration,
                        "notes" to appointment.notes
                    )
                    
                    db.collection("doctorCalendars")
                        .document(appointment.doctorId)
                        .collection("dates")
                        .document(appointment.date)
                        .collection("appointments")
                        .add(calendarAppointment)
                }
            }
    }
    
    private fun showDeleteConfirmationDialog(appointment: AdminAppointmentItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Appointment")
            .setMessage("Are you sure you want to delete the appointment for ${appointment.patientName} on ${appointment.date} at ${appointment.startTime}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteAppointment(appointment)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
    
    private fun deleteAppointment(appointment: AdminAppointmentItem) {
        // Delete from bookings collection
        db.collection("bookings")
            .document(appointment.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Appointment deleted successfully", Toast.LENGTH_SHORT).show()
                
                // Also delete from doctor's calendar
                db.collection("doctorCalendars")
                    .document(appointment.doctorId)
                    .collection("dates")
                    .document(appointment.date)
                    .collection("appointments")
                    .whereEqualTo("patientId", appointment.patientId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot.documents) {
                            document.reference.delete()
                        }
                    }
                
                // Remove from list and update adapter
                val index = appointmentList.indexOfFirst { it.id == appointment.id }
                if (index >= 0) {
                    appointmentList.removeAt(index)
                    appointmentAdapter.updateAppointments(appointmentList)
                    
                    // Also remove from allAppointments
                    val allIndex = allAppointments.indexOfFirst { it.id == appointment.id }
                    if (allIndex >= 0) {
                        allAppointments.removeAt(allIndex)
                    }
                    
                    // Show empty state if no appointments left
                    showEmptyState(appointmentList.isEmpty())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error deleting appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error deleting appointment", e)
            }
    }
}