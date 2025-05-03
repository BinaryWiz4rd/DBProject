package com.example.project.Doctor
// do DoctorCalendar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R // Make sure R is imported correctly

// Renamed to ScheduleAdapter and using ListAdapter for RecyclerView
class ScheduleAdapter(
    private val onItemClicked: (Appointment) -> Unit // Lambda for click handling
) : ListAdapter<Appointment, ScheduleAdapter.AppointmentViewHolder>(AppointmentDiffCallback()) {

    // ViewHolder holds references to the views in the item layout
    class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Adjust IDs based on your activity_item_appointment.xml layout
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val patientNameTextView: TextView = itemView.findViewById(R.id.patientNameTextView)
        // Assuming you have a TextView for doctor's name in your layout
        private val doctorNameTextView: TextView? = itemView.findViewById(R.id.doctorNameTextView) // Make nullable if optional

        fun bind(appointment: Appointment, onItemClicked: (Appointment) -> Unit) {
            timeTextView.text = appointment.time // Directly use the time string
            patientNameTextView.text = "${appointment.patient.firstName} ${appointment.patient.lastName}"
            // Display doctor's name if the TextView exists
            doctorNameTextView?.text = "Dr. ${appointment.doctor.lastName}"
            // Set click listener
            itemView.setOnClickListener { onItemClicked(appointment) }
        }
    }

    // Creates a new ViewHolder when RecyclerView needs one
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            // Make sure R.layout.activity_item_appointment is your correct item layout file
            .inflate(R.layout.activity_item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    // Binds data from the Appointment object to the views in the ViewHolder
    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = getItem(position)
        holder.bind(appointment, onItemClicked)
    }

    // DiffUtil helps ListAdapter determine changes efficiently
    class AppointmentDiffCallback : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            // Compare unique IDs if you add an 'id' field to Appointment
            // return oldItem.id == newItem.id
            // Fallback: compare essential fields if no ID
            return oldItem.doctor.email == newItem.doctor.email && // Assuming email is unique for doctor
                    oldItem.patient.email == newItem.patient.email && // Assuming email is unique for patient
                    oldItem.date == newItem.date &&
                    oldItem.time == newItem.time
        }

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem == newItem // Checks all fields due to data class
        }
    }

    // No need for explicit updateData method when using ListAdapter, use submitList() instead
    // fun updateData(newAppointments: List<Appointment>) {
    //     submitList(newAppointments)
    // }
}
