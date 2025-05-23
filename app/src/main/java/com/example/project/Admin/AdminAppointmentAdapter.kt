package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import java.util.Locale

// Comprehensive data class for admin appointments management
data class AdminAppointmentItem(
    var id: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val servicePrice: Int = 0,
    val serviceDuration: Int = 0,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val notes: String = "",
    val status: String = "confirmed"
)

class AdminAppointmentAdapter(
    private val onEditClick: (AdminAppointmentItem) -> Unit,
    private val onDeleteClick: (AdminAppointmentItem) -> Unit
) : RecyclerView.Adapter<AdminAppointmentAdapter.AppointmentViewHolder>() {

    private val appointmentList = mutableListOf<AdminAppointmentItem>()

    fun updateAppointments(appointments: List<AdminAppointmentItem>) {
        appointmentList.clear()
        appointmentList.addAll(appointments)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointmentList[position])
    }

    override fun getItemCount(): Int = appointmentList.size

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.appointmentDateTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.appointmentTimeTextView)
        private val patientTextView: TextView = itemView.findViewById(R.id.patientNameTextView)
        private val doctorTextView: TextView = itemView.findViewById(R.id.doctorNameTextView)
        private val serviceTextView: TextView = itemView.findViewById(R.id.serviceNameTextView)
        private val priceTextView: TextView = itemView.findViewById(R.id.servicePriceTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val editButton: Button = itemView.findViewById(R.id.editAppointmentButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteAppointmentButton)

        fun bind(appointment: AdminAppointmentItem) {
            dateTextView.text = appointment.date
            timeTextView.text = "${appointment.startTime} - ${appointment.endTime}"
            patientTextView.text = appointment.patientName
            doctorTextView.text = appointment.doctorName
            serviceTextView.text = appointment.serviceName
            priceTextView.text = "$${appointment.servicePrice}"
            statusTextView.text = appointment.status.capitalize(Locale.ROOT)

            // Set status text color based on status
            statusTextView.setTextColor(
                when (appointment.status.lowercase(Locale.ROOT)) {
                    "confirmed" -> itemView.context.getColor(android.R.color.holo_green_dark)
                    "cancelled" -> itemView.context.getColor(android.R.color.holo_red_dark)
                    "pending" -> itemView.context.getColor(android.R.color.holo_orange_dark)
                    else -> itemView.context.getColor(android.R.color.darker_gray)
                }
            )

            editButton.setOnClickListener { onEditClick(appointment) }
            deleteButton.setOnClickListener { onDeleteClick(appointment) }
        }
    }

    // Extension function to capitalize first letter of a string
    private fun String.capitalize(locale: Locale): String {
        return if (this.isNotEmpty()) {
            this[0].uppercaseChar() + this.substring(1).lowercase(locale)
        } else {
            this
        }
    }
}