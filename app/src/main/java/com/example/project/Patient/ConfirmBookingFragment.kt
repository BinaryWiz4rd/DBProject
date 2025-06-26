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

/**
 * A [Fragment] responsible for confirming a new appointment booking.
 *
 * This fragment displays the details of a selected service, doctor, date, and time,
 * and allows the patient to confirm the booking. It performs validation to ensure
 * the chosen time slot is still available before adding the booking to Firestore.
 */
class ConfirmBookingFragment : Fragment() {

    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var auth: FirebaseAuth

    /**
     * The ID of the selected service.
     */
    private var serviceId: String = ""
    /**
     * The ID of the selected doctor.
     */
    private var doctorId: String = ""
    /**
     * The selected date for the appointment in "yyyy-MM-dd" format.
     */
    private var date: String = ""
    /**
     * The start time of the selected slot in "HH:mm" format.
     */
    private var startTime: String = ""
    /**
     * The end time of the selected slot in "HH:mm" format.
     */
    private var endTime: String = ""
    /**
     * The name of the selected doctor.
     */
    private var doctorName: String = ""
    /**
     * The name of the selected service.
     */
    private var serviceName: String = ""

    /**
     * The ID of the currently authenticated patient.
     */
    private var patientId: String = ""
    /**
     * The full name of the currently authenticated patient.
     */
    private var patientName: String = ""

    private lateinit var serviceNameTextView: TextView
    private lateinit var doctorNameTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var patientNameTextView: TextView
    private lateinit var confirmBookingButton: Button
    private lateinit var progressBar: ProgressBar

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_confirm_booking, container, false)

        firestoreHelper = FirestoreHelper()
        auth = FirebaseAuth.getInstance()

        arguments?.let {
            serviceId = it.getString("service_id", "")
            doctorId = it.getString("doctor_id", "")
            date = it.getString("date", "")
            startTime = it.getString("start_time", "")
            endTime = it.getString("end_time", "")
            doctorName = it.getString("doctor_name", "N/A")
            serviceName = it.getString("service_name", "N/A")
        }

        serviceNameTextView = view.findViewById(R.id.bookingServiceNameTextView)
        doctorNameTextView = view.findViewById(R.id.bookingDoctorNameTextView)
        dateTextView = view.findViewById(R.id.bookingDateTextView)
        timeTextView = view.findViewById(R.id.bookingTimeTextView)
        patientNameTextView = view.findViewById(R.id.patientNameTextView)
        confirmBookingButton = view.findViewById(R.id.confirmBookingButton)
        progressBar = view.findViewById(R.id.bookingProgressBar)

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

    /**
     * Fetches the currently authenticated patient's details from Firestore
     * and updates the patient name TextView.
     *
     * If no user is logged in or the patient profile cannot be found/loaded,
     * it disables the confirm booking button and shows appropriate messages.
     */
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

    /**
     * Validates the selected time slot against existing bookings for the doctor on the given date.
     *
     * If the slot is still available, it proceeds to add the booking to Firestore.
     * If a conflict is found, it informs the user and navigates back.
     */
    private fun validateAndConfirmBooking() {
        if (patientId.isEmpty() || patientName.isEmpty()) {
            Toast.makeText(context, "Patient details not loaded yet. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

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
                    addBookingToFirestore()
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(context, "Failed to validate time slot: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Adds the confirmed booking details to the "bookings" collection in Firestore.
     *
     * On success, it shows a confirmation message and pops the entire back stack
     * to return to the main patient flow. On failure, it shows an error message.
     */
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
                parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(context, "Booking failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Controls the visibility of the progress bar and the enabled state of the confirm button.
     *
     * @param isLoading A boolean indicating whether the loading state should be active (true) or inactive (false).
     */
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