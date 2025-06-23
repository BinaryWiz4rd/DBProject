package com.example.project.Patient

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class AppointmentListAdapter(
    private var appointments: List<PatientAppointmentDetails>,
    private val onAppointmentClick: (PatientAppointmentDetails) -> Unit
) : RecyclerView.Adapter<AppointmentListAdapter.AppointmentViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_list, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount(): Int = appointments.size

    fun updateAppointments(newAppointments: List<PatientAppointmentDetails>) {
        this.appointments = newAppointments
        notifyDataSetChanged()
    }
    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.tvAppointmentDate)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvAppointmentTime)
        private val doctorTextView: TextView = itemView.findViewById(R.id.tvDoctorName)
        private val serviceTextView: TextView = itemView.findViewById(R.id.tvServiceName)
        private val statusTextView: TextView = itemView.findViewById(R.id.tvAppointmentStatus)
        private val actionButtonsLayout: LinearLayout = itemView.findViewById(R.id.actionButtonsLayout)
        private val chatButton: Button = itemView.findViewById(R.id.btnChat)
        fun bind(appointment: PatientAppointmentDetails) {
            var isPast = false
            try {
                // Parse and format date
                val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputDateFormat.parse(appointment.date)
                dateTextView.text = if (date != null) dateFormat.format(date) else appointment.date

                // Check if the appointment is in the past
                if (date != null) {
                    val today = Calendar.getInstance()
                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)
                    today.set(Calendar.MILLISECOND, 0)
                    isPast = date.before(today.time)
                }

            } catch (e: Exception) {
                dateTextView.text = appointment.date
            }
            
            timeTextView.text = "${appointment.startTime} - ${appointment.endTime}"
            doctorTextView.text = "Dr. ${appointment.doctorName}"
            serviceTextView.text = appointment.serviceName
            
            // Set status and color
            statusTextView.text = appointment.status.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            
            when (appointment.status.lowercase()) {
                "confirmed" -> {
                    statusTextView.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    actionButtonsLayout.visibility = View.VISIBLE
                    chatButton.visibility = View.VISIBLE
                }
                "pending" -> {
                    statusTextView.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    actionButtonsLayout.visibility = View.GONE
                }
                "cancelled" -> {
                    statusTextView.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    actionButtonsLayout.visibility = View.GONE
                }
                else -> {
                    statusTextView.setTextColor(itemView.context.getColor(R.color.grey))
                    actionButtonsLayout.visibility = View.GONE
                }
            }

            chatButton.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("patientId", FirebaseAuth.getInstance().currentUser?.uid)
                    putExtra("doctorId", appointment.doctorId)
                    putExtra("patientName", FirebaseAuth.getInstance().currentUser?.displayName ?: "Patient")
                    putExtra("doctorName", appointment.doctorName)
                    putExtra("chatTitle", "Chat with Dr. ${appointment.doctorName}")
                }
                context.startActivity(intent)
            }
            
            // Set opacity for past appointments
            itemView.alpha = if (isPast) 0.5f else 1.0f

            itemView.setOnClickListener {
                onAppointmentClick(appointment)
            }
        }
    }
}
