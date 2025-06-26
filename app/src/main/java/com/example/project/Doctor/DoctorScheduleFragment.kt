package com.example.project.doctor

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.project.Booking
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * A [Fragment] that displays a doctor's schedule for a selected date.
 * It allows the doctor to view bookings and update their status.
 */
class DoctorScheduleFragment : Fragment() {
    private lateinit var bookingsListView: ListView
    private lateinit var bookingsTitleTextView: TextView
    private lateinit var datePickerButton: Button
    private lateinit var refreshButton: Button
    private val bookingsDisplayList = mutableListOf<String>()
    private val bookingsList = mutableListOf<Booking>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth
    private var currentDoctorId: String? = null
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private var selectedDate: String = dateFormat.format(calendar.time)

    /**
     * Initializes the fragment's UI and data.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_schedule, container, false)

        bookingsListView = view.findViewById(R.id.patientsListView)
        bookingsTitleTextView = view.findViewById(R.id.patientsTitleTextView)
        datePickerButton = view.findViewById(R.id.datePickerButton)
        refreshButton = view.findViewById(R.id.refreshButton)

        firestoreHelper = FirestoreHelper()
        auth = FirebaseAuth.getInstance()

        currentDoctorId = auth.currentUser?.uid

        updateDateDisplay()

        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            bookingsDisplayList
        )
        bookingsListView.adapter = adapter

        setupListeners()

        if (currentDoctorId == null) {
            Log.w("DoctorSchedule", "Doctor ID is null. User might not be logged in.")
            Toast.makeText(context, "Error: Doctor not logged in.", Toast.LENGTH_LONG).show()
            bookingsDisplayList.add("Not logged in or Doctor ID not found.")
            adapter.notifyDataSetChanged()
        } else {
            fetchBookingsForDate(selectedDate)
        }

        return view
    }

    /**
     * Sets up UI event listeners.
     */
    private fun setupListeners() {
        datePickerButton.setOnClickListener {
            showDatePickerDialog()
        }

        refreshButton.setOnClickListener {
            if (currentDoctorId != null) {
                fetchBookingsForDate(selectedDate)
            }
        }

        bookingsListView.setOnItemClickListener { _, _, position, _ ->
            if (position < bookingsList.size) {
                val selectedBooking = bookingsList[position]
                showBookingStatusDialog(selectedBooking)
            }
        }
    }

    /**
     * Displays a date picker dialog.
     */
    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            selectedDate = dateFormat.format(calendar.time)
            updateDateDisplay()
            if (currentDoctorId != null) {
                fetchBookingsForDate(selectedDate)
            }
        }, year, month, day).show()
    }

    /**
     * Updates the displayed date.
     */
    private fun updateDateDisplay() {
        val displayDate = displayDateFormat.format(calendar.time)
        bookingsTitleTextView.text = "Bookings for $displayDate"
        datePickerButton.text = "Change Date"
    }

    /**
     * Fetches bookings for the selected date.
     * @param date The date to fetch bookings for.
     */
    private fun fetchBookingsForDate(date: String) {
        Log.d("DoctorSchedule", "Fetching bookings for doctor: $currentDoctorId on $date")

        val doctorId = currentDoctorId
        if (doctorId.isNullOrBlank()) {
            Log.w("DoctorSchedule", "Doctor ID is not set. Cannot fetch bookings.")
            bookingsDisplayList.clear()
            bookingsDisplayList.add("Doctor ID not available.")
            adapter.notifyDataSetChanged()
            return
        }

        firestoreHelper.getBookingsForDoctor(doctorId, date)
            .addOnSuccessListener { result ->
                bookingsList.clear()

                if (result.isEmpty) {
                    Log.d("DoctorSchedule", "No bookings found for $doctorId on $date")
                    bookingsDisplayList.clear()
                    bookingsDisplayList.add("No bookings for this date.")
                    adapter.notifyDataSetChanged()
                } else {
                    for (document in result) {
                        val booking = document.toObject(Booking::class.java).copy(id = document.id)
                        bookingsList.add(booking)
                    }
                    bookingsList.sortWith(compareBy { it.start_time })

                    firestoreHelper.getAllServices()
                        .addOnSuccessListener { servicesResult ->
                            val serviceMap = mutableMapOf<String, String>()
                            for (serviceDoc in servicesResult) {
                                val service = serviceDoc.toObject(com.example.project.Service::class.java)
                                serviceMap[serviceDoc.id] = service.name
                            }
                            fetchPatientDetailsAndPopulateList(bookingsList, serviceMap)
                        }
                        .addOnFailureListener { e_services ->
                            Log.w("DoctorSchedule", "Error getting services. Proceeding without service names.", e_services)
                            fetchPatientDetailsAndPopulateList(bookingsList, emptyMap())
                        }
                }
            }
            .addOnFailureListener { e_bookings ->
                Log.w("DoctorSchedule", "Error getting bookings.", e_bookings)
                bookingsDisplayList.clear()
                bookingsDisplayList.add("Error loading bookings.")
                adapter.notifyDataSetChanged()
            }
    }

    /**
     * Fetches patient details and populates the booking list.
     * @param currentBookings List of bookings.
     * @param serviceMap Map of service IDs to names.
     */
    private fun fetchPatientDetailsAndPopulateList(currentBookings: List<Booking>, serviceMap: Map<String, String>) {
        bookingsDisplayList.clear()

        if (currentBookings.isEmpty()) {
            if (bookingsDisplayList.isEmpty()) {
                bookingsDisplayList.add("No bookings for this date.")
            }
            adapter.notifyDataSetChanged()
            return
        }

        val patientNamesMap = mutableMapOf<String, String>()
        val totalBookingsToProcess = currentBookings.size
        var patientsFetchedCounter = 0

        for (booking in currentBookings) {
            firestoreHelper.getPatientById(booking.patient_name)
                .addOnCompleteListener { task ->
                    val patientDisplayName = if (task.isSuccessful && task.result?.exists() == true) {
                        val patientDoc = task.result!!
                        val firstName = patientDoc.getString("firstName") ?: ""
                        val lastName = patientDoc.getString("lastName") ?: ""
                        if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            "$firstName $lastName".trim()
                        } else {
                            Log.w("DoctorSchedule", "Patient name fields empty for ${booking.patient_name}, using ID.")
                            booking.patient_name
                        }
                    } else {
                        Log.w("DoctorSchedule", "Failed to fetch patient details for ${booking.patient_name}, using ID.", task.exception)
                        booking.patient_name
                    }
                    patientNamesMap[booking.patient_name] = patientDisplayName

                    patientsFetchedCounter++

                    if (patientsFetchedCounter == totalBookingsToProcess) {
                        for (b_item in currentBookings) {
                            val finalPatientName = patientNamesMap[b_item.patient_name] ?: b_item.patient_name
                            val finalServiceName = serviceMap[b_item.service_id] ?: b_item.service_id
                            val bookingStatus = b_item.status.capitalize()
                            val bookingInfo = "Patient: $finalPatientName\nService: $finalServiceName\nTime: ${b_item.start_time} - ${b_item.end_time}\nStatus: $bookingStatus"
                            bookingsDisplayList.add(bookingInfo)
                        }

                        if (bookingsDisplayList.isEmpty() && currentBookings.isNotEmpty()) {
                            bookingsDisplayList.add("Error displaying booking details.")
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }

    /**
     * Shows a dialog to update booking status.
     * @param booking The booking to update.
     */
    private fun showBookingStatusDialog(booking: Booking) {
        val statusOptions = arrayOf("Confirmed", "Cancelled", "Completed", "No-show")

        AlertDialog.Builder(requireContext())
            .setTitle("Update Booking Status")
            .setItems(statusOptions) { _, which ->
                val newStatus = statusOptions[which].lowercase()
                updateBookingStatus(booking, newStatus)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Updates a booking's status in Firestore.
     * @param booking The booking to update.
     * @param newStatus The new status.
     */
    private fun updateBookingStatus(booking: Booking, newStatus: String) {
        firestoreHelper.updateBookingStatus(booking.id, newStatus)
            .addOnSuccessListener {
                Toast.makeText(context, "Booking status updated to $newStatus", Toast.LENGTH_SHORT).show()
                fetchBookingsForDate(selectedDate)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update status: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("DoctorSchedule", "Error updating booking status", e)
            }
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private fun String.capitalize(): String {
        return if (this.isNotEmpty()) {
            this.substring(0, 1).uppercase() + this.substring(1)
        } else {
            this
        }
    }
}