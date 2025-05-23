package com.example.project.doctor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import java.text.NumberFormat
import java.util.Locale

class ScheduleAdapter(
    private val onAppointmentClick: (Appointment) -> Unit
) : ListAdapter<Appointment, ScheduleAdapter.AppointmentViewHolder>(AppointmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_schedule, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        private val patientNameTextView: TextView = itemView.findViewById(R.id.textViewPatientName)
        private val serviceTextView: TextView = itemView.findViewById(R.id.textViewService)
        private val priceTextView: TextView = itemView.findViewById(R.id.textViewPrice)

        fun bind(appointment: Appointment) {
            timeTextView.text = appointment.time
            patientNameTextView.text = "${appointment.patient.firstName} ${appointment.patient.lastName}"
            serviceTextView.text = appointment.serviceName
            
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            priceTextView.text = currencyFormat.format(appointment.servicePrice)

            itemView.setOnClickListener {
                onAppointmentClick(appointment)
            }
        }
    }

    class AppointmentDiffCallback : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem.time == newItem.time && 
                   oldItem.patient.email == newItem.patient.email
        }

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem == newItem
        }
    }
}
