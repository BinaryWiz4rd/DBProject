package com.example.project.Patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project.Booking
import com.example.project.R
import com.example.project.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class PatientAppointmentsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noAppointmentsTextView: TextView
    private lateinit var appointmentListAdapter: AppointmentListAdapter
    private val firestoreHelper = FirestoreHelper()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_patient_appointments, container, false)
        recyclerView = view.findViewById(R.id.appointmentsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        noAppointmentsTextView = view.findViewById(R.id.tvNoAppointments)
        setupRecyclerView()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAppointments()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        appointmentListAdapter = AppointmentListAdapter(emptyList()) { appointment ->
            // Handle appointment click if needed
        }
        recyclerView.adapter = appointmentListAdapter
    }

    private fun loadAppointments() {
        progressBar.visibility = View.VISIBLE
        if (userId == null) {
            progressBar.visibility = View.GONE
            noAppointmentsTextView.visibility = View.VISIBLE
            noAppointmentsTextView.text = "Please log in to see your appointments."
            return
        }

        firestoreHelper.getBookingsForPatient(userId).addOnSuccessListener { bookingSnapshot ->
            if (bookingSnapshot.isEmpty) {
                progressBar.visibility = View.GONE
                noAppointmentsTextView.visibility = View.VISIBLE
                Log.d("PatientAppointments", "No bookings found for patient: $userId")
                return@addOnSuccessListener
            }
            Log.d("PatientAppointments", "Found ${bookingSnapshot.size()} bookings for patient: $userId")

            val appointmentDetailsTasks = bookingSnapshot.documents.map { doc ->
                val booking = doc.toObject(Booking::class.java)!!
                val doctorTask = firestoreHelper.getDoctorById(booking.doctor_id)
                val serviceTask = firestoreHelper.getServiceById(booking.service_id)

                Tasks.whenAllSuccess<Any>(doctorTask, serviceTask).continueWith { task ->
                    val doctor = (task.result[0] as com.google.firebase.firestore.DocumentSnapshot).toObject(com.example.project.Admin.Doctor::class.java)
                    val service = (task.result[1] as com.google.firebase.firestore.DocumentSnapshot).toObject(com.example.project.Service::class.java)
                    PatientAppointmentDetails(
                        booking = booking,
                        doctorName = doctor?.firstName + " " + doctor?.lastName,
                        serviceName = service?.name ?: "N/A"
                    )
                }
            }

            Tasks.whenAllSuccess<PatientAppointmentDetails>(appointmentDetailsTasks).addOnSuccessListener { appointmentDetailsList ->
                progressBar.visibility = View.GONE
                Log.d("PatientAppointments", "Successfully processed ${appointmentDetailsList.size} appointments.")
                if (appointmentDetailsList.isNotEmpty()) {
                    val sortedList = appointmentDetailsList.sortedWith(compareBy({ it.date }, { it.startTime }))
                    appointmentListAdapter.updateAppointments(sortedList)
                } else {
                    noAppointmentsTextView.visibility = View.VISIBLE
                    appointmentListAdapter.updateAppointments(emptyList())
                    Log.d("PatientAppointments", "Appointment details list is empty after processing.")
                }
            }.addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("PatientAppointments", "Failed to load appointment details", e)
                Toast.makeText(context, "Failed to load appointment details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            progressBar.visibility = View.GONE
            Log.e("PatientAppointments", "Failed to load bookings", e)
            Toast.makeText(context, "Failed to load bookings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
