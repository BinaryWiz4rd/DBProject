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
    private val bookingsList = mutableListOf<Booking>() // Store bookings for reference when updating status
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth
    private var currentDoctorId: String? = null
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private var selectedDate: String = dateFormat.format(calendar.time)

    /**
     * Inflates the layout, initializes UI components and Firebase, and sets up listeners and adapters.
     * It also loads the initial data for the current date.
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
     * Sets up listeners for the date picker button, refresh button, and booking list items.
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
            // Skip if the item is not a booking (e.g., "No bookings for today")
            if (position < bookingsList.size) {
                val selectedBooking = bookingsList[position]
                showBookingStatusDialog(selectedBooking)
            }
        }
    }

    /**
     * Shows a [DatePickerDialog] to allow the user to select a date.
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
     * Updates the text view that displays the selected date.
     */
    private fun updateDateDisplay() {
        val displayDate = displayDateFormat.format(calendar.time)
        bookingsTitleTextView.text = "Bookings for $displayDate"
        datePickerButton.text = "Change Date"
    }

    /**
     * Fetches the bookings for a given date from Firestore.
     * @param date The date for which to fetch bookings.
     */
    private fun fetchBookingsForDate(date: String) {
        Log.d("DoctorSchedule", "Fetching bookings for doctor: $currentDoctorId on $date")
    
        // currentDoctorId is now nullable, ensure it's not null before proceeding
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
                bookingsList.clear() // Clear the main booking list for new data
                
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
                    // Sort bookings by time before further processing
                    bookingsList.sortWith(compareBy { it.start_time })
                    
                    // Fetch all services to map service_id to service_name
                    firestoreHelper.getAllServices()
                        .addOnSuccessListener { servicesResult ->
                            val serviceMap = mutableMapOf<String, String>()
                            for (serviceDoc in servicesResult) {
                                val service = serviceDoc.toObject(com.example.project.Service::class.java)
                                // Assuming serviceDoc.id is the service_id and service.name is what we want
                                serviceMap[serviceDoc.id] = service.name 
                            }
                            // Now that we have services, fetch patient details and update UI
                            fetchPatientDetailsAndPopulateList(bookingsList, serviceMap)
                        }
                        .addOnFailureListener { e_services ->
                            Log.w("DoctorSchedule", "Error getting services. Proceeding without service names.", e_services)
                            // Proceed with an empty service map; service_id will be shown as fallback
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
     * A helper function to fetch patient details and then populate the display list.
     * @param currentBookings The list of bookings for which to fetch patient details.
     * @param serviceMap A map of service IDs to service names.
     */
    private fun fetchPatientDetailsAndPopulateList(currentBookings: List<Booking>, serviceMap: Map<String, String>) {
        bookingsDisplayList.clear() // Clear display list before populating

        if (currentBookings.isEmpty()) {
            // This case should ideally be handled by the caller setting "No bookings for this date."
            // If bookingsDisplayList is still empty, add a default message.
            if (bookingsDisplayList.isEmpty()) { // Check if it wasn't already set by caller
                 bookingsDisplayList.add("No bookings for this date.")
            }
            adapter.notifyDataSetChanged()
            return
        }

        val patientNamesMap = mutableMapOf<String, String>() // patientId -> patientDisplayName
        val totalBookingsToProcess = currentBookings.size
        var patientsFetchedCounter = 0

        for (booking in currentBookings) {
            // Assuming booking.patient_name is the patient's UID
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
                            booking.patient_name // Fallback to ID
                        }
                    } else {
                        Log.w("DoctorSchedule", "Failed to fetch patient details for ${booking.patient_name}, using ID.", task.exception)
                        booking.patient_name // Fallback to ID
                    }
                    patientNamesMap[booking.patient_name] = patientDisplayName
                    
                    patientsFetchedCounter++

                    if (patientsFetchedCounter == totalBookingsToProcess) {
                        // All patient data fetched (or failed with fallback), now populate the display list in order
                        for (b_item in currentBookings) { // Iterate original sorted list
                            val finalPatientName = patientNamesMap[b_item.patient_name] ?: b_item.patient_name
                            val finalServiceName = serviceMap[b_item.service_id] ?: b_item.service_id // Fallback to service_id
                            val bookingStatus = b_item.status.capitalize() // Assuming capitalize extension exists
                            val bookingInfo = "Patient: $finalPatientName\nService: $finalServiceName\nTime: ${b_item.start_time} - ${b_item.end_time}\nStatus: $bookingStatus"
                            bookingsDisplayList.add(bookingInfo)
                        }
                        
                        if (bookingsDisplayList.isEmpty() && currentBookings.isNotEmpty()) {
                            // Fallback if list is empty despite having bookings (e.g., all fetches failed catastrophically)
                            bookingsDisplayList.add("Error displaying booking details.")
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }
    
    // Comment out or remove the old displayBookingsWithoutServiceNames function
    // private fun displayBookingsWithoutServiceNames() {
    // bookingsDisplayList.clear()
    // for (booking in bookingsList) {
    // val bookingStatus = booking.status.capitalize()
    // val bookingInfo = "Patient: ${booking.patient_name}\nService: ${booking.service_id}\nTime: ${booking.start_time} - ${booking.end_time}\nStatus: $bookingStatus"
    // bookingsDisplayList.add(bookingInfo)
    // }
    // adapter.notifyDataSetChanged()
    // }
    
    /**
     * Shows a dialog to update the status of a booking.
     * @param booking The booking to be updated.
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
     * Updates the status of a booking in Firestore.
     * @param booking The booking to be updated.
     * @param newStatus The new status of the booking.
     */
    private fun updateBookingStatus(booking: Booking, newStatus: String) {
        firestoreHelper.updateBookingStatus(booking.id, newStatus)
            .addOnSuccessListener {
                Toast.makeText(context, "Booking status updated to $newStatus", Toast.LENGTH_SHORT).show()
                fetchBookingsForDate(selectedDate) // Refresh the list with the current selected date
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update status: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("DoctorSchedule", "Error updating booking status", e)
            }
    }
    
    /**
     * A helper extension function to capitalize the first letter of a string.
     */
    private fun String.capitalize(): String {
        return if (this.isNotEmpty()) {
            this.substring(0, 1).uppercase() + this.substring(1)
        } else {
            this
        }
    }
}