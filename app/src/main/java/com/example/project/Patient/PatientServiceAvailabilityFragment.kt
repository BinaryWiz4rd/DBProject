package com.example.project.Patient

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.Booking
import com.example.project.R
import com.example.project.Service
import com.example.project.util.FirestoreHelper
import java.text.SimpleDateFormat
import java.util.*
import com.example.project.Admin.Doctor

class PatientServiceAvailabilityFragment : Fragment() {

    private lateinit var firestoreHelper: FirestoreHelper
    private var serviceId: String = ""
    private var doctorId: String = ""
    private var serviceDuration: Int = 30 // Default duration
    private var doctorName: String = ""
    private var serviceName: String = ""

    private lateinit var calendarView: CalendarView
    private lateinit var timeSlotsRecyclerView: RecyclerView
    private lateinit var selectedDateTextView: TextView
    private lateinit var noSlotsTextView: TextView
    private lateinit var timeSlotAdapter: TimeSlotAdapter

    private val timeSlots = mutableListOf<String>()
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_patient_service_availability, container, false)

        firestoreHelper = FirestoreHelper()
        serviceId = arguments?.getString("service_id") ?: ""
        doctorId = arguments?.getString("doctor_id") ?: ""

        calendarView = view.findViewById(R.id.calendarView)
        timeSlotsRecyclerView = view.findViewById(R.id.timeSlotsRecyclerView)
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView)
        noSlotsTextView = view.findViewById(R.id.noSlotsTextView)

        calendarView.minDate = System.currentTimeMillis()

        setupRecyclerView()
        fetchServiceAndDoctorDetails()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            updateSelectedDateText()
            loadTimeSlotsForDate(selectedDate.time)
        }

        updateSelectedDateText()

        return view
    }

    private fun setupRecyclerView() {
        timeSlotAdapter = TimeSlotAdapter(timeSlots) { timeSlot ->
            onTimeSlotClicked(timeSlot)
        }
        timeSlotsRecyclerView.layoutManager = LinearLayoutManager(context)
        timeSlotsRecyclerView.adapter = timeSlotAdapter
    }
    
    private fun fetchServiceAndDoctorDetails() {
        if (serviceId.isEmpty() || doctorId.isEmpty()) {
            Toast.makeText(context, "Service or Doctor ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceTask = firestoreHelper.getServiceById(serviceId)
        val doctorTask = firestoreHelper.getDoctorById(doctorId)

        com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(serviceTask, doctorTask)
            .addOnSuccessListener { results ->
                val serviceDoc = results[0] as com.google.firebase.firestore.DocumentSnapshot
                val doctorDoc = results[1] as com.google.firebase.firestore.DocumentSnapshot

                val service = serviceDoc.toObject(Service::class.java)
                serviceDuration = service?.duration_minutes ?: 30
                serviceName = service?.name ?: "Unknown Service"

                val doctor = doctorDoc.toObject(Doctor::class.java)
                doctorName = doctor?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown Doctor"
                
                // Now that we have the details, load slots for the initial date
                loadTimeSlotsForDate(selectedDate.time)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to get service/doctor details: ${e.message}", Toast.LENGTH_SHORT).show()
                // Load with default duration even if details fail
                loadTimeSlotsForDate(selectedDate.time)
            }
    }

    private fun updateSelectedDateText() {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        selectedDateTextView.text = "Available slots for: ${sdf.format(selectedDate.time)}"
    }
    
    private fun loadTimeSlotsForDate(date: Date) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(date)

        val doctorSettingsTask = firestoreHelper.getDoctorSettings(doctorId)
        val bookingsTask = firestoreHelper.getBookingsForDoctor(doctorId, dateString)

        com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(doctorSettingsTask, bookingsTask)
            .addOnSuccessListener { results ->
                val doctorSettingsDoc = results[0] as com.google.firebase.firestore.DocumentSnapshot
                val bookingsSnapshot = results[1] as com.google.firebase.firestore.QuerySnapshot

                val startHour = doctorSettingsDoc.getLong("startHour")?.toInt() ?: 8 // Default 8 AM
                val endHour = doctorSettingsDoc.getLong("endHour")?.toInt() ?: 17 // Default 5 PM

                val bookings = bookingsSnapshot.toObjects(Booking::class.java)
                
                val generatedSlots = generateTimeSlots(startHour, endHour, serviceDuration, bookings)

                timeSlots.clear()
                timeSlots.addAll(generatedSlots)

                if (timeSlots.isEmpty()) {
                    noSlotsTextView.visibility = View.VISIBLE
                    timeSlotsRecyclerView.visibility = View.GONE
                } else {
                    noSlotsTextView.visibility = View.GONE
                    timeSlotsRecyclerView.visibility = View.VISIBLE
                }
                timeSlotAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load schedule: ${e.message}", Toast.LENGTH_SHORT).show()
                noSlotsTextView.visibility = View.VISIBLE
                timeSlotsRecyclerView.visibility = View.GONE
            }
    }
    
    private fun generateTimeSlots(startHour: Int, endHour: Int, durationMinutes: Int, bookings: List<Booking>): List<String> {
        val slots = mutableListOf<String>()
        val calendar = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCalendar = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, 0)
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        while (calendar.before(endCalendar)) {
            val slotStart = calendar.time
            calendar.add(Calendar.MINUTE, durationMinutes)
            val slotEnd = calendar.time
            
            if (calendar.after(endCalendar)) break

            val slotStartTime = timeFormat.format(slotStart)
            val slotEndTime = timeFormat.format(slotEnd)

            val isBooked = bookings.any { booking ->
                val bookingStart = timeFormat.parse(booking.start_time)
                val bookingEnd = timeFormat.parse(booking.end_time)
                // Check for overlap
                slotStart.before(bookingEnd) && slotEnd.after(bookingStart)
            }

            if (!isBooked) {
                slots.add("$slotStartTime - $slotEndTime")
            }
        }
        return slots
    }

    private fun onTimeSlotClicked(timeSlot: String) {
        val times = timeSlot.split(" - ")
        val startTime = times[0]
        val endTime = times[1]
        
        navigateToConfirmBooking(serviceId, doctorId, selectedDate, startTime, endTime)
    }

    private fun navigateToConfirmBooking(
        serviceId: String,
        doctorId: String,
        date: Calendar,
        startTime: String,
        endTime: String
    ) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateString = dateFormat.format(date.time)
            
            val fragment = ConfirmBookingFragment()
            val bundle = Bundle().apply {
                putString("service_id", serviceId)
                putString("doctor_id", doctorId)
                putString("date", dateString)
                putString("start_time", startTime)
                putString("end_time", endTime)
                putString("doctor_name", doctorName)
                putString("service_name", serviceName)
            }
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Log.e("PatientServiceAvailability", "Error navigating to confirm booking", e)
            Toast.makeText(context, "Error navigating to booking confirmation", Toast.LENGTH_SHORT).show()
        }
    }
}
