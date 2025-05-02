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

    // Appointments list
    private val appointmentsList = mutableListOf<Appointment>()
    private lateinit var appointmentAdapter: AppointmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_calendar, container, false)

        // Initialize Firebase
        val doctorId = auth.currentUser?.uid ?: "unknown"
        database = FirebaseDatabase.getInstance().reference
        appointmentsRef = database.child("doctors").child(doctorId).child("appointments")
        workingHoursRef = database.child("doctors").child(doctorId).child("workingHours")

        // Initialize views
        calendarView = view.findViewById(R.id.calendarView)
        addAppointmentButton = view.findViewById(R.id.addAppointmentButton)
        appointmentsListView = view.findViewById(R.id.appointmentsListView)
        workingHoursTextView = view.findViewById(R.id.workingHoursTextView)
        editWorkingHoursButton = view.findViewById(R.id.editWorkingHoursButton)

        // Initialize adapter
        appointmentAdapter = AppointmentAdapter(requireContext(), appointmentsList)
        appointmentsListView.adapter = appointmentAdapter

        // Set initial date
        selectedDate = dateFormatter.format(Date(calendarView.date))

        setupListeners()
        loadWorkingHours()

        return view
    }

    private fun setupListeners() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            loadAppointments()
        }

        addAppointmentButton.setOnClickListener {
            showAddAppointmentDialog()
        }

        editWorkingHoursButton.setOnClickListener {
            showEditWorkingHoursDialog()
        }

        appointmentsListView.setOnItemLongClickListener { _, _, position, _ ->
            val appointment = appointmentsList[position]
            showAppointmentOptionsDialog(appointment)
            true
        }
    }

    private fun loadWorkingHours() {
        workingHoursRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val workingHours = snapshot.getValue(WorkingHours::class.java)
                    if (workingHours != null) {
                        startHour = workingHours.startHour
                        endHour = workingHours.endHour
                        updateWorkingHoursDisplay()
                    }
                } else {
                    // Set default working hours if not configured
                    saveWorkingHours(startHour, endHour)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load working hours: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateWorkingHoursDisplay() {
        workingHoursTextView.text = "Working Hours: $startHour:00 - $endHour:00"
    }

    private fun loadAppointments() {
        appointmentsRef.child(selectedDate).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                appointmentsList.clear()
                for (appointmentSnapshot in snapshot.children) {
                    val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                    if (appointment != null) {
                        appointment.id = appointmentSnapshot.key ?: ""
                        appointmentsList.add(appointment)
                    }
                }

                // Sort appointments by time
                appointmentsList.sortBy { it.timeSlot }
                appointmentAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load appointments: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddAppointmentDialog() {
        // Get available time slots
        val availableTimeSlots = getAvailableTimeSlots()

        if (availableTimeSlots.isEmpty()) {
            Toast.makeText(context, "No available time slots for today", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_add_appointment, null)
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
                    Toast.makeText(context, "Patient name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                addAppointment(patientName, timeSlot, notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getAvailableTimeSlots(): List<String> {
        val allTimeSlots = mutableListOf<String>()
        val bookedTimeSlots = appointmentsList.map { it.timeSlot }

        // Generate all possible time slots based on working hours
        for (hour in startHour until endHour) {
            allTimeSlots.add(String.format("%02d:00", hour))
            allTimeSlots.add(String.format("%02d:30", hour))
        }

        // Remove already booked slots
        return allTimeSlots.filter { it !in bookedTimeSlots }
    }

    private fun addAppointment(patientName: String, timeSlot: String, notes: String) {
        val appointment = Appointment(
            id = "",
            patientName = patientName,
            date = selectedDate,
            time = timeSlot,
            notes = notes
        )


        val appointmentKey = appointmentsRef.child(selectedDate).push().key
        if (appointmentKey != null) {
            appointment.id = appointmentKey
            appointmentsRef.child(selectedDate).child(appointmentKey).setValue(appointment)
                .addOnSuccessListener {
                    Toast.makeText(context, "Appointment added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error adding appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showAppointmentOptionsDialog(appointment: Appointment) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(requireContext())
            .setTitle("Appointment Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditAppointmentDialog(appointment)
                    1 -> showDeleteConfirmationDialog(appointment)
                }
            }
            .show()
    }

    private fun showEditAppointmentDialog(appointment: Appointment) {
        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_edit_appointment, null)
        val patientNameEditText = dialogView.findViewById<EditText>(R.id.patientNameEditText)
        val timeSlotSpinner = dialogView.findViewById<Spinner>(R.id.timeSlotSpinner)
        val notesEditText = dialogView.findViewById<EditText>(R.id.notesEditText)

        // Set current values
        patientNameEditText.setText(appointment.patientName)
        notesEditText.setText(appointment.notes)

        // Get available time slots including the current one
        val availableTimeSlots = getAvailableTimeSlots().toMutableList()
        if (!availableTimeSlots.contains(appointment.timeSlot)) {
            availableTimeSlots.add(appointment.timeSlot)
        }
        availableTimeSlots.sort()

        // Set up spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableTimeSlots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSlotSpinner.adapter = adapter
        timeSlotSpinner.setSelection(availableTimeSlots.indexOf(appointment.timeSlot))

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Appointment")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val patientName = patientNameEditText.text.toString().trim()
                val timeSlot = timeSlotSpinner.selectedItem.toString()
                val notes = notesEditText.text.toString().trim()

                if (patientName.isEmpty()) {
                    Toast.makeText(context, "Patient name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateAppointment(appointment.id, patientName, timeSlot, notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAppointment(id: String, patientName: String, timeSlot: String, notes: String) {
        val updatedAppointment = Appointment(
            id = id,
            patientName = patientName,
            date = selectedDate,
            time = timeSlot,
            notes = notes
        )

        appointmentsRef.child(selectedDate).child(id).setValue(updatedAppointment)
            .addOnSuccessListener {
                Toast.makeText(context, "Appointment updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating appointment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(appointment: Appointment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Appointment")
            .setMessage("Are you sure you want to delete this appointment?")
            .setPositiveButton("Yes") { _, _ ->
                deleteAppointment(appointment.id)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteAppointment(id: String) {
        appointmentsRef.child(selectedDate).child(id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Appointment deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error deleting appointment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditWorkingHoursDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_dialog_edit_working_hours, null)
        val startTimeButton = dialogView.findViewById<Button>(R.id.startTimeButton)
        val endTimeButton = dialogView.findViewById<Button>(R.id.endTimeButton)

        startTimeButton.text = "$startHour:00"
        endTimeButton.text = "$endHour:00"

        startTimeButton.setOnClickListener {
            TimePickerDialog(
                context,
                { _, hourOfDay, _ ->
                    startHour = hourOfDay
                    startTimeButton.text = "$startHour:00"
                },
                startHour,
                0,
                true
            ).show()
        }

        endTimeButton.setOnClickListener {
            TimePickerDialog(
                context,
                { _, hourOfDay, _ ->
                    endHour = hourOfDay
                    endTimeButton.text = "$endHour:00"
                },
                endHour,
                0,
                true
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Working Hours")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                if (startHour >= endHour) {
                    Toast.makeText(context, "End time must be after start time", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveWorkingHours(startHour, endHour)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveWorkingHours(start: Int, end: Int) {
        val workingHours = WorkingHours(start, end)
        workingHoursRef.setValue(workingHours)
            .addOnSuccessListener {
                Toast.makeText(context, "Working hours updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating working hours: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}