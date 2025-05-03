package com.example.project.Doctor

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.project.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DoctorCalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var addAppointmentButton: Button
    private lateinit var appointmentsListView: ListView
    private lateinit var workingHoursTextView: TextView
    private lateinit var editWorkingHoursButton: Button

    // Firebase references
    private lateinit var database: DatabaseReference
    private lateinit var appointmentsRef: DatabaseReference
    private lateinit var workingHoursRef: DatabaseReference
    private val auth = FirebaseAuth.getInstance()

    // Working hours
    private var startHour = 8
    private var endHour = 14

    // Selected date
    private var selectedDate = ""
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Appointments list - Changed to AppointmentCalendar
    private val appointmentsList = mutableListOf<AppointmentCalendar>()
    private lateinit var appointmentAdapter: AppointmentAdapter // Adapter should be using AppointmentCalendar now

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_calendar, container, false)

        // Initialize Firebase
        val doctorId = auth.currentUser?.uid ?: "unknown_doctor" // Handle potential null UID
        database = FirebaseDatabase.getInstance().reference
        appointmentsRef = database.child("doctors").child(doctorId).child("appointments")
        workingHoursRef = database.child("doctors").child(doctorId).child("workingHours")

        // Initialize views
        calendarView = view.findViewById(R.id.calendarView)
        addAppointmentButton = view.findViewById(R.id.addAppointmentButton)
        appointmentsListView = view.findViewById(R.id.appointmentsListView)
        workingHoursTextView = view.findViewById(R.id.workingHoursTextView)
        editWorkingHoursButton = view.findViewById(R.id.editWorkingHoursButton)

        // Initialize adapter with the correct list type
        appointmentAdapter = AppointmentAdapter(requireContext(), appointmentsList)
        appointmentsListView.adapter = appointmentAdapter

        // Set initial date
        selectedDate = dateFormatter.format(Date(calendarView.date))

        setupListeners()
        loadWorkingHours()
        loadAppointments() // Load appointments for the initial date

        return view
    }

    private fun setupListeners() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Calendar month is 0-based, so add 1
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            // Load appointments for the newly selected date
            loadAppointments()
        }

        addAppointmentButton.setOnClickListener {
            showAddAppointmentDialog()
        }

        editWorkingHoursButton.setOnClickListener {
            showEditWorkingHoursDialog()
        }

        // Use the correct type (AppointmentCalendar) for the item
        appointmentsListView.setOnItemLongClickListener { _, _, position, _ ->
            val appointment = appointmentsList[position] // Type is AppointmentCalendar
            showAppointmentOptionsDialog(appointment)
            true // Indicate that the click was handled
        }
    }

    private fun loadWorkingHours() {
        workingHoursRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Make sure WorkingHours class matches the data structure in Firebase
                    val workingHours = snapshot.getValue(WorkingHours::class.java)
                    if (workingHours != null) {
                        startHour = workingHours.startHour
                        endHour = workingHours.endHour
                        updateWorkingHoursDisplay()
                        // Reload appointments in case working hours changed which might affect available slots display
                        loadAppointments()
                    } else {
                        // Data exists but couldn't be parsed, maybe log an error
                        saveWorkingHours(startHour, endHour) // Optionally save defaults
                        updateWorkingHoursDisplay()
                    }
                } else {
                    // Node doesn't exist, set default working hours and save them
                    saveWorkingHours(startHour, endHour)
                    updateWorkingHoursDisplay()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (context != null) {
                    Toast.makeText(context, "Failed to load working hours: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateWorkingHoursDisplay() {
        workingHoursTextView.text = getString(R.string.working_hours_format, startHour, endHour) // Use string resource
        // Example string resource: <string name="working_hours_format">Working Hours: %d:00 - %d:00</string>
    }

    private fun loadAppointments() {
        // Remove previous listener to avoid multiple listeners on date change
        appointmentsRef.child(selectedDate).removeEventListener(appointmentValueListener)
        // Add new listener for the selected date
        appointmentsRef.child(selectedDate).addValueEventListener(appointmentValueListener)
    }

    // Define the listener separately to easily remove it
    private val appointmentValueListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            appointmentsList.clear()
            for (appointmentSnapshot in snapshot.children) {
                // Deserialize as AppointmentCalendar
                val appointment = appointmentSnapshot.getValue(AppointmentCalendar::class.java)
                if (appointment != null) {
                    // Assign the Firebase key as the ID
                    appointment.id = appointmentSnapshot.key ?: ""
                    appointmentsList.add(appointment)
                }
            }

            // Sort appointments by timeSlot (assuming it's like "HH:mm")
            appointmentsList.sortBy { it.timeSlot }
            // Notify the adapter that the data has changed
            appointmentAdapter.notifyDataSetChanged()
            // Update visibility based on whether the list is empty
            if (appointmentsList.isEmpty()) {
                appointmentsListView.visibility = View.GONE
                // Optionally show a TextView saying "No appointments"
            } else {
                appointmentsListView.visibility = View.VISIBLE
            }
        }

        override fun onCancelled(error: DatabaseError) {
            if (context != null) {
                Toast.makeText(context, "Failed to load appointments: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showAddAppointmentDialog() {
        // Get available time slots
        val availableTimeSlots = getAvailableTimeSlots()

        if (availableTimeSlots.isEmpty()) {
            if (context != null) {
                Toast.makeText(context, "No available time slots for this day.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_add_appointment, null) // Ensure layout name matches
        val patientNameEditText = dialogView.findViewById<EditText>(R.id.patientNameEditText)
        val timeSlotSpinner = dialogView.findViewById<Spinner>(R.id.timeSlotSpinner)
        val notesEditText = dialogView.findViewById<EditText>(R.id.notesEditText)

        // Set up spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableTimeSlots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSlotSpinner.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Appointment")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val patientName = patientNameEditText.text.toString().trim()
                val timeSlot = timeSlotSpinner.selectedItem.toString()
                val notes = notesEditText.text.toString().trim()

                if (patientName.isEmpty()) {
                    if (context != null) {
                        Toast.makeText(context, "Patient name cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                    // Don't dismiss the dialog - consider keeping it open or showing error differently
                    // For simplicity, we just return here. Re-showing dialog requires more complex setup.
                    return@setPositiveButton
                }

                addAppointment(patientName, timeSlot, notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getAvailableTimeSlots(): List<String> {
        val allTimeSlots = mutableListOf<String>()
        // Use timeSlot from AppointmentCalendar
        val bookedTimeSlots = appointmentsList.map { it.timeSlot }.toSet() // Use Set for efficient lookup

        // Generate all possible 30-minute time slots based on working hours
        for (hour in startHour until endHour) {
            val slot1 = String.format(Locale.US, "%02d:00", hour)
            val slot2 = String.format(Locale.US, "%02d:30", hour)
            if (!bookedTimeSlots.contains(slot1)) {
                allTimeSlots.add(slot1)
            }
            if (!bookedTimeSlots.contains(slot2)) {
                allTimeSlots.add(slot2)
            }
        }

        return allTimeSlots // Already filtered
    }

    private fun addAppointment(patientName: String, timeSlot: String, notes: String) {
        // Create an AppointmentCalendar object
        // Check the AppointmentCalendar definition for 'time' vs 'timeSlot'
        // Assuming 'timeSlot' holds the "HH:mm" string and 'time' might be redundant or for timestamp
        val appointment = AppointmentCalendar(
            id = "", // ID will be generated by Firebase push()
            patientName = patientName,
            date = selectedDate,
            timeSlot = timeSlot, // Use the correct field
            notes = notes,
            time = timeSlot // If 'time' should store the same as 'timeSlot' or a timestamp? Using timeSlot for now.
        )

        // Get a unique key for the new appointment under the selected date
        val appointmentKey = appointmentsRef.child(selectedDate).push().key
        if (appointmentKey != null) {
            // Set the generated key as the ID in the object before saving
            appointment.id = appointmentKey
            appointmentsRef.child(selectedDate).child(appointmentKey).setValue(appointment)
                .addOnSuccessListener {
                    if (context != null) {
                        Toast.makeText(context, "Appointment added successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    if (context != null) {
                        Toast.makeText(context, "Error adding appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            if (context != null) {
                Toast.makeText(context, "Error generating appointment key", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Parameter type changed to AppointmentCalendar
    private fun showAppointmentOptionsDialog(appointment: AppointmentCalendar) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(requireContext())
            .setTitle("Appointment Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditAppointmentDialog(appointment)
                    1 -> showDeleteConfirmationDialog(appointment)
                }
            }
            .setNegativeButton("Cancel", null) // Add a cancel button
            .show()
    }

    // Parameter type changed to AppointmentCalendar
    private fun showEditAppointmentDialog(appointment: AppointmentCalendar) {
        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_edit_appointment, null) // Ensure layout name matches
        val patientNameEditText = dialogView.findViewById<EditText>(R.id.patientNameEditText)
        val timeSlotSpinner = dialogView.findViewById<Spinner>(R.id.timeSlotSpinner)
        val notesEditText = dialogView.findViewById<EditText>(R.id.notesEditText)

        // Set current values using properties from AppointmentCalendar
        patientNameEditText.setText(appointment.patientName)
        notesEditText.setText(appointment.notes)

        // Get available time slots including the current one
        val availableTimeSlots = getAvailableTimeSlots().toMutableList()
        // Add the current appointment's time slot back to the list if it wasn't already available
        if (!availableTimeSlots.contains(appointment.timeSlot)) {
            availableTimeSlots.add(appointment.timeSlot)
        }
        availableTimeSlots.sort() // Sort the list alphabetically/numerically

        // Set up spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableTimeSlots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSlotSpinner.adapter = adapter
        // Set the spinner to the appointment's current time slot
        val currentPosition = availableTimeSlots.indexOf(appointment.timeSlot)
        if (currentPosition >= 0) {
            timeSlotSpinner.setSelection(currentPosition)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Appointment")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val patientName = patientNameEditText.text.toString().trim()
                val timeSlot = timeSlotSpinner.selectedItem.toString()
                val notes = notesEditText.text.toString().trim()

                if (patientName.isEmpty()) {
                    if (context != null) {
                        Toast.makeText(context, "Patient name cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                    return@setPositiveButton // Stay in dialog
                }

                // Use the correct properties from AppointmentCalendar
                updateAppointment(appointment.id, patientName, timeSlot, notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAppointment(id: String, patientName: String, timeSlot: String, notes: String) {
        // Create an AppointmentCalendar object for the update
        val updatedAppointment = AppointmentCalendar(
            id = id, // Keep the original ID
            patientName = patientName,
            date = selectedDate, // Date remains the same for this update
            timeSlot = timeSlot, // Use the correct field
            notes = notes,
            time = timeSlot // Again, assuming time holds same value as timeSlot or similar
        )

        // Update the specific appointment using its ID under the selected date
        appointmentsRef.child(selectedDate).child(id).setValue(updatedAppointment)
            .addOnSuccessListener {
                if (context != null) {
                    Toast.makeText(context, "Appointment updated successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (context != null) {
                    Toast.makeText(context, "Error updating appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Parameter type changed to AppointmentCalendar
    private fun showDeleteConfirmationDialog(appointment: AppointmentCalendar) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Appointment")
            .setMessage("Are you sure you want to delete the appointment for ${appointment.patientName} at ${appointment.timeSlot}?") // More informative message
            .setPositiveButton("Yes, Delete") { _, _ ->
                // Use the correct ID property from AppointmentCalendar
                deleteAppointment(appointment.id)
            }
            .setNegativeButton("No, Cancel", null)
            .show()
    }

    private fun deleteAppointment(id: String) {
        // Remove the specific appointment using its ID under the selected date
        appointmentsRef.child(selectedDate).child(id).removeValue()
            .addOnSuccessListener {
                if (context != null) {
                    Toast.makeText(context, "Appointment deleted successfully", Toast.LENGTH_SHORT).show()
                }
                // The ValueEventListener will automatically update the list
            }
            .addOnFailureListener { e ->
                if (context != null) {
                    Toast.makeText(context, "Error deleting appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showEditWorkingHoursDialog() {
        // Check context before showing dialog
        if (context == null) return

        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_edit_working_hours, null) // Ensure layout name matches
        val startTimeButton = dialogView.findViewById<Button>(R.id.startTimeButton)
        val endTimeButton = dialogView.findViewById<Button>(R.id.endTimeButton)

        // Display current hours
        startTimeButton.text = String.format(Locale.US, "%d:00", startHour)
        endTimeButton.text = String.format(Locale.US, "%d:00", endHour)

        var tempStartHour = startHour
        var tempEndHour = endHour

        startTimeButton.setOnClickListener {
            TimePickerDialog(
                context, // Use context directly
                { _, hourOfDay, _ -> // Minute is ignored (set to 0)
                    tempStartHour = hourOfDay
                    startTimeButton.text = String.format(Locale.US, "%d:00", tempStartHour)
                    // Basic validation inside picker if possible, or check on save
                },
                tempStartHour, // Initial hour
                0, // Initial minute (always 0 for simplicity)
                true // 24-hour format
            ).show()
        }

        endTimeButton.setOnClickListener {
            TimePickerDialog(
                context, // Use context directly
                { _, hourOfDay, _ -> // Minute is ignored
                    tempEndHour = hourOfDay
                    endTimeButton.text = String.format(Locale.US, "%d:00", tempEndHour)
                },
                tempEndHour, // Initial hour
                0, // Initial minute
                true // 24-hour format
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Working Hours")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Validate hours before saving
                if (tempStartHour >= tempEndHour) {
                    if (context != null) {
                        Toast.makeText(context, "End time must be after start time", Toast.LENGTH_SHORT).show()
                    }
                    // Don't save, maybe re-show dialog or handle error differently
                    return@setPositiveButton
                }
                // Save the temporary hours to the actual variables and Firebase
                startHour = tempStartHour
                endHour = tempEndHour
                saveWorkingHours(startHour, endHour)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveWorkingHours(start: Int, end: Int) {
        val workingHours = WorkingHours(startHour = start, endHour = end)
        workingHoursRef.setValue(workingHours)
            .addOnSuccessListener {
                if (context != null) {
                    Toast.makeText(context, "Working hours updated successfully", Toast.LENGTH_SHORT).show()
                }
                // ValueEventListener on workingHoursRef will update the display
            }
            .addOnFailureListener { e ->
                if (context != null) {
                    Toast.makeText(context, "Error updating working hours: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        // Clean up the listener when the view is destroyed to prevent memory leaks
        appointmentsRef.child(selectedDate).removeEventListener(appointmentValueListener)
        super.onDestroyView()
    }
}