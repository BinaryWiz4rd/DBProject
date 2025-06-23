package com.example.project.Patient

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.project.Admin.Patient
import com.example.project.Booking
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ConfirmBookingFragment : Fragment() {

    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth

    // Arguments from Bundle
    private var serviceId: String = ""
    private var doctorId: String = ""
    private var date: String = ""
    private var startTime: String = ""
    private var endTime: String = ""
    private var doctorName: String = ""
    private var serviceName: String = ""
    
    // Patient details
    private var patientId: String = ""
    private var patientName: String = ""

    // UI Elements
    private lateinit var serviceNameTextView: TextView
    private lateinit var doctorNameTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var patientNameTextView: TextView
    private lateinit var confirmBookingButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_confirm_booking, container, false)

        firestoreHelper = FirestoreHelper()
        auth = FirebaseAuth.getInstance()

        // Get arguments from Bundle
        arguments?.let {
            serviceId = it.getString("service_id", "")
            doctorId = it.getString("doctor_id", "")
            date = it.getString("date", "")
            startTime = it.getString("start_time", "")
            endTime = it.getString("end_time", "")
            doctorName = it.getString("doctor_name", "N/A")
            serviceName = it.getString("service_name", "N/A")
        }

        // Bind views
        serviceNameTextView = view.findViewById(R.id.bookingServiceNameTextView)
        doctorNameTextView = view.findViewById(R.id.bookingDoctorNameTextView)
        dateTextView = view.findViewById(R.id.bookingDateTextView)
        timeTextView = view.findViewById(R.id.bookingTimeTextView)
        patientNameTextView = view.findViewById(R.id.patientNameTextView)
        confirmBookingButton = view.findViewById(R.id.confirmBookingButton)
        progressBar = view.findViewById(R.id.bookingProgressBar)

        // Populate the views
        serviceNameTextView.text = serviceName
        doctorNameTextView.text = doctorName
        dateTextView.text = date
        timeTextView.text = "$startTime - $endTime"

        fetchAndDisplayPatientName()

        confirmBookingButton.setOnClickListener {
            validateAndConfirmBooking()
        }

        return view
    }

    private fun fetchAndDisplayPatientName() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "You must be logged in to book.", Toast.LENGTH_SHORT).show()
            confirmBookingButton.isEnabled = false
            return
        }
        
        patientId = currentUser.uid
        patientNameTextView.text = "Loading..."
        
        firestoreHelper.getPatientById(patientId).addOnSuccessListener { document ->
            if (document.exists()) {
                val patient = document.toObject(Patient::class.java)
                patientName = "${patient?.firstName} ${patient?.lastName}"
                patientNameTextView.text = patientName
            } else {
                patientNameTextView.text = "Unknown User"
                Toast.makeText(context, "Could not find patient profile.", Toast.LENGTH_SHORT).show()
                confirmBookingButton.isEnabled = false
            }
        }.addOnFailureListener {
            patientNameTextView.text = "Error"
            Toast.makeText(context, "Failed to load patient profile.", Toast.LENGTH_SHORT).show()
            confirmBookingButton.isEnabled = false
        }
    }

    private fun validateAndConfirmBooking() {
        if (patientId.isEmpty() || patientName.isEmpty()) {
            Toast.makeText(context, "Patient details not loaded yet. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }
        
        setLoading(true)

        // Step 1: Validate that the slot is still free
        firestoreHelper.getBookingsForDoctor(doctorId, date)
            .addOnSuccessListener { bookingsSnapshot ->
                val bookings = bookingsSnapshot.toObjects(Booking::class.java)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val newBookingStart = timeFormat.parse(startTime)
                val newBookingEnd = timeFormat.parse(endTime)

                val hasConflict = bookings.any { booking ->
                    val existingStart = timeFormat.parse(booking.start_time)
                    val existingEnd = timeFormat.parse(booking.end_time)
                    newBookingStart.before(existingEnd) && newBookingEnd.after(existingStart)
                }

                if (hasConflict) {
                    setLoading(false)
                    Toast.makeText(context, "This time slot is no longer available. Please choose another one.", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                } else {
                    // Step 2: Add the booking
                    addBookingToFirestore()
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(context, "Failed to validate time slot: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun addBookingToFirestore() {
        val booking = hashMapOf(
            "doctor_id" to doctorId,
            "service_id" to serviceId,
            "date" to date,
            "start_time" to startTime,
            "end_time" to endTime,
            "patient_id" to patientId,
            "patient_name" to patientName,
            "status" to "confirmed"
        )

        firestoreHelper.getDbInstance().collection("bookings").add(booking)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(context, "Booking confirmed!", Toast.LENGTH_LONG).show()
                // Navigate back to the root of the patient flow
                parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(context, "Booking failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            confirmBookingButton.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            confirmBookingButton.isEnabled = true
        }
    }
}
